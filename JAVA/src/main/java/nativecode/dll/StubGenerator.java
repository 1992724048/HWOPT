package nativecode.dll;

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Opcodes.*;

enum StubGenerator {
    ;
    
    static byte[] generate(Class<?> api, List<Method> methods) {
        String implName = api.getName().replace('.', '/') + "$FFM";
        String apiName = api.getName().replace('.', '/');
        
        List<Method> nativeMethods = new ArrayList<>();
        for (Method m : methods) {
            if (!m.isAnnotationPresent(Field.class)) {
                nativeMethods.add(m);
            }
        }
        
        LibraryImport lib = api.getAnnotation(LibraryImport.class);
        if (lib == null) {
            throw new IllegalStateException("Missing @LibraryImport");
        }
        
        long structSize = lib.structSize();
        if (structSize <= 0) {
            throw new IllegalStateException("structSize must be > 0");
        }
        
        if (structSize <= 0) {
            throw new IllegalStateException("No @Field(size=...) found");
        }
        
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V21, ACC_PUBLIC | ACC_FINAL, implName, null, "java/lang/Object", new String[]{apiName});
        
        for (int i = 0; i < nativeMethods.size(); i++) {
            cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "MH" + i, "Ljava/lang/invoke/MethodHandle;", null,
                          null).visitEnd();
        }
        
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "ptr", "Ljava/lang/foreign/MemorySegment;", null, null).visitEnd();
        
        emitClinit(cw, implName, nativeMethods.size());
        emitCtor(cw, implName);
        emitDefaultCtor(cw, implName);
        
        int nativeIndex = 0;
        for (Method m : methods) {
            if (m.isAnnotationPresent(Field.class)) {
                emitFieldMethod(cw, implName, m, m.getAnnotation(Field.class).offset());
            } else if (m.isAnnotationPresent(FieldArray.class)) {
                emitFieldArrayMethod(cw, implName, m, m.getAnnotation(FieldArray.class));
            } else {
                emitMethod(cw, implName, m, nativeIndex++, structSize);
            }
        }
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private static void emitClinit(ClassWriter cw, String implName, int count) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        for (int i = 0; i < count; i++) {
            mv.visitLdcInsn(i);
            mv.visitMethodInsn(INVOKESTATIC, "nativecode/dll/FFMFactory", "getHandle",
                               "(I)Ljava/lang/invoke/MethodHandle;", false);
            mv.visitFieldInsn(PUTSTATIC, implName, "MH" + i, "Ljava/lang/invoke/MethodHandle;");
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private static void emitCtor(ClassWriter cw, String implName) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "(Ljava/lang/foreign/MemorySegment;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private static void emitDefaultCtor(ClassWriter cw, String implName) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
    
    private static void emitFieldArrayMethod(ClassWriter cw, String implName, Method m, FieldArray fa) {
        
        Class<?> rt = m.getReturnType();
        if (!rt.isArray()) {
            throw new IllegalStateException("@FieldArray must return array");
        }
        
        Class<?> ct = rt.getComponentType();
        long offset = fa.offset();
        int len = fa.length();
        
        int elemSize;
        String ofArrayDesc;
        if (ct == double.class) {
            elemSize = 8;
            ofArrayDesc = "([D)Ljava/lang/foreign/MemorySegment;";
        } else if (ct == int.class) {
            elemSize = 4;
            ofArrayDesc = "([I)Ljava/lang/foreign/MemorySegment;";
        } else {
            throw new IllegalStateException("Unsupported @FieldArray type: " + ct);
        }
        
        MethodType mt = MethodType.methodType(rt);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), mt.toMethodDescriptorString(), null, null);
        
        mv.visitCode();
        
        mv.visitIntInsn(SIPUSH, len);
        mv.visitTypeInsn(NEWARRAY, String.valueOf(ct == double.class ? T_DOUBLE : T_INT));
        mv.visitVarInsn(ASTORE, 1);
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
        mv.visitLdcInsn(offset);
        mv.visitLdcInsn((long) len * elemSize);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/foreign/MemorySegment", "asSlice",
                           "(JJ)Ljava/lang/foreign/MemorySegment;", true);
        mv.visitVarInsn(ASTORE, 2);
        
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/foreign/MemorySegment", "ofArray", ofArrayDesc, false);
        mv.visitVarInsn(ASTORE, 3);
        
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/foreign/MemorySegment", "copyFrom",
                           "(Ljava/lang/foreign/MemorySegment;)V", true);
        
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private static void emitFieldMethod(ClassWriter cw, String implName, Method m, long offset) {
        
        MethodType mt = MethodType.methodType(m.getReturnType(), m.getParameterTypes());
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), mt.toMethodDescriptorString(), null, null);
        
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
        
        boolean getter = m.getParameterCount() == 0;
        
        Class<?> type = getter ? m.getReturnType() : m.getParameterTypes()[0];
        
        String layoutOwner = "java/lang/foreign/ValueLayout";
        String layoutName;
        String layoutDesc;
        String getDesc;
        String setDesc;
        int returnOpcode;
        int loadOpcode;
        
        if (type == int.class) {
            layoutName = "JAVA_INT";
            layoutDesc = "Ljava/lang/foreign/ValueLayout$OfInt;";
            getDesc = "(Ljava/lang/foreign/ValueLayout$OfInt;J)I";
            setDesc = "(Ljava/lang/foreign/ValueLayout$OfInt;JI)V";
            returnOpcode = IRETURN;
            loadOpcode = ILOAD;
        } else if (type == long.class) {
            layoutName = "JAVA_LONG";
            layoutDesc = "Ljava/lang/foreign/ValueLayout$OfLong;";
            getDesc = "(Ljava/lang/foreign/ValueLayout$OfLong;J)J";
            setDesc = "(Ljava/lang/foreign/ValueLayout$OfLong;JJ)V";
            returnOpcode = LRETURN;
            loadOpcode = LLOAD;
        } else if (type == double.class) {
            layoutName = "JAVA_DOUBLE";
            layoutDesc = "Ljava/lang/foreign/ValueLayout$OfDouble;";
            getDesc = "(Ljava/lang/foreign/ValueLayout$OfDouble;J)D";
            setDesc = "(Ljava/lang/foreign/ValueLayout$OfDouble;JD)V";
            returnOpcode = DRETURN;
            loadOpcode = DLOAD;
        } else if (type == float.class) {
            layoutName = "JAVA_FLOAT";
            layoutDesc = "Ljava/lang/foreign/ValueLayout$OfFloat;";
            getDesc = "(Ljava/lang/foreign/ValueLayout$OfFloat;J)F";
            setDesc = "(Ljava/lang/foreign/ValueLayout$OfFloat;JF)V";
            returnOpcode = FRETURN;
            loadOpcode = FLOAD;
        } else if (type == short.class) {
            layoutName = "JAVA_SHORT";
            layoutDesc = "Ljava/lang/foreign/ValueLayout$OfShort;";
            getDesc = "(Ljava/lang/foreign/ValueLayout$OfShort;J)S";
            setDesc = "(Ljava/lang/foreign/ValueLayout$OfShort;JS)V";
            returnOpcode = IRETURN;
            loadOpcode = ILOAD;
        } else if (type == byte.class) {
            layoutName = "JAVA_BYTE";
            layoutDesc = "Ljava/lang/foreign/ValueLayout$OfByte;";
            getDesc = "(Ljava/lang/foreign/ValueLayout$OfByte;J)B";
            setDesc = "(Ljava/lang/foreign/ValueLayout$OfByte;JB)V";
            returnOpcode = IRETURN;
            loadOpcode = ILOAD;
        } else if (type == boolean.class) {
            layoutName = "JAVA_BOOLEAN";
            layoutDesc = "Ljava/lang/foreign/ValueLayout$OfBoolean;";
            getDesc = "(Ljava/lang/foreign/ValueLayout$OfBoolean;J)Z";
            setDesc = "(Ljava/lang/foreign/ValueLayout$OfBoolean;JZ)V";
            returnOpcode = IRETURN;
            loadOpcode = ILOAD;
        } else {
            throw new IllegalStateException("Unsupported @Field type: " + type);
        }
        
        mv.visitFieldInsn(GETSTATIC, layoutOwner, layoutName, layoutDesc);
        mv.visitLdcInsn(offset);
        
        if (getter) {
            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/foreign/MemorySegment", "get", getDesc, true);
            mv.visitInsn(returnOpcode);
        } else {
            mv.visitVarInsn(loadOpcode, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/foreign/MemorySegment", "set", setDesc, true);
            mv.visitInsn(RETURN);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private static void emitMethod(ClassWriter cw, String implName, Method m, int index, long structSize) {
        
        MethodType javaMT = MethodType.methodType(m.getReturnType(), m.getParameterTypes());
        MethodType nativeMT = buildNativeMethodType(m);
        
        String javaDesc = javaMT.toMethodDescriptorString();
        String nativeDesc = nativeMT.toMethodDescriptorString();
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), javaDesc, null, null);
        
        mv.visitCode();
        
        boolean isCreate = m.getReturnType().isInterface();
        
        mv.visitFieldInsn(GETSTATIC, implName, "MH" + index, "Ljava/lang/invoke/MethodHandle;");
        
        boolean isStatic = m.isAnnotationPresent(Static.class);
        
        int slot = 1;
        if (!isCreate && !isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, implName, "ptr", "Ljava/lang/foreign/MemorySegment;");
        }
        
        for (Class<?> p : m.getParameterTypes()) {
            if (boolean.class == p || int.class == p || byte.class == p || short.class == p) {
                mv.visitVarInsn(ILOAD, slot);
            } else if (long.class == p) {
                mv.visitVarInsn(LLOAD, slot);
            } else if (float.class == p) {
                mv.visitVarInsn(FLOAD, slot);
            } else if (double.class == p) {
                mv.visitVarInsn(DLOAD, slot);
            } else if (String.class == p) {
                mv.visitVarInsn(ALOAD, slot);
                mv.visitMethodInsn(INVOKESTATIC, "nativecode/dll/FFMFactory", "toCString",
                                   "(Ljava/lang/String;)Ljava/lang/foreign/MemorySegment;", false);
            } else if (p.isArray()) {
                mv.visitVarInsn(ALOAD, slot);
                String sig = "(" + p.descriptorString() + ")Ljava/lang/foreign/MemorySegment;";
                mv.visitMethodInsn(INVOKESTATIC, "nativecode/dll/FFMFactory", "toNative", sig, false);
            } else {
                mv.visitVarInsn(ALOAD, slot);
            }
            
            slot += (p == long.class || p == double.class) ? 2 : 1;
        }
        
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", nativeDesc, false);
        
        if (isCreate) {
            mv.visitLdcInsn(structSize);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/foreign/MemorySegment", "reinterpret",
                               "(J)Ljava/lang/foreign/MemorySegment;", true);
            
            mv.visitTypeInsn(NEW, implName);
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, implName, "<init>", "(Ljava/lang/foreign/MemorySegment;)V", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return;
        }
        
        Class<?> r = m.getReturnType();
        if (void.class == r) {
            mv.visitInsn(RETURN);
        } else if (boolean.class == r || int.class == r) {
            mv.visitInsn(IRETURN);
        } else if (long.class == r) {
            mv.visitInsn(LRETURN);
        } else if (float.class == r) {
            mv.visitInsn(FRETURN);
        } else if (double.class == r) {
            mv.visitInsn(DRETURN);
        } else {
            mv.visitInsn(ARETURN);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private static MethodType buildNativeMethodType(Method m) {
        boolean isCreate = m.getReturnType().isInterface();
        boolean isStatic = m.isAnnotationPresent(Static.class);
        
        Class<?>[] pts = m.getParameterTypes();
        Class<?>[] nativePts =
                new Class<?>[pts.length + ((isCreate || isStatic) ? 0 : 1)];
        
        int i = 0;
        if (!isCreate && !isStatic) {
            nativePts[i++] = java.lang.foreign.MemorySegment.class;
        }
        
        for (Class<?> p : pts) {
            if (p.isArray() || p == String.class || p == java.lang.foreign.MemorySegment.class) {
                nativePts[i++] = java.lang.foreign.MemorySegment.class;
            } else {
                nativePts[i++] = p;
            }
        }
        
        Class<?> rt = m.getReturnType();
        Class<?> nativeRt =
                (isCreate || rt == String.class || rt.isArray())
                        ? java.lang.foreign.MemorySegment.class
                        : rt;
        
        return MethodType.methodType(nativeRt, nativePts);
    }
}
