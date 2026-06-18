package nativecode.dll;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.Cleaner;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FFMFactory {
	
	static final Arena ARENA = Arena.global();
	static final Linker LINKER = Linker.nativeLinker();
	
	private static final Map<String, NativeLibrary> LIBRARY_CACHE = new HashMap<>();
	private static final Map<Class<?>, byte[]> BYTECODE_CACHE = new HashMap<>();
	private static NativeLibrary currentLibrary;
	private static final Cleaner CLEANER = Cleaner.create();
	
	private FFMFactory() {
	}
	
	/**
	 * 加载并生成指定本地 API 接口的实现。
	 */
	public static <T> T load(Class<T> api) {
		if (!api.isInterface()) {
			throw new IllegalArgumentException("FFM API must be an interface");
		}
		LibraryImport lib = api.getAnnotation(LibraryImport.class);
		if (lib == null) {
			throw new IllegalStateException("Missing @LibraryImport on " + api);
		}
		NativeLibrary nativeLib = LIBRARY_CACHE.computeIfAbsent(lib.dll(), key -> {
			NativeLibraryLoader.extractOnce();
			return new NativeLibrary(NativeLibraryLoader.getDllPath(key));
		});
		List<Method> allMethods = Downcalls.getAbstractMethods(api);
		List<Method> nativeMethods = Downcalls.filterNativeMethods(allMethods);
		nativeLib.bind(nativeMethods);
		try {
			currentLibrary = nativeLib;
			byte[] bytecode = BYTECODE_CACHE.computeIfAbsent(api, key -> StubGenerator.generate(key, allMethods));
			var lookup = MethodHandles.privateLookupIn(api, MethodHandles.lookup());
			Class<?> impl = lookup.defineHiddenClass(bytecode, true).lookupClass();
			return (T) impl.getConstructor().newInstance();
		} catch (Throwable e) {
			throw new RuntimeException("Failed to generate implementation for " + api, e);
		} finally {
			currentLibrary = null;
		}
	}
	
	/**
	 * 由生成的字节码在 {@code <clinit>} 中调用，获取指定索引的本地方法句柄。
	 */
	public static MethodHandle getHandle(int index) {
		NativeLibrary lib = currentLibrary;
		if (lib == null) {
			throw new IllegalStateException("No active native library");
		}
		return lib.getHandle(index);
	}
	
	public static void endDowncall() {
		BumpAllocators.endDowncall();
	}
	
	public static MemorySegment tempAlloc(long size) {
		return BumpAllocators.tempAlloc(size);
	}
	
	public static MemorySegment toCString(String s) {
		return Strings.toCString(s);
	}
	
	public static MemorySegment toCStringTemp(String s) {
		return Strings.toCStringTemp(s);
	}
	
	public static double[] fromSpanDouble(MemorySegment spanPtr) {
		return SpanSupport.fromSpanDouble(spanPtr);
	}
	
	public static int[] fromSpanInt(MemorySegment spanPtr) {
		return SpanSupport.fromSpanInt(spanPtr);
	}
	
	public static float[] fromSpanFloat(MemorySegment spanPtr) {
		return SpanSupport.fromSpanFloat(spanPtr);
	}
	
	public static long[] fromSpanLong(MemorySegment spanPtr) {
		return SpanSupport.fromSpanLong(spanPtr);
	}
	
	public static short[] fromSpanShort(MemorySegment spanPtr) {
		return SpanSupport.fromSpanShort(spanPtr);
	}
	
	public static byte[] fromSpanByte(MemorySegment spanPtr) {
		return SpanSupport.fromSpanByte(spanPtr);
	}
	
	public static <T extends AutoCloseable> T trackCleaner(Object owner, T nativeObj) {
		return Cleanup.trackCleaner(owner, nativeObj);
	}
	
	static MethodHandle createDowncallHandle(MemorySegment symbol, Method method) {
		return Downcalls.createDowncallHandle(symbol, method);
	}
	
	/**
	 * Per-call bump allocator for temp allocations (String).
	 */
	static final class BumpAllocators {
		private static final int TEMP_BUF_SIZE = 16384;
		private static final ThreadLocal<MemorySegment> TEMP_BUF = ThreadLocal.withInitial(() -> ARENA.allocate(TEMP_BUF_SIZE));
		private static final ThreadLocal<int[]> TEMP_POS = ThreadLocal.withInitial(() -> new int[]{0});
		
		static void endDowncall() {
			TEMP_POS.get()[0] = 0;
		}
		
		static MemorySegment tempAlloc(long size) {
			int[] pos = TEMP_POS.get();
			int offset = pos[0];
			long needed = offset + size;
			MemorySegment buf = TEMP_BUF.get();
			if (needed > buf.byteSize()) {
				long newSize = Math.max(buf.byteSize() * 2, size);
				MemorySegment newBuf = ARENA.allocate(newSize);
				TEMP_BUF.set(newBuf);
				pos[0] = 0;
				return newBuf.asSlice(0, size);
			}
			pos[0] = (int) needed;
			return buf.asSlice(offset, size);
		}
	}
	
	/**
	 * C string conversion.
	 */
	static final class Strings {
		static MemorySegment toCString(String s) {
			return ARENA.allocateFrom(s);
		}
		
		static MemorySegment toCStringTemp(String s) {
			int len = s.length();
			boolean ascii = true;
			for (int i = 0; i < len; i++) {
				if (s.charAt(i) > 127) {
					ascii = false;
					break;
				}
			}
			if (ascii) {
				MemorySegment seg = BumpAllocators.tempAlloc(len + 1);
				for (int i = 0; i < len; i++) {
					seg.set(ValueLayout.JAVA_BYTE, i, (byte) s.charAt(i));
				}
				seg.set(ValueLayout.JAVA_BYTE, len, (byte) 0);
				return seg;
			}
			byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
			MemorySegment seg = BumpAllocators.tempAlloc(bytes.length + 1);
			MemorySegment.copy(bytes, 0, seg, ValueLayout.JAVA_BYTE, 0, bytes.length);
			seg.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
			return seg;
		}
	}
	
	/**
	 * 从 C++ 返回的 {@code std::span} 中读取数据到 Java 数组。
	 */
	static final class SpanSupport {
		static double[] fromSpanDouble(MemorySegment spanPtr) {
			if (spanPtr == MemorySegment.NULL) return new double[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new double[0];
			double[] result = new double[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 8));
			return result;
		}
		
		static int[] fromSpanInt(MemorySegment spanPtr) {
			if (spanPtr == MemorySegment.NULL) return new int[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new int[0];
			int[] result = new int[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 4));
			return result;
		}
		
		static float[] fromSpanFloat(MemorySegment spanPtr) {
			if (spanPtr == MemorySegment.NULL) return new float[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new float[0];
			float[] result = new float[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 4));
			return result;
		}
		
		static long[] fromSpanLong(MemorySegment spanPtr) {
			if (spanPtr == MemorySegment.NULL) return new long[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new long[0];
			long[] result = new long[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 8));
			return result;
		}
		
		static short[] fromSpanShort(MemorySegment spanPtr) {
			if (spanPtr == MemorySegment.NULL) return new short[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new short[0];
			short[] result = new short[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 2));
			return result;
		}
		
		static byte[] fromSpanByte(MemorySegment spanPtr) {
			if (spanPtr == MemorySegment.NULL) return new byte[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new byte[0];
			byte[] result = new byte[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size));
			return result;
		}
	}
	
	/**
	 * {@link Cleaner} registration for native object lifecycle.
	 */
	static final class Cleanup {
		static <T extends AutoCloseable> T trackCleaner(Object owner, T nativeObj) {
			CLEANER.register(owner, () -> {
				try {
					nativeObj.close();
				} catch (Exception e) {
					throw new RuntimeException("Failed to close native object", e);
				}
			});
			return nativeObj;
		}
	}
	
	/**
	 * Downcall handle creation and {@link FunctionDescriptor} / {@link MethodType} building.
	 */
	static final class Downcalls {
		private static final Map<Class<?>, MemoryLayout> PRIMITIVE_LAYOUTS = Map.of(byte.class, ValueLayout.JAVA_BYTE, short.class, ValueLayout.JAVA_SHORT, int.class, ValueLayout.JAVA_INT, long.class, ValueLayout.JAVA_LONG, float.class, ValueLayout.JAVA_FLOAT, double.class, ValueLayout.JAVA_DOUBLE, boolean.class, ValueLayout.JAVA_BOOLEAN);
		
		static MethodHandle createDowncallHandle(MemorySegment symbol, Method method) {
			FunctionDescriptor fd = buildDescriptor(method);
			MethodHandle raw = LINKER.downcallHandle(symbol, fd, Linker.Option.critical(true));
			return raw.asType(buildNativeMethodType(method));
		}
		
		static List<Method> getAbstractMethods(Class<?> api) {
			List<Method> methods = new ArrayList<>();
			for (Method m : api.getMethods()) {
				if (m.getDeclaringClass() != api) continue;
				if (Modifier.isAbstract(m.getModifiers())) {
					methods.add(m);
				} else if (Modifier.isStatic(m.getModifiers()) && m.isAnnotationPresent(Name.class)) {
					methods.add(m);
				}
			}
			return methods;
		}
		
		static List<Method> filterNativeMethods(List<Method> methods) {
			List<Method> nativeMethods = new ArrayList<>();
			for (Method m : methods) {
				if (!m.isAnnotationPresent(nativecode.dll.Field.class) && !m.isAnnotationPresent(FieldView.class) && !m.isAnnotationPresent(FieldArray.class)) {
					nativeMethods.add(m);
				}
			}
			return nativeMethods;
		}
		
		private static boolean isCreateMethod(Method m) {
			return m.getReturnType().isInterface();
		}
		
		private static FunctionDescriptor buildDescriptor(Method m) {
			boolean isStatic = m.isAnnotationPresent(Static.class) || Modifier.isStatic(m.getModifiers());
			boolean create = isCreateMethod(m);
			Class<?>[] pts = m.getParameterTypes();
			int base = (!create && !isStatic) ? 1 : 0;
			List<MemoryLayout> layouts = new ArrayList<>(pts.length + base);
			if (!create && !isStatic) {
				layouts.add(ValueLayout.ADDRESS);
			}
			for (Class<?> p : pts) {
				if (p.isArray()) {
					layouts.add(ValueLayout.ADDRESS);
					layouts.add(ValueLayout.JAVA_INT);
				} else {
					layouts.add(mapType(p));
				}
			}
			if (void.class == m.getReturnType()) {
				return FunctionDescriptor.ofVoid(layouts.toArray(MemoryLayout[]::new));
			}
			if (create || !m.getReturnType().isPrimitive()) {
				return FunctionDescriptor.of(ValueLayout.ADDRESS, layouts.toArray(MemoryLayout[]::new));
			}
			return FunctionDescriptor.of(mapType(m.getReturnType()), layouts.toArray(MemoryLayout[]::new));
		}
		
		private static MethodType buildNativeMethodType(Method m) {
			boolean create = isCreateMethod(m);
			boolean isStatic = m.isAnnotationPresent(Static.class) || Modifier.isStatic(m.getModifiers());
			Class<?>[] pts = m.getParameterTypes();
			int base = (!create && !isStatic) ? 1 : 0;
			List<Class<?>> nativePts = new ArrayList<>(pts.length + base);
			if (!create && !isStatic) {
				nativePts.add(MemorySegment.class);
			}
			for (Class<?> p : pts) {
				if (p.isArray()) {
					nativePts.add(MemorySegment.class);
					nativePts.add(int.class);
				} else if (p == String.class || p == MemorySegment.class || p.isInterface()) {
					nativePts.add(MemorySegment.class);
				} else {
					nativePts.add(p);
				}
			}
			Class<?> rt = m.getReturnType();
			if (create || !rt.isPrimitive()) {
				rt = MemorySegment.class;
			} else if (String.class == rt || rt.isArray()) {
				rt = MemorySegment.class;
			}
			return MethodType.methodType(rt, nativePts.toArray(Class<?>[]::new));
		}
		
		private static MemoryLayout mapType(Class<?> c) {
			MemoryLayout layout = PRIMITIVE_LAYOUTS.get(c);
			if (layout != null) return layout;
			if (c == String.class || c == MemorySegment.class || c.isInterface() || c.isArray()) {
				return ValueLayout.ADDRESS;
			}
			throw new UnsupportedOperationException("Unsupported type: " + c);
		}
	}
}
