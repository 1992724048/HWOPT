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
 * 使用 ASM 在运行时为 {@code @LibraryImport} 接口生成隐藏类实现。
 */
enum StubGenerator {
	;
	
	/**
	 * 字段类型对应的 ASM 描述符
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
	 * @param methods 接口中所有抽象方法
	 * @return 生成的类字节码
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
		cw.visit(V21, ACC_PUBLIC | ACC_FINAL, implName, null, "java/lang/Object", new String[]{apiName, "java/lang/AutoCloseable"});
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
	 * 生成 MethodHandle 静态字段和 {@code ptr} 字段。
	 */
	private static void emitFields(ClassWriter cw, String implName, int nativeMethodCount) {
		for (int i = 0; i < nativeMethodCount; i++) {
			cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "MH" + i, "Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();
		}
		cw.visitField(ACC_PRIVATE | ACC_FINAL, "ptr", "Ljava/lang/foreign/MemorySegment;", null, null).visitEnd();
	}
	
	/**
	 * 生成 {@code <clinit>}，通过 {@link FFMFactory#getHandle(int)} 绑定 MH 字段。
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
	 * 生成接受 {@link MemorySegment} 的私有构造函数，供"创建"方法使用。
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
	 * 生成无参默认构造函数，供 {@link FFMFactory#load} 使用。
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
	 * 生成 {@code close()} 方法，委托给接口中声明的 {@code destroy()}。
	 */
	private static void emitCloseMethod(ClassWriter cw, String implName) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "close", "()V", null, new String[]{"java/lang/Exception"});
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, implName, "destroy", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}
	
	/**
	 * 生成 {@code @Field} 的 getter/setter。
	 * 无参为 getter，单参数为 setter，直接读写 {@code ptr} 的偏移处内存。
	 */
	private static void emitFieldMethod(ClassWriter cw, String implName, Method m, long offset) {
		MethodType mt = MethodType.methodType(m.getReturnType(), m.getParameterTypes());
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), mt.toMethodDescriptorString(), null, null);
		mv.visitCode();
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
	 * 返回 {@code this.ptr.asSlice(offset, size)}。
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
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
		mv.visitLdcInsn(fv.offset());
		mv.visitLdcInsn(fv.size());
		mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "asSlice", "(JJ)Ljava/lang/foreign/MemorySegment;", true);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	/**
	 * 生成 {@code @FieldArray} 的 getter/setter。
	 * 支持 {@code int[]}/{@code double[]}/{@code byte[]} 类型的数组字段读写。
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
		
		// slot 0: this, slot 1: slice, slot 2: arr, slot 3: arrSeg
		if (getter) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
			mv.visitLdcInsn(offset);
			mv.visitLdcInsn((long) len * elemSize);
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "asSlice", "(JJ)Ljava/lang/foreign/MemorySegment;", true);
			mv.visitVarInsn(ASTORE, 1);
			mv.visitIntInsn(SIPUSH, len);
			int newType = ct == double.class ? T_DOUBLE : ct == byte.class ? T_BYTE : T_INT;
			mv.visitIntInsn(NEWARRAY, newType);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKESTATIC, MEMORY_SEGMENT, "ofArray", ofArrayDesc, true);
			mv.visitVarInsn(ASTORE, 3);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "copyFrom", "(Ljava/lang/foreign/MemorySegment;)V", true);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitInsn(ARETURN);
		} else {
			// slot 0: this, slot 1: array param, slot 2: slice, slot 3: arrSeg
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
			mv.visitLdcInsn(offset);
			mv.visitLdcInsn((long) len * elemSize);
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "asSlice", "(JJ)Ljava/lang/foreign/MemorySegment;", true);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESTATIC, MEMORY_SEGMENT, "ofArray", ofArrayDesc, true);
			mv.visitVarInsn(ASTORE, 3);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "copyFrom", "(Ljava/lang/foreign/MemorySegment;)V", true);
			mv.visitInsn(RETURN);
		}
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	/**
	 * 生成本地函数方法的实现。
	 *
	 * <p>对于包含 String 参数的方法，调用后会执行 {@link FFMFactory#endDowncall()}
	 * 以回收临时 native 内存。
	 */
	private static void emitMethod(ClassWriter cw, String implName, Method m, int index, long structSize) {
		MethodType javaMT = MethodType.methodType(m.getReturnType(), m.getParameterTypes());
		MethodType nativeMT = buildNativeMethodType(m);
		boolean isCreate = m.getReturnType().isInterface();
		boolean isSpanReturn = m.getReturnType().isArray();
		boolean skipThis = m.isAnnotationPresent(Static.class) || Modifier.isStatic(m.getModifiers());
		boolean isJavaStatic = Modifier.isStatic(m.getModifiers());
		boolean needsScope = hasTempAllocParams(m);
		int access = ACC_PUBLIC | (isJavaStatic ? ACC_STATIC : 0);
		MethodVisitor mv = cw.visitMethod(access, m.getName(), javaMT.toMethodDescriptorString(), null, null);
		mv.visitCode();

		mv.visitFieldInsn(GETSTATIC, implName, "MH" + index, "Ljava/lang/invoke/MethodHandle;");
		if (!isCreate && !skipThis) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
		}
		int startSlot = isJavaStatic ? 0 : 1;
		emitLoadParams(mv, m.getParameterTypes(), startSlot);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", nativeMT.toMethodDescriptorString(), false);

		if (needsScope) {
			mv.visitMethodInsn(INVOKESTATIC, FFM_FACTORY, "endDowncall", "()V", false);
		}

		if (isCreate) {
			emitCreateReturn(mv, implName, structSize);
		} else if (isSpanReturn) {
			emitSpanReturn(mv, m.getReturnType());
		} else {
			emitReturn(mv, m.getReturnType());
		}

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	/**
	 * 判断方法是否有需要临时内存分配的参数类型（String）。
	 */
	private static boolean hasTempAllocParams(Method m) {
		for (Class<?> p : m.getParameterTypes()) {
			if (p == String.class) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 生成参数加载指令。
	 * String → {@code toCStringTemp}、数组 → {@code MemorySegment.ofArray} + length。
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
				mv.visitMethodInsn(INVOKESTATIC, FFM_FACTORY, "toCStringTemp", "(Ljava/lang/String;)Ljava/lang/foreign/MemorySegment;", false);
			} else if (p.isArray()) {
				mv.visitVarInsn(ALOAD, slot);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/foreign/MemorySegment", "ofArray", "(" + p.descriptorString() + ")Ljava/lang/foreign/MemorySegment;", true);
				mv.visitVarInsn(ALOAD, slot);
				mv.visitInsn(ARRAYLENGTH);
			} else if (p == MemorySegment.class) {
				mv.visitVarInsn(ALOAD, slot);
			} else if (p.isInterface()) {
				mv.visitVarInsn(ALOAD, slot);
				String implName = p.getName().replace('.', '/') + "$FFM";
				mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
			} else {
				mv.visitVarInsn(ALOAD, slot);
			}
			slot += (p == long.class || p == double.class) ? 2 : 1;
		}
	}
	
	/**
	 * 生成"创建"方法的返回处理。
	 * 将返回的 {@link MemorySegment} reinterpret + 包装为接口实例。
	 */
	private static void emitCreateReturn(MethodVisitor mv, String implName, long structSize) {
		// ptr.reinterpret(structSize) → new Impl(ptr) → ARETURN
		mv.visitLdcInsn(structSize);
		mv.visitMethodInsn(INVOKEINTERFACE, MEMORY_SEGMENT, "reinterpret", "(J)Ljava/lang/foreign/MemorySegment;", true);
		mv.visitTypeInsn(NEW, implName);
		mv.visitInsn(DUP_X1);
		mv.visitInsn(SWAP);
		mv.visitMethodInsn(INVOKESPECIAL, implName, "<init>", "(Ljava/lang/foreign/MemorySegment;)V", false);
		mv.visitInsn(ARETURN);
	}
	
	/**
	 * 生成基本类型/引用类型的返回指令。
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
	 * 生成 {@code std::span} 返回值的处理代码。
	 * 通过 {@code FFMFactory.fromSpanXxx} 将 {@link MemorySegment} 转换为 Java 数组。
	 */
	private static void emitSpanReturn(MethodVisitor mv, Class<?> returnType) {
		Class<?> ct = returnType.getComponentType();
		String fromSpanMethod;
		String fromSpanDesc;
		if (ct == double.class) {
			fromSpanMethod = "fromSpanDouble";
			fromSpanDesc = "(Ljava/lang/foreign/MemorySegment;)[D";
		} else if (ct == int.class) {
			fromSpanMethod = "fromSpanInt";
			fromSpanDesc = "(Ljava/lang/foreign/MemorySegment;)[I";
		} else if (ct == float.class) {
			fromSpanMethod = "fromSpanFloat";
			fromSpanDesc = "(Ljava/lang/foreign/MemorySegment;)[F";
		} else if (ct == long.class) {
			fromSpanMethod = "fromSpanLong";
			fromSpanDesc = "(Ljava/lang/foreign/MemorySegment;)[J";
		} else if (ct == short.class) {
			fromSpanMethod = "fromSpanShort";
			fromSpanDesc = "(Ljava/lang/foreign/MemorySegment;)[S";
		} else if (ct == byte.class) {
			fromSpanMethod = "fromSpanByte";
			fromSpanDesc = "(Ljava/lang/foreign/MemorySegment;)[B";
		} else {
			throw new IllegalStateException("Unsupported span element type: " + ct);
		}
		mv.visitMethodInsn(INVOKESTATIC, FFM_FACTORY, fromSpanMethod, fromSpanDesc, false);
		mv.visitInsn(ARETURN);
	}
	
	/**
	 * 构建本地函数的 {@link MethodType}，用于 {@code MethodHandle.invokeExact}。
	 * 实例方法插入 {@code MemorySegment} 类型作为 this 指针参数。
	 * 数组参数展开为 MemorySegment + int 两个参数。
	 */
	private static MethodType buildNativeMethodType(Method m) {
		boolean isCreate = m.getReturnType().isInterface();
		boolean isStatic = m.isAnnotationPresent(Static.class) || Modifier.isStatic(m.getModifiers());
		Class<?>[] pts = m.getParameterTypes();
		boolean hasThis = !isCreate && !isStatic;
		List<Class<?>> nativePts = new ArrayList<>(pts.length + (hasThis ? 1 : 0));
		if (hasThis) {
			nativePts.add(MemorySegment.class);
		}
		for (Class<?> p : pts) {
			if (p.isArray()) {
				nativePts.add(MemorySegment.class);
				nativePts.add(int.class);
			} else if (isRefType(p)) {
				nativePts.add(MemorySegment.class);
			} else {
				nativePts.add(p);
			}
		}
		Class<?> rt = m.getReturnType();
		Class<?> nativeRt = (isCreate || isRefType(rt)) ? MemorySegment.class : rt;
		return MethodType.methodType(nativeRt, nativePts.toArray(Class<?>[]::new));
	}
	
	/**
	 * 判断类型是否在本地层映射为 {@link MemorySegment}。
	 */
	private static boolean isRefType(Class<?> c) {
		return c.isArray() || c == String.class || c == MemorySegment.class || c.isInterface();
	}
}
