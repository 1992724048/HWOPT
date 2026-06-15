package nativecode.dll;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将接口方法映射到本地结构体中的内存切片（零拷贝视图）。
 * <p>
 * 与 {@link FieldArray} 不同，{@code @FieldView} 不会拷贝数据，
 * 而是返回一个指向本地内存的 {@link java.lang.foreign.MemorySegment} 切片。
 * 用户通过 {@code MemorySegment.get/set} 按需读写，无分配、无拷贝。
 *
 * <pre>{@code
 * @FieldView(offset = 0, size = 64)
 * MemorySegment buffer();  // 返回切片视图
 *
 * // 使用时按需读写，无拷贝开销
 * int val = api.buffer().get(ValueLayout.JAVA_INT, 0);
 * }</pre>
 *
 * @see Field
 * @see FieldArray
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FieldView {
    /** 字段在结构体中的字节偏移量 */
    long offset();
    /** 切片的字节大小 */
    long size();
}
