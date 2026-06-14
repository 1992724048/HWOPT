package nativecode.dll;

import java.lang.annotation.*;

/**
 * 标记接口方法为静态本地函数。
 * <p>
 * 默认情况下，接口方法的第一个参数为 {@code this} 指针（{@link java.lang.foreign.MemorySegment}），
 * 表示实例方法。标注 {@code @Static} 后，生成的代码不会传递 {@code this} 指针，
 * 直接调用本地函数。
 *
 * <pre>{@code
 * @Name("GlobalInit")
 * @Static
 * void init();  // 调用时不传递 this 指针
 * }</pre>
 *
 * @see LibraryImport
 * @see Name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Static {}
