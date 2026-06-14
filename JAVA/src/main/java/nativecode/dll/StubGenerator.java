package nativecode.dll;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * 本地接口实现类的字节码生成器。
 * <p>
 * 使用 ASM 在运行时为 {@code @LibraryImport} 接口生成隐藏类实现，
 * 生成的类通过 {@link MethodHandle} 调用本地函数，支持以下方法类型：
 * <ul>
 *   <li>本地函数方法 — 通过 {@code MethodHandle.invokeExact} 调用</li>
 *   <li>{@code @Field} 结构体字段 — 直接读写内存偏移</li>
 *   <li>{@code @FieldArray} 结构体数组字段 — 切片 + 拷贝</li>
 * </ul>
 */
enum StubGenerator {
	;
	
	/**
	 * 字段类型对应的 ASM 描述符信息
	 */
	private record FieldTypeInfo(String layoutName, String layoutDesc, String getDesc, String setDesc, int returnOpcode,
	                             int loadOpcode) {
	}
	
	private static final String FFM_FACTORY = "nativecode/dll/FFMFactory";
	private static final String MEMORY_SEGMENT = "java/lang/foreign/MemorySegment";
	
	private static final Map<Class<?>, FieldTypeInfo> FIELD_TYPES = Map.of(int.class, new FieldTypeInfo("JAVA_INT", "Ljava/lang/foreign/ValueLayout$OfInt;", "(Ljava/lang/foreign/ValueLayout$OfInt;J)I", "(Ljava/lang/foreign/ValueLayout$OfInt;JI)V", IRETURN, ILOAD), long.class, new FieldTypeInfo("JAVA_LONG", "Ljava/lang/foreign/ValueLayout$OfLong;", "(Ljava/lang/foreign/ValueLayout$OfLong;J)J", "(Ljava/lang/foreign/ValueLayout$OfLong;JJ)V", LRETURN, LLOAD), double.class, new FieldTypeInfo("JAVA_DOUBLE", "Ljava/lang/foreign/ValueLayout$OfDouble;", "(Ljava/lang/foreign/ValueLayout$OfDouble;J)D", "(Ljava/lang/foreign/ValueLayout$OfDouble;JD)V", DRETURN, DLOAD), float.class, new FieldTypeInfo("JAVA_FLOAT", "Ljava/lang/foreign/ValueLayout$OfFloat;", "(Ljava/lang/foreign/ValueLayout$OfFloat;J)F", "(Ljava/lang/foreign/ValueLayout$OfFloat;JF)V", FRETURN, FLOAD), short.class, new FieldTypeInfo("JAVA_SHORT", "Ljava/lang/foreign/ValueLayout$OfShort;", "(Ljava/lang/foreign/ValueLayout$OfShort;J)S", "(Ljava/lang/foreign/ValueLayout$OfShort;JS)V", IRETURN, ILOAD), byte.class, new FieldTypeInfo("JAVA_BYTE", "Ljava/lang/foreign/ValueLayout$OfByte;", "(Ljava/lang/foreign/ValueLayout$OfByte;J)B", "(Ljava/lang/foreign/ValueLayout$OfByte;JB)V", IRETURN, ILOAD), boolean.class, new FieldTypeInfo("JAVA_BOOLEAN", "Ljava/lang/foreign/ValueLayout$OfBoolean;", "(Ljava/lang/foreign/ValueLayout$OfBoolean;J)Z", "(Ljava/lang/foreign/ValueLayout$OfBoolean;JZ)V", IRETURN, ILOAD));
	
	/**
	 * 生成接口实现类的字节码。
	 *
	 * @param api     标注了 {@code @LibraryImport} 的接口
	 * @param methods 接口中所有抽象方法（包括 {@code @Field} 和 {@code @FieldArray}）
	 * @return 生成的类字节码，可通过 {@link java.lang.invoke.MethodHandles.Lookup#defineHiddenClass} 加载
	 */
	static byte[] generate(Class<?> api, List<Method> methods) {
		String implName = api.getName().replace('.', '/') + "$FFM";
		String apiName = api.getName().replace('.', '/');
		
		LibraryImport lib = api.getAnnotation(LibraryImport.class);
		if (lib == null) {
			throw new IllegalStateException("Missing @LibraryImport");
		}
		long structSize = lib.structSize();
		if (structSize <= 0) {
			throw new IllegalStateException("structSize must be > 0");
		}
		
		List<Method> nativeMethods = new ArrayList<>();
		for (Method m : methods) {
			if (!m.isAnnotationPresent(Field.class)) {
				nativeMethods.add(m);
			}
		}
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(V21, ACC_PUBLIC | ACC_FINAL, implName, null, "java/lang/Object",
				new String[]{apiName, "java/lang/AutoCloseable"});

		emitFields(cw, implName, nativeMethods.size());
		emitClinit(cw, implName, nativeMethods.size());
		emitCtor(cw, implName);
		emitDefaultCtor(cw, implName);
		emitCloseMethod(cw, implName);
		
		int nativeIndex = 0;
		for (Method m : methods) {
			if (m.isAnnotationPresent(Field.class)) {
				emitFieldMethod(cw, implName, m, m.getAnnotation(Field.class).offset());
			} else if (m.isAnnotationPresent(FieldView.class)) {
				emitFieldViewMethod(cw, implName, m, m.getAnnotation(FieldView.class));
			} else if (m.isAnnotationPresent(FieldArray.class)) {
				emitFieldArrayMethod(cw, implName, m, m.getAnnotation(FieldArray.class));
			} else {
				emitMethod(cw, implName, m, nativeIndex++, structSize);
			}
		}
		
		cw.visitEnd();
		return cw.toByteArray();
	}
	
	/**
	 * 生成类的静态字段声明：每个本地方法对应一个 {@code static final MethodHandle} 字段，
	 * 以及一个用于存储本地对象指针的 {@code ptr} 字段。
	 *
	 * @param cw                类写入器
	 * @param implName          生成类的内部名（如 {@code com/example/MyApi$FFM}）
	 * @param nativeMethodCount 需要绑定的本地方法数量
	 */
	private static void emitFields(ClassWriter cw, String implName, int nativeMethodCount) {
		for (int i = 0; i < nativeMethodCount; i++) {
			cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "MH" + i, "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();
		}
		cw.visitField(ACC_PRIVATE | ACC_FINAL, "ptr", "Ljava/lang/foreign/MemorySegment;", null, null).visitEnd();
	}
	
	/**
	 * 生成 {@code <clinit>} 静态初始化块。
	 * <p>
	 * 在类加载时通过 {@link FFMFactory#getHandle(int)} 将所有本地方法句柄
	 * 绑定到对应的 {@code MH0, MH1, ...} 静态字段。
	 *
	 * @param cw       类写入器
	 * @param implName 生成类的内部名
	 * @param count    本地方法数量
	 */
	private static void emitClinit(ClassWriter cw, String implName, int count) {
		MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		for (int i = 0; i < count; i++) {
			mv.visitLdcInsn(i);
			mv.visitMethodInsn(INVOKESTATIC, FFM_FACTORY, "getHandle", "(I)Ljava/lang/invoke/MethodHandle;", false);
			mv.visitFieldInsn(PUTSTATIC, implName, "MH" + i, "Ljava/lang/invoke/MethodHandle;");
		}
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	/**
	 * 生成接受 {@link MemorySegment} 指针的私有构造函数。
	 * <p>
	 * 供"创建"方法（返回接口类型）使用，将本地返回的指针包装为接口实例。
	 *
	 * @param cw       类写入器
	 * @param implName 生成类的内部名
	 */
	private static void emitCtor(ClassWriter cw, String implName) {
		MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "(Ljava/lang/foreign/MemorySegment;)V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitFieldInsn(PUTFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	/**
	 * 生成无参默认构造函数。
	 * <p>
	 * 供 {@link FFMFactory#load(Class)} 返回实例时使用。
	 *
	 * @param cw       类写入器
	 * @param implName 生成类的内部名
	 */
	private static void emitDefaultCtor(ClassWriter cw, String implName) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	/**
	 * 生成 {@code close()} 方法，实现 {@link AutoCloseable}。
	 * <p>
	 * 直接委托给接口中声明的 {@code destroy()} 方法。
	 * 生成的类可直接用于 try-with-resources 或 {@link java.lang.ref.Cleaner}。
	 */
	private static void emitCloseMethod(ClassWriter cw, String implName) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "close", "()V",
				null, new String[]{"java/lang/Exception"});
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, implName, "destroy", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	/**
	 * 生成 {@code @Field} 结构体字段的 getter/setter 方法。
	 * <p>
	 * 无参方法为 getter（读取），单参数方法为 setter（写入）。
	 * 通过 {@link MemorySegment#get} / {@link MemorySegment#set} 直接读写指定偏移处的内存。
	 *
	 * @param cw       类写入器
	 * @param implName 生成类的内部名
	 * @param m        字段访问方法
	 * @param offset   字段在结构体中的字节偏移量
	 */
	private static void emitFieldMethod(ClassWriter cw, String implName, Method m, long offset) {
		MethodType mt = MethodType.methodType(m.getReturnType(), m.getParameterTypes());
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), mt.toMethodDescriptorString(), null, null);
		mv.visitCode();
		
		// this.ptr
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
		
		boolean getter = m.getParameterCount() == 0;
		Class<?> type = getter ? m.getReturnType() : m.getParameterTypes()[0];
		
		FieldTypeInfo info = FIELD_TYPES.get(type);
		if (info == null) {
			throw new IllegalStateException("Unsupported @Field type: " + type);
		}
		
		mv.visitFieldInsn(GETSTATIC, "java/lang/foreign/ValueLayout", info.layoutName(), info.layoutDesc());
		mv.visitLdcInsn(offset);
		
		if (getter) {
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "get", info.getDesc(), true);
			mv.visitInsn(info.returnOpcode());
		} else {
			mv.visitVarInsn(info.loadOpcode(), 1);
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "set", info.setDesc(), true);
			mv.visitInsn(RETURN);
		}
		
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	/**
	 * 生成 {@code @FieldView} 零拷贝视图方法。
	 * <p>
	 * 返回本地内存的 {@link MemorySegment} 切片，不拷贝数据。
	 * 用户通过 {@code MemorySegment.get/set} 按需读写，无分配、无拷贝。
	 * <p>
	 * 生成的代码等价于：
	 * <pre>{@code
	 * public MemorySegment fieldName() {
	 *     return this.ptr.asSlice(offset, size);
	 * }
	 * }</pre>
	 *
	 * @param cw       类写入器
	 * @param implName 生成类的内部名
	 * @param m        字段访问方法（必须返回 {@link MemorySegment}，无参）
	 * @param fv       {@code @FieldView} 注解实例
	 */
	private static void emitFieldViewMethod(ClassWriter cw, String implName, Method m, FieldView fv) {
		if (m.getReturnType() != MemorySegment.class) {
			throw new IllegalStateException("@FieldView must return MemorySegment");
		}
		if (m.getParameterCount() != 0) {
			throw new IllegalStateException("@FieldView must be a no-arg getter");
		}

		MethodType mt = MethodType.methodType(MemorySegment.class);
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), mt.toMethodDescriptorString(), null, null);
		mv.visitCode();

		// return this.ptr.asSlice(offset, size);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
		mv.visitLdcInsn(fv.offset());
		mv.visitLdcInsn(fv.size());
		mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "asSlice",
				"(JJ)Ljava/lang/foreign/MemorySegment;", true);
		mv.visitInsn(ARETURN);

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	/**
	 * 生成 {@code @FieldArray} 结构体数组字段的访问方法。
	 * <p>
	 * 支持 getter 和 setter 两种模式：
	 * <ul>
	 *   <li><b>getter</b>（无参，返回数组）：从本地内存切片拷贝到 Java 数组并返回</li>
	 *   <li><b>setter</b>（有参，返回 void）：将 Java 数组拷贝到本地内存切片</li>
	 * </ul>
	 * 支持的数组类型：{@code int[]}、{@code double[]}、{@code byte[]}。
	 *
	 * @param cw       类写入器
	 * @param implName 生成类的内部名
	 * @param m        字段访问方法
	 * @param fa       {@code @FieldArray} 注解实例
	 */
	private static void emitFieldArrayMethod(ClassWriter cw, String implName, Method m, FieldArray fa) {
		Class<?> rt = m.getReturnType();
		boolean getter = m.getParameterCount() == 0;

		if (getter && !rt.isArray()) {
			throw new IllegalStateException("@FieldArray getter must return array");
		}
		if (!getter && rt != void.class) {
			throw new IllegalStateException("@FieldArray setter must return void");
		}

		Class<?> ct = getter ? rt.getComponentType() : m.getParameterTypes()[0];
		long offset = fa.offset();
		int len = fa.length();

		int elemSize;
		String ofArrayDesc;
		if (ct == double.class) {
			elemSize = 8;
			ofArrayDesc = "([D)Ljava/lang/foreign/MemorySegment;";
		} else if (ct == int.class) {
			elemSize = 4;
			ofArrayDesc = "([I)Ljava/lang/foreign/MemorySegment;";
		} else if (ct == byte.class) {
			elemSize = 1;
			ofArrayDesc = "([B)Ljava/lang/foreign/MemorySegment;";
		} else {
			throw new IllegalStateException("Unsupported @FieldArray type: " + ct);
		}

		MethodType mt = MethodType.methodType(rt, getter ? new Class<?>[0] : new Class<?>[]{ct.arrayType()});
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), mt.toMethodDescriptorString(), null, null);
		mv.visitCode();

		if (getter) {
			// getter: native → Java
			// slot 0: this, slot 1: slice, slot 2: arr, slot 3: arrSeg

			// MemorySegment slice = this.ptr.asSlice(offset, len * elemSize);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
			mv.visitLdcInsn(offset);
			mv.visitLdcInsn((long) len * elemSize);
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "asSlice",
					"(JJ)Ljava/lang/foreign/MemorySegment;", true);
			mv.visitVarInsn(ASTORE, 1);

			// T[] arr = new T[len];
			mv.visitIntInsn(SIPUSH, len);
			int newType = ct == double.class ? T_DOUBLE : ct == byte.class ? T_BYTE : T_INT;
			mv.visitIntInsn(NEWARRAY, newType);
			mv.visitVarInsn(ASTORE, 2);

			// arrSeg = MemorySegment.ofArray(arr);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKESTATIC, MEMORY_SEGMENT, "ofArray", ofArrayDesc, false);
			mv.visitVarInsn(ASTORE, 3);

			// arrSeg.copyFrom(slice);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "copyFrom",
					"(Ljava/lang/foreign/MemorySegment;)V", true);

			// return arr;
			mv.visitVarInsn(ALOAD, 2);
			mv.visitInsn(ARETURN);
		} else {
			// setter: Java → native
			// slot 0: this, slot 1: array param, slot 2: slice, slot 3: arrSeg

			// MemorySegment slice = this.ptr.asSlice(offset, len * elemSize);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
			mv.visitLdcInsn(offset);
			mv.visitLdcInsn((long) len * elemSize);
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "asSlice",
					"(JJ)Ljava/lang/foreign/MemorySegment;", true);
			mv.visitVarInsn(ASTORE, 2);

			// MemorySegment arrSeg = MemorySegment.ofArray(arr);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESTATIC, MEMORY_SEGMENT, "ofArray", ofArrayDesc, false);
			mv.visitVarInsn(ASTORE, 3);

			// slice.copyFrom(arrSeg);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "copyFrom",
					"(Ljava/lang/foreign/MemorySegment;)V", true);

			mv.visitInsn(RETURN);
		}

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	/**
	 * 生成本地函数方法的实现。
	 * <p>
	 * 生成的代码流程：
	 * <ol>
	 *   <li>加载对应的 {@code MethodHandle} 静态字段</li>
	 *   <li>加载 {@code this.ptr}（实例方法）或跳过（静态方法/"创建"方法）</li>
	 *   <li>加载并转换参数（String→CString, 数组→MemorySegment）</li>
	 *   <li>调用 {@code MethodHandle.invokeExact}</li>
	 *   <li>处理返回值（基本类型直接返回，"创建"方法 reinterpret + 包装为接口实例）</li>
	 * </ol>
	 *
	 * @param cw         类写入器
	 * @param implName   生成类的内部名
	 * @param m          本地函数方法
	 * @param index      方法句柄在 {@code MH} 字段数组中的索引
	 * @param structSize 结构体字节大小（用于"创建"方法的 reinterpret）
	 */
	private static void emitMethod(ClassWriter cw, String implName, Method m, int index, long structSize) {
		MethodType javaMT = MethodType.methodType(m.getReturnType(), m.getParameterTypes());
		MethodType nativeMT = buildNativeMethodType(m);

		boolean isCreate = m.getReturnType().isInterface();
		boolean skipThis = m.isAnnotationPresent(Static.class) || Modifier.isStatic(m.getModifiers());
		boolean isJavaStatic = Modifier.isStatic(m.getModifiers());

		int access = ACC_PUBLIC | (isJavaStatic ? ACC_STATIC : 0);
		MethodVisitor mv = cw.visitMethod(access, m.getName(), javaMT.toMethodDescriptorString(), null, null);
		mv.visitCode();

		// 加载 MethodHandle
		mv.visitFieldInsn(GETSTATIC, implName, "MH" + index, "Ljava/lang/invoke/MethodHandle;");

		// 加载 this 指针（仅实例方法 + 非 @Static 方法）
		if (!isCreate && !skipThis) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
		}
		
		// 加载参数（静态方法从 slot 0 开始，实例方法从 slot 1 开始）
		emitLoadParams(mv, m.getParameterTypes(), isJavaStatic ? 0 : 1);
		
		// 调用本地函数
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", nativeMT.toMethodDescriptorString(), false);
		
		// 处理返回值
		if (isCreate) {
			emitCreateReturn(mv, implName, structSize);
		} else {
			emitReturn(mv, m.getReturnType());
		}
		
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	/**
	 * 生成参数加载指令。
	 * <p>
	 * 按照 JVM 调用约定逐个加载参数到操作数栈，同时处理类型转换：
	 * <ul>
	 *   <li>基本类型：直接 {@code xLOAD}</li>
	 *   <li>{@link String}：转换为 {@code MemorySegment}（通过 {@code FFMFactory.toCString}）</li>
	 *   <li>数组：转换为 {@code MemorySegment}（通过 {@code FFMFactory.toNative}）</li>
	 *   <li>其他引用类型：直接 {@code ALOAD}</li>
	 * </ul>
	 *
	 * @param mv     方法访问器
	 * @param params 参数类型数组
	 */
	private static void emitLoadParams(MethodVisitor mv, Class<?>[] params, int startSlot) {
		int slot = startSlot;
		for (Class<?> p : params) {
			if (p == boolean.class || p == int.class || p == byte.class || p == short.class) {
				mv.visitVarInsn(ILOAD, slot);
			} else if (p == long.class) {
				mv.visitVarInsn(LLOAD, slot);
			} else if (p == float.class) {
				mv.visitVarInsn(FLOAD, slot);
			} else if (p == double.class) {
				mv.visitVarInsn(DLOAD, slot);
			} else if (p == String.class) {
				mv.visitVarInsn(ALOAD, slot);
				mv.visitMethodInsn(INVOKESTATIC, FFM_FACTORY, "toCString", "(Ljava/lang/String;)Ljava/lang/foreign/MemorySegment;", false);
			} else if (p.isArray()) {
				mv.visitVarInsn(ALOAD, slot);
				String sig = "(" + p.descriptorString() + ")Ljava/lang/foreign/MemorySegment;";
				mv.visitMethodInsn(INVOKESTATIC, FFM_FACTORY, "toNative", sig, false);
			} else {
				mv.visitVarInsn(ALOAD, slot);
			}
			slot += (p == long.class || p == double.class) ? 2 : 1;
		}
	}
	
	/**
	 * 生成"创建"方法的返回处理。
	 * <p>
	 * 将本地函数返回的 {@code MemorySegment} 指针 reinterpret 为结构体大小，
	 * 然后通过私有构造函数包装为接口实例。
	 *
	 * @param mv         方法访问器
	 * @param implName   生成类的内部名
	 * @param structSize 结构体字节大小
	 */
	private static void emitCreateReturn(MethodVisitor mv, String implName, long structSize) {
		// ptr = ptr.reinterpret(structSize)
		mv.visitLdcInsn(structSize);
		mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "reinterpret", "(J)Ljava/lang/foreign/MemorySegment;", true);
		// new Impl(ptr)
		mv.visitTypeInsn(NEW, implName);
		mv.visitInsn(DUP_X1);
		mv.visitInsn(SWAP);
		mv.visitMethodInsn(INVOKESPECIAL, implName, "<init>", "(Ljava/lang/foreign/MemorySegment;)V", false);
		mv.visitInsn(ARETURN);
	}
	
	/**
	 * 生成基本类型/引用类型的返回指令。
	 *
	 * @param mv         方法访问器
	 * @param returnType Java 返回类型
	 */
	private static void emitReturn(MethodVisitor mv, Class<?> returnType) {
		if (returnType == void.class) {
			mv.visitInsn(RETURN);
		} else if (returnType == boolean.class || returnType == int.class) {
			mv.visitInsn(IRETURN);
		} else if (returnType == long.class) {
			mv.visitInsn(LRETURN);
		} else if (returnType == float.class) {
			mv.visitInsn(FRETURN);
		} else if (returnType == double.class) {
			mv.visitInsn(DRETURN);
		} else {
			mv.visitInsn(ARETURN);
		}
	}
	
	/**
	 * 构建本地函数的 {@link MethodType}，用于 {@code MethodHandle.invokeExact} 调用。
	 * <p>
	 * 将 Java 方法签名转换为本地函数签名：
	 * <ul>
	 *   <li>实例方法：插入 {@code MemorySegment} 作为第一个参数（this 指针）</li>
	 *   <li>{@link String} / 数组 / {@link MemorySegment} 参数：映射为 {@code MemorySegment}</li>
	 *   <li>引用类型返回值：映射为 {@code MemorySegment}</li>
	 * </ul>
	 *
	 * @param m Java 方法
	 * @return 本地函数的 {@link MethodType}
	 */
	private static MethodType buildNativeMethodType(Method m) {
		boolean isCreate = m.getReturnType().isInterface();
		boolean isStatic = m.isAnnotationPresent(Static.class) || Modifier.isStatic(m.getModifiers());
		
		Class<?>[] pts = m.getParameterTypes();
		boolean hasThis = !isCreate && !isStatic;
		Class<?>[] nativePts = new Class<?>[pts.length + (hasThis ? 1 : 0)];
		
		int i = 0;
		if (hasThis) {
			nativePts[i++] = MemorySegment.class;
		}
		for (Class<?> p : pts) {
			nativePts[i++] = isRefType(p) ? MemorySegment.class : p;
		}
		
		Class<?> rt = m.getReturnType();
		Class<?> nativeRt = (isCreate || isRefType(rt)) ? MemorySegment.class : rt;
		return MethodType.methodType(nativeRt, nativePts);
	}
	
	/**
	 * 判断 Java 类型是否为引用类型（数组、{@link String}、{@link MemorySegment}）。
	 * <p>
	 * 引用类型在本地调用中统一映射为 {@code MemorySegment}。
	 *
	 * @param c Java 类型
	 * @return 如果是引用类型返回 {@code true}
	 */
	private static boolean isRefType(Class<?> c) {
		return c.isArray() || c == String.class || c == MemorySegment.class;
	}
}
