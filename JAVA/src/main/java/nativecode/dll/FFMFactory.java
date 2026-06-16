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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;

public final class FFMFactory {

	static final Arena ARENA = Arena.global();
	static final Linker LINKER = Linker.nativeLinker();

	private static final Map<String, NativeLibrary> LIBRARY_CACHE = new HashMap<>();
	private static final Map<Class<?>, byte[]> BYTECODE_CACHE = new HashMap<>();
	private static NativeLibrary currentLibrary;
	private static final Cleaner CLEANER = Cleaner.create();

	private FFMFactory() {}

	/** 加载并生成指定本地 API 接口的实现。 */
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

	/** 由生成的字节码在 {@code <clinit>} 中调用，获取指定索引的本地方法句柄。 */
	public static MethodHandle getHandle(int index) {
		NativeLibrary lib = currentLibrary;
		if (lib == null) {
			throw new IllegalStateException("No active native library");
		}
		return lib.getHandle(index);
	}

	public static void beginDowncall() { BumpAllocators.beginDowncall(); }

	public static void endDowncall() { BumpAllocators.endDowncall(); }

	/** @deprecated 改用 {@link #endDowncall()} */
	@Deprecated
	public static void resetSpanAllocator() { endDowncall(); }

	public static MemorySegment tempAlloc(long size) { return BumpAllocators.tempAlloc(size); }

	public static MemorySegment toCString(String s) { return Strings.toCString(s); }

	public static MemorySegment toCStringFast(String s) { return Strings.toCStringFast(s); }

	public static MemorySegment toCStringTemp(String s) { return Strings.toCStringTemp(s); }

	public static MemorySegment toCStringFastTemp(String s) { return Strings.toCStringFastTemp(s); }

	public static MemorySegment toNative(byte[] arr) { return SpanSupport.toNative(arr); }
	public static MemorySegment toNative(short[] arr) { return SpanSupport.toNative(arr); }
	public static MemorySegment toNative(int[] arr) { return SpanSupport.toNative(arr); }
	public static MemorySegment toNative(long[] arr) { return SpanSupport.toNative(arr); }
	public static MemorySegment toNative(float[] arr) { return SpanSupport.toNative(arr); }
	public static MemorySegment toNative(double[] arr) { return SpanSupport.toNative(arr); }

	public static MemorySegment toNativeTemp(byte[] arr) { return SpanSupport.toNativeTemp(arr); }
	public static MemorySegment toNativeTemp(short[] arr) { return SpanSupport.toNativeTemp(arr); }
	public static MemorySegment toNativeTemp(int[] arr) { return SpanSupport.toNativeTemp(arr); }
	public static MemorySegment toNativeTemp(long[] arr) { return SpanSupport.toNativeTemp(arr); }
	public static MemorySegment toNativeTemp(float[] arr) { return SpanSupport.toNativeTemp(arr); }
	public static MemorySegment toNativeTemp(double[] arr) { return SpanSupport.toNativeTemp(arr); }

	public static MemorySegment toNative(MemorySegment[] arr) { return SpanSupport.toNative(arr); }

	public static MemorySegment toNativeInterface(Object[] arr) { return SpanSupport.toNativeInterface(arr); }

	public static MemorySegment toNativeInterfaceTemp(Object[] arr) { return SpanSupport.toNativeInterfaceTemp(arr); }

	public static double[] fromSpanDouble(MemorySegment spanPtr) { return SpanSupport.fromSpanDouble(spanPtr); }
	public static int[] fromSpanInt(MemorySegment spanPtr) { return SpanSupport.fromSpanInt(spanPtr); }
	public static float[] fromSpanFloat(MemorySegment spanPtr) { return SpanSupport.fromSpanFloat(spanPtr); }
	public static long[] fromSpanLong(MemorySegment spanPtr) { return SpanSupport.fromSpanLong(spanPtr); }
	public static short[] fromSpanShort(MemorySegment spanPtr) { return SpanSupport.fromSpanShort(spanPtr); }
	public static byte[] fromSpanByte(MemorySegment spanPtr) { return SpanSupport.fromSpanByte(spanPtr); }

	@SuppressWarnings("unchecked")
	public static <T> T[] fromSpanStruct(MemorySegment spanPtr, Class<T> elemType) {
		return SpanSupport.fromSpanStruct(spanPtr, elemType);
	}

	public static MemorySegment toNativeObject(Object obj) { return Structs.toNativeObject(obj); }

	public static MemorySegment toNativeObjectTemp(Object obj) { return Structs.toNativeObjectTemp(obj); }

	public static <T> T fromNativeObject(MemorySegment seg, Class<T> type) {
		return Structs.fromNativeObject(seg, type);
	}

	public static long sizeof(Class<?> type) { return Structs.sizeof(type); }

	static boolean isStructClass(Class<?> c) { return Structs.isStructClass(c); }

	static <T> T createStructInstance(Class<T> ifaceType, MemorySegment ptr) {
		return Structs.createStructInstance(ifaceType, ptr);
	}

	static MemorySegment getPtrField(Object obj) { return Structs.getPtrField(obj); }

	public static <T extends AutoCloseable> T trackCleaner(Object owner, T nativeObj) {
		return Cleanup.trackCleaner(owner, nativeObj);
	}

	static MethodHandle createDowncallHandle(MemorySegment symbol, Method method) {
		return Downcalls.createDowncallHandle(symbol, method);
	}

	/** Per-call bump allocators and downcall lifecycle. */
	static final class BumpAllocators {
		private static final int SPAN_BUF_SIZE = 4096;
		private static final ThreadLocal<MemorySegment> SPAN_BUF = ThreadLocal.withInitial(() -> ARENA.allocate(SPAN_BUF_SIZE));
		private static final ThreadLocal<int[]> SPAN_POS = ThreadLocal.withInitial(() -> new int[]{0});
		private static final int TEMP_BUF_SIZE = 16384;
		private static final ThreadLocal<MemorySegment> TEMP_BUF = ThreadLocal.withInitial(() -> ARENA.allocate(TEMP_BUF_SIZE));
		private static final ThreadLocal<int[]> TEMP_POS = ThreadLocal.withInitial(() -> new int[]{0});

		static void beginDowncall() {}

		static void endDowncall() {
			SPAN_POS.get()[0] = 0;
			TEMP_POS.get()[0] = 0;
		}

		@Deprecated
		static void resetSpanAllocator() { endDowncall(); }

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

		private static MemorySegment toSpanRaw(MemorySegment data, long size) {
			if (data.heapBase().isPresent()) {
				MemorySegment nativeCopy = ARENA.allocate(data.byteSize());
				nativeCopy.copyFrom(data);
				data = nativeCopy;
			}
			return writeSpan(data, size);
		}

		private static MemorySegment toSpanRawTemp(MemorySegment data, long size) {
			if (data.heapBase().isPresent()) {
				MemorySegment nativeCopy = tempAlloc(data.byteSize());
				nativeCopy.copyFrom(data);
				data = nativeCopy;
			}
			return writeSpan(data, size);
		}

		private static MemorySegment writeSpan(MemorySegment data, long size) {
			int[] pos = SPAN_POS.get();
			int offset = pos[0];
			pos[0] = offset + 16;
			if (pos[0] > SPAN_BUF_SIZE) {
				throw new IllegalStateException("Span buffer overflow: too many array params in one call");
			}
			MemorySegment span = SPAN_BUF.get().asSlice(offset, 16);
			span.set(ValueLayout.ADDRESS, 0, data);
			span.set(ValueLayout.JAVA_LONG, 8, size);
			return span;
		}
	}

	/** C string conversions (global arena and per-call temp variants). */
	static final class Strings {
		static MemorySegment toCString(String s) {
			return ARENA.allocateFrom(s);
		}

		static MemorySegment toCStringFast(String s) {
			int len = s.length();
			for (int i = 0; i < len; i++) {
				if (s.charAt(i) > 127) {
					return ARENA.allocateFrom(s);
				}
			}
			MemorySegment seg = ARENA.allocate(len + 1);
			for (int i = 0; i < len; i++) {
				seg.set(ValueLayout.JAVA_BYTE, i, (byte) s.charAt(i));
			}
			seg.set(ValueLayout.JAVA_BYTE, len, (byte) 0);
			return seg;
		}

		static MemorySegment toCStringTemp(String s) {
			byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
			MemorySegment seg = BumpAllocators.tempAlloc(bytes.length + 1);
			MemorySegment.copy(bytes, 0, seg, ValueLayout.JAVA_BYTE, 0, bytes.length);
			seg.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
			return seg;
		}

		static MemorySegment toCStringFastTemp(String s) {
			return toCStringTemp(s);
		}
	}

	/** Span ({@code std::span}) array conversions: Java array ↔ native span. */
	static final class SpanSupport {
		private static MemorySegment toNativeSeg(MemorySegment arrSeg, int len, boolean temp) {
			return temp
				? BumpAllocators.toSpanRawTemp(arrSeg, len)
				: BumpAllocators.toSpanRaw(arrSeg, len);
		}

		static MemorySegment toNative(byte[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, false); }
		static MemorySegment toNative(short[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, false); }
		static MemorySegment toNative(int[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, false); }
		static MemorySegment toNative(long[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, false); }
		static MemorySegment toNative(float[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, false); }
		static MemorySegment toNative(double[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, false); }

		static MemorySegment toNativeTemp(byte[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, true); }
		static MemorySegment toNativeTemp(short[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, true); }
		static MemorySegment toNativeTemp(int[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, true); }
		static MemorySegment toNativeTemp(long[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, true); }
		static MemorySegment toNativeTemp(float[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, true); }
		static MemorySegment toNativeTemp(double[] arr) { return toNativeSeg(MemorySegment.ofArray(arr), arr.length, true); }

		static MemorySegment toNative(MemorySegment[] arr) {
			MemorySegment seg = ARENA.allocate(ValueLayout.ADDRESS.byteSize() * arr.length);
			for (int i = 0; i < arr.length; i++) {
				seg.setAtIndex(ValueLayout.ADDRESS, i, arr[i]);
			}
			return seg;
		}

		private static MemorySegment toNativeInterfaceImpl(Object[] arr, LongFunction<MemorySegment> alloc, boolean temp) {
			if (arr.length == 0) return temp ? BumpAllocators.toSpanRawTemp(MemorySegment.NULL, 0) : BumpAllocators.toSpanRaw(MemorySegment.NULL, 0);
			Class<?> elemType = arr.getClass().getComponentType();
			LibraryImport lib = elemType.getAnnotation(LibraryImport.class);
			if (lib == null) {
				throw new IllegalArgumentException(elemType + " is not a @LibraryImport interface");
			}
			long structSize = lib.structSize();
			MemorySegment data = alloc.apply(structSize * arr.length);
			for (int i = 0; i < arr.length; i++) {
				MemorySegment elemPtr = Structs.getPtrField(arr[i]);
				if (elemPtr != null && elemPtr != MemorySegment.NULL) {
					data.asSlice(i * structSize, structSize).copyFrom(elemPtr.asSlice(0, structSize));
				}
			}
			return temp ? BumpAllocators.toSpanRawTemp(data, arr.length) : BumpAllocators.toSpanRaw(data, arr.length);
		}

		static MemorySegment toNativeInterface(Object[] arr) {
			return toNativeInterfaceImpl(arr, ARENA::allocate, false);
		}

		static MemorySegment toNativeInterfaceTemp(Object[] arr) {
			return toNativeInterfaceImpl(arr, FFMFactory::tempAlloc, true);
		}

		static double[] fromSpanDouble(MemorySegment spanPtr) {
			if (spanPtr == null || spanPtr == MemorySegment.NULL) return new double[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new double[0];
			double[] result = new double[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 8));
			return result;
		}

		static int[] fromSpanInt(MemorySegment spanPtr) {
			if (spanPtr == null || spanPtr == MemorySegment.NULL) return new int[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new int[0];
			int[] result = new int[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 4));
			return result;
		}

		static float[] fromSpanFloat(MemorySegment spanPtr) {
			if (spanPtr == null || spanPtr == MemorySegment.NULL) return new float[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new float[0];
			float[] result = new float[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 4));
			return result;
		}

		static long[] fromSpanLong(MemorySegment spanPtr) {
			if (spanPtr == null || spanPtr == MemorySegment.NULL) return new long[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new long[0];
			long[] result = new long[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 8));
			return result;
		}

		static short[] fromSpanShort(MemorySegment spanPtr) {
			if (spanPtr == null || spanPtr == MemorySegment.NULL) return new short[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new short[0];
			short[] result = new short[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size * 2));
			return result;
		}

		static byte[] fromSpanByte(MemorySegment spanPtr) {
			if (spanPtr == null || spanPtr == MemorySegment.NULL) return new byte[0];
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) return new byte[0];
			byte[] result = new byte[(int) size];
			MemorySegment.ofArray(result).copyFrom(data.asSlice(0, size));
			return result;
		}

		@SuppressWarnings("unchecked")
		static <T> T[] fromSpanStruct(MemorySegment spanPtr, Class<T> elemType) {
			if (spanPtr == null || spanPtr == MemorySegment.NULL) {
				return (T[]) java.lang.reflect.Array.newInstance(elemType, 0);
			}
			MemorySegment data = spanPtr.get(ValueLayout.ADDRESS, 0);
			long size = spanPtr.get(ValueLayout.JAVA_LONG, 8);
			if (size == 0) {
				return (T[]) java.lang.reflect.Array.newInstance(elemType, 0);
			}
			LibraryImport lib = elemType.getAnnotation(LibraryImport.class);
			if (lib == null) {
				throw new IllegalArgumentException(elemType + " is not a @LibraryImport interface");
			}
			long structSize = lib.structSize();
			T[] result = (T[]) java.lang.reflect.Array.newInstance(elemType, (int) size);
			for (int i = 0; i < size; i++) {
				MemorySegment elemCopy = ARENA.allocate(structSize);
				elemCopy.copyFrom(data.asSlice(i * structSize, structSize));
				result[i] = Structs.createStructInstance(elemType, elemCopy);
			}
			return result;
		}
	}

	/** Java object ↔ struct memory conversions and struct layout computation. */
	static final class Structs {
		private record StructField(java.lang.reflect.Field field, long offset, ValueLayout layout) {}

		private static final Map<Class<?>, ValueLayout> FIELD_LAYOUTS = Map.of(
			double.class, ValueLayout.JAVA_DOUBLE,
			float.class, ValueLayout.JAVA_FLOAT,
			long.class, ValueLayout.JAVA_LONG,
			int.class, ValueLayout.JAVA_INT,
			short.class, ValueLayout.JAVA_SHORT,
			byte.class, ValueLayout.JAVA_BYTE,
			boolean.class, ValueLayout.JAVA_BOOLEAN);

		private static final Map<Class<?>, List<StructField>> LAYOUT_CACHE = new ConcurrentHashMap<>();

		private static MemorySegment toNativeObjectImpl(Object obj, LongFunction<MemorySegment> alloc) {
			if (obj == null) return MemorySegment.NULL;
			List<StructField> fields = computeStructLayout(obj.getClass());
			if (fields.isEmpty()) return MemorySegment.NULL;
			long structSize = structSize(fields);
			MemorySegment seg = alloc.apply(structSize);
			for (StructField sf : fields) {
				Class<?> ft = sf.field.getType();
				long off = sf.offset;
				try {
					if (ft == double.class) seg.set(ValueLayout.JAVA_DOUBLE, off, sf.field.getDouble(obj));
					else if (ft == float.class) seg.set(ValueLayout.JAVA_FLOAT, off, sf.field.getFloat(obj));
					else if (ft == long.class) seg.set(ValueLayout.JAVA_LONG, off, sf.field.getLong(obj));
					else if (ft == int.class) seg.set(ValueLayout.JAVA_INT, off, sf.field.getInt(obj));
					else if (ft == short.class) seg.set(ValueLayout.JAVA_SHORT, off, sf.field.getShort(obj));
					else if (ft == byte.class) seg.set(ValueLayout.JAVA_BYTE, off, sf.field.getByte(obj));
					else if (ft == boolean.class)
						seg.set(ValueLayout.JAVA_BYTE, off, sf.field.getBoolean(obj) ? (byte) 1 : (byte) 0);
				} catch (Exception e) {
					throw new RuntimeException("Failed to read field " + sf.field.getName(), e);
				}
			}
			return seg;
		}

		static MemorySegment toNativeObject(Object obj) {
			return toNativeObjectImpl(obj, ARENA::allocate);
		}

		static MemorySegment toNativeObjectTemp(Object obj) {
			return toNativeObjectImpl(obj, FFMFactory::tempAlloc);
		}

		static <T> T fromNativeObject(MemorySegment seg, Class<T> type) {
			if (seg == null || seg == MemorySegment.NULL) return null;
			List<StructField> fields = computeStructLayout(type);
			if (fields.isEmpty()) return null;
			T obj;
			try {
				obj = type.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Failed to create instance of " + type + " (needs no-arg constructor)", e);
			}
			for (StructField sf : fields) {
				Class<?> ft = sf.field.getType();
				long off = sf.offset;
				try {
					if (ft == double.class) sf.field.setDouble(obj, seg.get(ValueLayout.JAVA_DOUBLE, off));
					else if (ft == float.class) sf.field.setFloat(obj, seg.get(ValueLayout.JAVA_FLOAT, off));
					else if (ft == long.class) sf.field.setLong(obj, seg.get(ValueLayout.JAVA_LONG, off));
					else if (ft == int.class) sf.field.setInt(obj, seg.get(ValueLayout.JAVA_INT, off));
					else if (ft == short.class) sf.field.setShort(obj, seg.get(ValueLayout.JAVA_SHORT, off));
					else if (ft == byte.class) sf.field.setByte(obj, seg.get(ValueLayout.JAVA_BYTE, off));
					else if (ft == boolean.class) sf.field.setBoolean(obj, seg.get(ValueLayout.JAVA_BYTE, off) != 0);
				} catch (Exception e) {
					throw new RuntimeException("Failed to set field " + sf.field.getName(), e);
				}
			}
			return obj;
		}

		static long sizeof(Class<?> type) {
			return structSize(computeStructLayout(type));
		}

		static boolean isStructClass(Class<?> c) {
			return !c.isPrimitive() && !c.isArray() && !c.isInterface()
				&& c != String.class && c != MemorySegment.class && c != Void.class;
		}

		private static List<StructField> computeStructLayout(Class<?> type) {
			return LAYOUT_CACHE.computeIfAbsent(type, t -> {
				List<StructField> result = new ArrayList<>();
				long offset = 0;
				for (java.lang.reflect.Field f : t.getDeclaredFields()) {
					if (Modifier.isStatic(f.getModifiers())) continue;
					ValueLayout layout = FIELD_LAYOUTS.get(f.getType());
					if (layout == null) continue;
					offset = alignUp(offset, layout.byteSize());
					f.setAccessible(true);
					result.add(new StructField(f, offset, layout));
					offset += layout.byteSize();
				}
				return result;
			});
		}

		private static long structSize(List<StructField> fields) {
			if (fields.isEmpty()) return 0;
			StructField last = fields.get(fields.size() - 1);
			return alignUp(last.offset + last.layout.byteSize(), 8);
		}

		private static long alignUp(long value, long alignment) {
			return (value + alignment - 1) & ~(alignment - 1);
		}

		@SuppressWarnings("unchecked")
		static <T> T createStructInstance(Class<T> ifaceType, MemorySegment ptr) {
			try {
				String implName = ifaceType.getName().replace('.', '/') + "$FFM";
				Class<?> implClass = Class.forName(implName);
				var ctor = implClass.getDeclaredConstructor(MemorySegment.class);
				ctor.setAccessible(true);
				return (T) ctor.newInstance(ptr);
			} catch (Exception e) {
				throw new RuntimeException("Failed to create struct instance for " + ifaceType, e);
			}
		}

		static MemorySegment getPtrField(Object obj) {
			try {
				var field = obj.getClass().getDeclaredField("ptr");
				field.setAccessible(true);
				return (MemorySegment) field.get(obj);
			} catch (Exception e) {
				return null;
			}
		}
	}

	/** {@link Cleaner} registration for native object lifecycle. */
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

	/** Downcall handle creation and {@link FunctionDescriptor} / {@link MethodType} building. */
	static final class Downcalls {
		private static final Map<Class<?>, MemoryLayout> PRIMITIVE_LAYOUTS = Map.of(
			byte.class, ValueLayout.JAVA_BYTE,
			short.class, ValueLayout.JAVA_SHORT,
			int.class, ValueLayout.JAVA_INT,
			long.class, ValueLayout.JAVA_LONG,
			float.class, ValueLayout.JAVA_FLOAT,
			double.class, ValueLayout.JAVA_DOUBLE,
			boolean.class, ValueLayout.JAVA_BOOLEAN);

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
				if (!m.isAnnotationPresent(nativecode.dll.Field.class)
					&& !m.isAnnotationPresent(FieldView.class)
					&& !m.isAnnotationPresent(FieldArray.class)) {
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
				layouts.add(mapType(p));
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
				if (p.isArray() || p == String.class || p == MemorySegment.class || p.isInterface()) {
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
