package nativecode.dll;

import java.lang.annotation.*;

/**
 * 标记一个接口为本地库绑定接口。
 * <p>
 * 被此注解标注的接口会通过 {@link FFMFactory#load(Class)} 自动生成实现类，
 * 将接口方法映射到指定 DLL 中的本地函数。
 *
 * <pre>{@code
 * @LibraryImport(dll = "hwlib.dll", structSize = 128)
 * interface HwLib {
 *     @Name("ReadRegister")
 *     int readRegister(int addr);
 * }
 * }</pre>
 *
 * @see FFMFactory#load(Class)
 * @see Name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LibraryImport {
    /** 本地 DLL 文件名（如 {@code "hwlib.dll"}） */
    String dll();
    /** 该库中结构体的字节大小，用于 {@code @Field} 和 {@code @FieldArray} 的内存切片 */
    long structSize();
}
