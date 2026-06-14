package nativecode.dll;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.Cleaner;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FFM (Foreign Function & Memory) 本地库绑定工厂。
 * <p>
 * 通过注解驱动的方式，将本地 DLL 中的函数自动映射为 Java 接口实现。
 * 使用 ASM 在运行时生成接口的隐藏类实现，通过 {@code MethodHandle} 调用本地函数。
 *
 * <pre>{@code
 * @LibraryImport(dll = "hwlib.dll", structSize = 128)
 * interface MyApi {
 *     @Name("GetData")
 *     void getData(int[] buf);
 *
 *     @Field(offset = 0)
 *     int getValue();
 * }
 *
 * MyApi api = FFMFactory.load(MyApi.class);
 * }</pre>
 *
 * @see NativeLibrary
 * @see StubGenerator
 */
public final class FFMFactory {

    /** 全局内存区域，用于所有本地内存分配 */
    static final Arena ARENA = Arena.global();
    /** 本地链接器，用于构建本地函数下拉调用句柄 */
    static final Linker LINKER = Linker.nativeLinker();

    /** 已加载的本地库缓存，按 DLL 名称索引 */
    private static final Map<String, NativeLibrary> LIBRARY_CACHE = new HashMap<>();
    /** 已生成的字节码缓存，按接口类索引，避免重复生成 */
    private static final Map<Class<?>, byte[]> BYTECODE_CACHE = new HashMap<>();
    /** 当前正在绑定的本地库实例，供生成的字节码在类初始化时访问 */
    private static NativeLibrary currentLibrary;

    private FFMFactory() {}

    /**
     * 加载并生成指定本地 API 接口的实现。
     * <p>
     * 流程：
     * <ol>
     *   <li>验证接口标注了 {@code @LibraryImport}</li>
     *   <li>提取并加载对应的本地 DLL（首次调用时）</li>
     *   <li>通过 {@code JAVA_ResolveFunction} 解析每个本地符号</li>
     *   <li>生成接口实现类的字节码并实例化</li>
     * </ol>
     *
     * @param api 标注了 {@code @LibraryImport} 的接口类型
     * @param <T> 接口类型
     * @return 接口的本地实现实例
     * @throws IllegalArgumentException 如果 {@code api} 不是接口
     * @throws IllegalStateException    如果缺少 {@code @LibraryImport} 或 {@code @Name} 注解
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

        List<Method> allMethods = getAbstractMethods(api);
        List<Method> nativeMethods = filterNativeMethods(allMethods);
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
     * 获取指定索引的本地方法句柄。
     * <p>
     * 由生成的字节码在类初始化（{@code <clinit>}）阶段调用，
     * 用于将静态字段 {@code MH0, MH1, ...} 绑定到对应的本地函数句柄。
     *
     * @param index 方法句柄的索引，对应接口中本地方法的声明顺序
     * @return 对应的 {@link MethodHandle}
     * @throws IllegalStateException 如果当前没有正在绑定的本地库
     */
    public static MethodHandle getHandle(int index) {
        NativeLibrary lib = currentLibrary;
        if (lib == null) {
            throw new IllegalStateException("No active native library");
        }
        return lib.getHandle(index);
    }

    /**
     * 将 Java 字符串转换为以 null 结尾的本地 C 字符串。
     *
     * @param s 要转换的 Java 字符串
     * @return 包含 UTF-8 编码且以 {@code \0} 结尾的 {@link MemorySegment}
     */
    public static MemorySegment toCString(String s) {
        return ARENA.allocateFrom(s);
    }

    /**
     * 快速路径：纯 ASCII 字符串转 C 字符串，跳过 UTF-8 编码器开销。
     * <p>
     * 如果字符串只包含 ASCII 字符（每个 char ≤ 127），直接按字节拷贝，
     * 避免 {@code String.getBytes(StandardCharsets.UTF_8)} 的编码开销。
     * 非 ASCII 字符串自动回退到标准 {@link #toCString(String)}。
     *
     * @param s 要转换的 Java 字符串
     * @return 包含 UTF-8 编码且以 {@code \0} 结尾的 {@link MemorySegment}
     */
    public static MemorySegment toCStringFast(String s) {
        int len = s.length();
        boolean ascii = true;
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) > 127) {
                ascii = false;
                break;
            }
        }
        if (ascii) {
            MemorySegment seg = ARENA.allocate(len + 1);
            for (int i = 0; i < len; i++) {
                seg.set(ValueLayout.JAVA_BYTE, i, (byte) s.charAt(i));
            }
            seg.set(ValueLayout.JAVA_BYTE, len, (byte) 0);
            return seg;
        }
        return ARENA.allocateFrom(s);
    }

    /**
     * 将 Java 基本类型数组转换为本地 {@link MemorySegment}。
     * <p>
     * 用于在生成的字节码中将 Java 数组参数传递给本地函数。
     */
    public static MemorySegment toNative(byte[] arr) {
        return MemorySegment.ofArray(arr);
    }

    public static MemorySegment toNative(short[] arr) {
        return MemorySegment.ofArray(arr);
    }

    public static MemorySegment toNative(int[] arr) {
        return MemorySegment.ofArray(arr);
    }

    public static MemorySegment toNative(long[] arr) {
        return MemorySegment.ofArray(arr);
    }

    public static MemorySegment toNative(float[] arr) {
        return MemorySegment.ofArray(arr);
    }

    public static MemorySegment toNative(double[] arr) {
        return MemorySegment.ofArray(arr);
    }

    /**
     * 将 {@link MemorySegment} 数组转换为本地指针数组。
     * <p>
     * 分配一块连续内存，逐个写入每个 {@link MemorySegment} 的地址。
     *
     * @param arr 本地内存段数组
     * @return 包含所有指针的连续内存段
     */
    public static MemorySegment toNative(MemorySegment[] arr) {
        MemorySegment seg = ARENA.allocate(ValueLayout.ADDRESS.byteSize() * arr.length);
        for (int i = 0; i < arr.length; i++) {
            seg.setAtIndex(ValueLayout.ADDRESS, i, arr[i]);
        }
        return seg;
    }

    private static final Cleaner CLEANER = Cleaner.create();

    /**
     * 将本地对象注册到 {@link Cleaner}，当持有者被 GC 回收时自动调用 {@code close()}。
     * <p>
     * 生成的 stub 实现了 {@link AutoCloseable}，{@code close()} 会委托给接口中声明的 {@code destroy()}。
     *
     * @param owner  持有本地对象的 Java 对象（GC 回收时触发清理）
     * @param nativeObj 本地对象实例（实现了 AutoCloseable）
     * @param <T>    本地接口类型
     * @return 原样返回 {@code nativeObj}，便于链式调用
     */
    public static <T extends AutoCloseable> T trackCleaner(Object owner, T nativeObj) {
        CLEANER.register(owner, () -> {
            try {
                nativeObj.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to close native object", e);
            }
        });
        return nativeObj;
    }

    /**
     * 为指定的本地方法创建下拉调用句柄（downcall handle）。
     * <p>
     * 根据方法签名构建 {@link FunctionDescriptor}，通过 {@link Linker} 创建下拉调用句柄，
     * 并调整类型以匹配本地函数的调用约定。
     *
     * @param symbol 本地函数的符号地址
     * @param method 对应的 Java 方法
     * @return 调整后的 {@link MethodHandle}，可直接通过 {@code invokeExact} 调用
     */
    static MethodHandle createDowncallHandle(MemorySegment symbol, Method method) {
        FunctionDescriptor fd = buildDescriptor(method);
        MethodHandle raw = LINKER.downcallHandle(symbol, fd, Linker.Option.critical(true));
        return raw.asType(buildNativeMethodType(method));
    }
    
    /**
     * 获取接口中所有需要生成实现的方法（排除 Object 方法）。
     * <p>
     * 包括：抽象方法 + 带有 {@code @Name} 注解的 {@code static} 方法。
     */
    private static List<Method> getAbstractMethods(Class<?> api) {
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

    /**
     * 从抽象方法列表中过滤出需要绑定本地符号的方法（排除 {@code @Field}、{@code @FieldView} 和 {@code @FieldArray}）。
     */
    private static List<Method> filterNativeMethods(List<Method> methods) {
        List<Method> nativeMethods = new ArrayList<>();
        for (Method m : methods) {
            if (!m.isAnnotationPresent(Field.class)
                    && !m.isAnnotationPresent(FieldView.class)
                    && !m.isAnnotationPresent(FieldArray.class)) {
                nativeMethods.add(m);
            }
        }
        return nativeMethods;
    }

    /**
     * 判断方法是否为"创建"方法（返回接口类型，表示创建新的本地对象）。
     */
    private static boolean isCreateMethod(Method m) {
        return m.getReturnType().isInterface();
    }

    /**
     * 根据 Java 方法签名构建本地函数描述符 {@link FunctionDescriptor}。
     * <p>
     * 自动处理 this 指针（实例方法）、静态方法、基本类型和引用类型的映射。
     */
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

    /**
     * 构建本地函数的 {@link MethodType}，用于 {@link MethodHandle#asType} 适配。
     * <p>
     * 将 String、数组等引用类型统一映射为 {@link MemorySegment}，
     * 基本类型保持不变。
     */
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
            if (p.isArray() || p == String.class || p == MemorySegment.class) {
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

    private static final Map<Class<?>, MemoryLayout> PRIMITIVE_LAYOUTS = Map.of(
            byte.class, ValueLayout.JAVA_BYTE,
            short.class, ValueLayout.JAVA_SHORT,
            int.class, ValueLayout.JAVA_INT,
            long.class, ValueLayout.JAVA_LONG,
            float.class, ValueLayout.JAVA_FLOAT,
            double.class, ValueLayout.JAVA_DOUBLE,
            boolean.class, ValueLayout.JAVA_BOOLEAN
    );

    /**
     * 将 Java 类型映射为 FFM {@link MemoryLayout}。
     * <p>
     * 基本类型使用预定义的布局常量，引用类型（String、MemorySegment、接口、数组）
     * 统一映射为 {@link ValueLayout#ADDRESS}。
     *
     * @param c Java 类型
     * @return 对应的本地内存布局
     * @throws UnsupportedOperationException 如果类型不支持
     */
    private static MemoryLayout mapType(Class<?> c) {
        MemoryLayout layout = PRIMITIVE_LAYOUTS.get(c);
        if (layout != null) return layout;
        if (c == String.class || c == MemorySegment.class || c.isInterface() || c.isArray()) {
            return ValueLayout.ADDRESS;
        }
        throw new UnsupportedOperationException("Unsupported type: " + c);
    }
}
