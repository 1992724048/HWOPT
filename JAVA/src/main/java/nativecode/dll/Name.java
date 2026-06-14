package nativecode.dll;

import java.lang.annotation.*;

/**
 * 将接口方法映射到本地 DLL 中的函数名称。
 * <p>
 * 标注在 {@link LibraryImport} 接口的方法上，指定该方法对应的本地函数符号名。
 *
 * <pre>{@code
 * @Name("ReadRegister")
 * int readRegister(int addr);
 * }</pre>
 *
 * @see LibraryImport
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Name {
    /** 本地函数的符号名称 */
    String value();
}
