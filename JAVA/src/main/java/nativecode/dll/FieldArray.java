package nativecode.dll;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将接口方法映射到本地结构体中的数组字段。
 * <p>
 * 标注在 {@link LibraryImport} 接口的方法上，生成的代码会从本地内存中
 * 按偏移量切片并拷贝到 Java 数组。方法必须返回数组类型（{@code int[]} 或 {@code double[]}）。
 *
 * <pre>{@code
 * @FieldArray(offset = 32, length = 16)
 * int[] getData();  // 从偏移 32 处读取 16 个 int
 * }</pre>
 *
 * @see Field
 * @see LibraryImport#structSize()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FieldArray {
    /** 数组在结构体中的字节偏移量 */
    long offset();
    /** 数组元素个数 */
    int length();
}
