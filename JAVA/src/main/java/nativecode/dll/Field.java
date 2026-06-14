package nativecode.dll;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将接口方法映射到本地结构体中的字段偏移量。
 * <p>
 * 标注在 {@link LibraryImport} 接口的方法上，生成的代码会直接通过内存偏移读写字段值。
 * 无参方法为 getter，单参数方法为 setter。
 *
 * <pre>{@code
 * @Field(offset = 0)
 * int getValue();          // getter: 读取偏移 0 处的 int
 *
 * @Field(offset = 0)
 * void setValue(int val);  // setter: 写入偏移 0 处的 int
 * }</pre>
 *
 * @see FieldArray
 * @see LibraryImport#structSize()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Field {
    /** 字段在结构体中的字节偏移量 */
    long offset();
}
