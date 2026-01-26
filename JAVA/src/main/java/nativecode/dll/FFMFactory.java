package nativecode.dll;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.ByteOrder;
import java.util.*;

public enum FFMFactory {
    ;

    private static final Linker LINKER = Linker.nativeLinker();
    static final Arena ARENA = Arena.global();

    public static MethodHandle[] CURRENT_HANDLES;

    public static <T> T load(Class<T> api) {
        if (!api.isInterface())
            throw new IllegalArgumentException("FFM API must be interface");

        LibraryImport lib = api.getAnnotation(LibraryImport.class);
        if (lib == null)
            throw new IllegalStateException("Missing @LibraryImport");

        SymbolLookup lookup = SymbolLookup.libraryLookup(lib.dll(), ARENA);

        try {
            List<Method> methods = new ArrayList<>();
            for (Method m : api.getMethods()) {
                if (m.getDeclaringClass() == Object.class) continue;
                if (!Modifier.isAbstract(m.getModifiers())) continue;
                methods.add(m);
            }

            MethodHandle[] handles = new MethodHandle[methods.size()];

            for (int i = 0; i < methods.size(); i++) {
                Method m = methods.get(i);
                Name name = m.getAnnotation(Name.class);
                if (name == null)
                    throw new IllegalStateException("Missing @Name on " + m);

                FunctionDescriptor fd = buildDescriptor(m);
                MemorySegment sym = lookup.find(name.value())
                        .orElseThrow(() ->
                                new UnsatisfiedLinkError("Symbol not found: " + name.value()));

                MethodHandle raw = LINKER.downcallHandle(sym, fd);
                MethodType nativeMT = buildNativeMethodType(m);
                handles[i] = raw.asType(nativeMT);
            }

            CURRENT_HANDLES = handles;

            byte[] bytecode = StubGenerator.generate(api, methods);
            MethodHandles.Lookup lookup2 =
                    MethodHandles.privateLookupIn(api, MethodHandles.lookup());

            Class<?> impl = lookup2.defineHiddenClass(bytecode, true).lookupClass();
            return (T) impl.getConstructor().newInstance();

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static FunctionDescriptor buildDescriptor(Method m) {
        MemoryLayout[] params = Arrays.stream(m.getParameterTypes())
                .map(FFMFactory::map)
                .toArray(MemoryLayout[]::new);

        if (m.getReturnType() == void.class)
            return FunctionDescriptor.ofVoid(params);
        return FunctionDescriptor.of(map(m.getReturnType()), params);
    }

    private static MethodType buildNativeMethodType(Method m) {
        Class<?>[] pts = m.getParameterTypes();
        Class<?>[] nativePts = new Class<?>[pts.length];

        for (int i = 0; i < pts.length; i++) {
            Class<?> p = pts[i];
            if (p.isArray() || p == String.class || p == MemorySegment.class)
                nativePts[i] = MemorySegment.class;
            else
                nativePts[i] = p;
        }

        Class<?> rt = m.getReturnType();
        Class<?> nativeRt =
                (rt == String.class || rt.isArray()) ? MemorySegment.class : rt;

        return MethodType.methodType(nativeRt, nativePts);
    }

    private static MemoryLayout map(Class<?> c) {
        if (c == int.class)    return ValueLayout.JAVA_INT;
        if (c == long.class)   return ValueLayout.JAVA_LONG;
        if (c == float.class)  return ValueLayout.JAVA_FLOAT;
        if (c == double.class) return ValueLayout.JAVA_DOUBLE;
        if (c == boolean.class) return ValueLayout.JAVA_BOOLEAN;

        if (c == String.class) return ValueLayout.ADDRESS;
        if (c == MemorySegment.class) return ValueLayout.ADDRESS;

        if (c.isArray()) {
            Class<?> ct = c.getComponentType();
            if (ct.isPrimitive() || ct == MemorySegment.class)
                return ValueLayout.ADDRESS;
        }

        throw new UnsupportedOperationException("Unsupported type: " + c);
    }

    public static MemorySegment toCString(String s) {
        return ARENA.allocateFrom(s);
    }

    public static MemorySegment toNative(byte[] arr) {
        MemorySegment seg = ARENA.allocate(ValueLayout.JAVA_BYTE.byteSize() * arr.length);
        seg.copyFrom(MemorySegment.ofArray(arr));
        return seg;
    }

    public static MemorySegment toNative(short[] arr) {
        MemorySegment seg = ARENA.allocate(ValueLayout.JAVA_SHORT.byteSize() *  arr.length);
        seg.asByteBuffer().order(ByteOrder.nativeOrder()).asShortBuffer().put(arr);
        return seg;
    }

    public static MemorySegment toNative(int[] arr) {
        MemorySegment seg = ARENA.allocate(ValueLayout.JAVA_INT.byteSize() *  arr.length);
        seg.asByteBuffer().order(ByteOrder.nativeOrder()).asIntBuffer().put(arr);
        return seg;
    }

    public static MemorySegment toNative(long[] arr) {
        MemorySegment seg = ARENA.allocate(ValueLayout.JAVA_LONG.byteSize() *  arr.length);
        seg.asByteBuffer().order(ByteOrder.nativeOrder()).asLongBuffer().put(arr);
        return seg;
    }

    public static MemorySegment toNative(float[] arr) {
        MemorySegment seg = ARENA.allocate(ValueLayout.JAVA_FLOAT.byteSize() * arr.length);
        seg.asByteBuffer().order(ByteOrder.nativeOrder()).asFloatBuffer().put(arr);
        return seg;
    }

    public static MemorySegment toNative(double[] arr) {
        MemorySegment seg = ARENA.allocate(ValueLayout.JAVA_DOUBLE.byteSize() *  arr.length);
        seg.asByteBuffer().order(ByteOrder.nativeOrder()).asDoubleBuffer().put(arr);
        return seg;
    }

    public static MemorySegment toNative(MemorySegment[] arr) {
        MemorySegment seg = ARENA.allocate(ValueLayout.ADDRESS.byteSize() *  arr.length);
        for (int i = 0; i < arr.length; i++)
            seg.setAtIndex(ValueLayout.ADDRESS, i, arr[i]);
        return seg;
    }

    public static MethodHandle getHandle(int i) {
        return CURRENT_HANDLES[i];
    }
}
