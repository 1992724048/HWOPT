package com.server.misc.asynclogger;

import java.lang.invoke.MethodHandle;

import static com.server.misc.asynclogger.ReflectionHelper.unreflect;

public class ClassLoaderHelper {
    public static final MethodHandle IMPL_ADD_READS_ALL_UNNAMED = unreflect(() -> Module.class.getDeclaredMethod("implAddReadsAllUnnamed"));
    public static final MethodHandle DEFINE_CLASS = unreflect(() -> ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class));

    public static void implAddReadsAllUnnamed(Module module) {
        try {
            IMPL_ADD_READS_ALL_UNNAMED.invokeExact(module);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> defineClass(ClassLoader classLoader, String name, byte[] bytes) throws Throwable {
        return (Class<?>) DEFINE_CLASS.invoke(classLoader, name, bytes, 0, bytes.length);
    }
}
