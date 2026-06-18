package nativecode.dll;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

/**
 * 封装单个本地 DLL 的加载状态。
 * <p>
 * 每个实例对应一个已加载的 DLL，持有该库的符号查找表、
 * {@code JAVA_ResolveFunction} 解析器句柄，以及绑定后的本地方法句柄数组。
 *
 * @see FFMFactory#load(Class)
 */
final class NativeLibrary {

    /** 符号查找器，用于在 DLL 中定位函数符号 */
    private final SymbolLookup lookup;
    /** 本地解析函数的句柄，用于通过名称动态查找符号 */
    private final MethodHandle resolver;
    /** 绑定后的本地方法句柄数组，索引与接口方法声明顺序对应 */
    private MethodHandle[] handles;

    /**
     * 加载指定路径的本地 DLL 并初始化符号查找。
     *
     * @param dllPath DLL 文件的绝对路径
     * @throws UnsatisfiedLinkError 如果 DLL 加载失败或 {@code JAVA_ResolveFunction} 符号不存在
     */
    NativeLibrary(Path dllPath) {
		this.lookup = SymbolLookup.libraryLookup(dllPath, FFMFactory.ARENA);
		this.resolver = resolveResolver();
	}
	
    /**
     * 将接口方法绑定到本地函数。
     * <p>
     * 遍历方法列表，通过 {@code @Name} 注解获取本地函数名，
     * 使用解析器查找符号地址，然后为每个方法创建下拉调用句柄。
     *
     * @param methods 需要绑定的本地方法列表（已排除 {@code @Field} 和 {@code @FieldArray}）
     * @throws RuntimeException 如果绑定过程中发生错误
     */
    void bind(List<Method> methods) {
		this.handles = new MethodHandle[methods.size()];
		try {
			for (int i = 0; i < methods.size(); i++) {
				Method m = methods.get(i);
				Name name = m.getAnnotation(Name.class);
				if (name == null) {
					throw new IllegalStateException("Missing @Name on " + m);
				}
				MemorySegment symbol = lookupSymbol(name.value());
				if (symbol == MemorySegment.NULL) {
					throw new UnsatisfiedLinkError("Symbol not found: " + name.value());
				}
				handles[i] = FFMFactory.createDowncallHandle(symbol, m);
			}
		} catch (Throwable e) {
			throw new RuntimeException("Failed to bind native methods", e);
		}
	}
	
    /**
     * 获取指定索引的本地方法句柄。
     *
     * @param index 方法句柄索引
     * @return 对应的 {@link MethodHandle}
     */
    MethodHandle getHandle(int index) {
		return handles[index];
	}
	
    /**
     * 解析本地库中的 {@code JAVA_ResolveFunction} 符号，
     * 该函数用于通过名称动态查找 DLL 中的其他函数。
     */
    private MethodHandle resolveResolver() {
		MemorySegment sym = lookup.find("JAVA_ResolveFunction").orElseThrow(() -> new UnsatisfiedLinkError("JAVA_ResolveFunction not found"));
		return FFMFactory.LINKER.downcallHandle(sym, FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
	}
	
    /**
     * 通过 {@code JAVA_ResolveFunction} 查找指定名称的本地符号。
     *
     * @param name 本地函数名称
     * @return 符号的内存地址
     * @throws UnsatisfiedLinkError 如果符号不存在
     */
    private MemorySegment lookupSymbol(String name) throws Throwable {
		MemorySegment ptr = (MemorySegment) resolver.invokeExact(FFMFactory.toCString(name));
		if (ptr == MemorySegment.NULL) {
			throw new UnsatisfiedLinkError("Symbol not found: " + name);
		}
		return ptr;
	}
}
