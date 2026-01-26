package nativecode.dll;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

final class StubGenerator {

    static byte[] generate(Class<?> api, List<Method> methods) {
        String implName = api.getName().replace('.', '/') + "$FFM";

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21,
                ACC_PUBLIC | ACC_FINAL,
                implName,
                null,
                "java/lang/Object",
                new String[]{ api.getName().replace('.', '/') });

        for (int i = 0; i < methods.size(); i++) {
            cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                    "MH" + i,
                    "Ljava/lang/invoke/MethodHandle;",
                    null,
                    null).visitEnd();
        }

        emitClinit(cw, implName, methods.size());

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1,1);
        mv.visitEnd();

        for (int i = 0; i < methods.size(); i++) {
            emitMethod(cw, implName, methods.get(i), i);
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void emitClinit(ClassWriter cw, String implName, int count) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();

        for (int i = 0; i < count; i++) {
            mv.visitLdcInsn(i);
            mv.visitMethodInsn(INVOKESTATIC,
                    "nativecode/dll/FFMFactory",
                    "getHandle",
                    "(I)Ljava/lang/invoke/MethodHandle;",
                    false);

            mv.visitFieldInsn(PUTSTATIC,
                    implName,
                    "MH" + i,
                    "Ljava/lang/invoke/MethodHandle;");
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
    }

    private static void emitMethod(ClassWriter cw, String implName, Method m, int index) {
        MethodType javaMT = MethodType.methodType(
                m.getReturnType(),
                m.getParameterTypes()
        );
        MethodType nativeMT = buildNativeMethodType(m);

        String javaDesc = javaMT.toMethodDescriptorString();
        String nativeDesc = nativeMT.toMethodDescriptorString();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), javaDesc, null, null);
        mv.visitCode();

        mv.visitFieldInsn(GETSTATIC,
                implName,
                "MH" + index,
                "Ljava/lang/invoke/MethodHandle;");

        int slot = 1;
        for (Class<?> p : m.getParameterTypes()) {
            if (p == boolean.class) mv.visitVarInsn(ILOAD, slot);
            else if (p == int.class) mv.visitVarInsn(ILOAD, slot);
            else if (p == long.class) mv.visitVarInsn(LLOAD, slot);
            else if (p == float.class) mv.visitVarInsn(FLOAD, slot);
            else if (p == double.class) mv.visitVarInsn(DLOAD, slot);
            else if (p == String.class) {
                mv.visitVarInsn(ALOAD, slot);
                mv.visitMethodInsn(INVOKESTATIC,
                        "nativecode/dll/FFMFactory",
                        "toCString",
                        "(Ljava/lang/String;)Ljava/lang/foreign/MemorySegment;",
                        false);
            }
            else if (p.isArray()) {
                mv.visitVarInsn(ALOAD, slot);
                String sig = "(" + p.descriptorString() + ")Ljava/lang/foreign/MemorySegment;";
                mv.visitMethodInsn(INVOKESTATIC,
                        "nativecode/dll/FFMFactory",
                        "toNative",
                        sig,
                        false);
            }
            else { // MemorySegment
                mv.visitVarInsn(ALOAD, slot);
            }

            slot += (p == long.class || p == double.class) ? 2 : 1;
        }

        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandle",
                "invokeExact",
                nativeDesc,
                false);

        Class<?> r = m.getReturnType();
        if (r == void.class) mv.visitInsn(RETURN);
        else if (r == boolean.class) mv.visitInsn(IRETURN);
        else if (r == int.class) mv.visitInsn(IRETURN);
        else if (r == long.class) mv.visitInsn(LRETURN);
        else if (r == float.class) mv.visitInsn(FRETURN);
        else if (r == double.class) mv.visitInsn(DRETURN);
        else mv.visitInsn(ARETURN);

        mv.visitMaxs(0,0);
        mv.visitEnd();
    }

    private static MethodType buildNativeMethodType(Method m) {
        Class<?>[] pts = m.getParameterTypes();
        Class<?>[] nativePts = new Class<?>[pts.length];

        for (int i = 0; i < pts.length; i++) {
            Class<?> p = pts[i];
            if (p.isArray() || p == String.class || p == java.lang.foreign.MemorySegment.class)
                nativePts[i] = java.lang.foreign.MemorySegment.class;
            else
                nativePts[i] = p;
        }

        Class<?> rt = m.getReturnType();
        Class<?> nativeRt =
                (rt == String.class || rt.isArray())
                        ? java.lang.foreign.MemorySegment.class
                        : rt;

        return MethodType.methodType(nativeRt, nativePts);
    }
}
