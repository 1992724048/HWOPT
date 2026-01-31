package nativecode.dll;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.*;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public enum FFMFactory {
    ;
    
    static final Arena ARENA = Arena.global();
    private static final Linker LINKER = Linker.nativeLinker();
    private static final Map<String, MethodHandle> RESOLVER_CACHE = new HashMap<>();
    public static MethodHandle[] CURRENT_HANDLES;
    public static boolean isWrite;
    
    public static <T> T load(Class<T> api) {
        if (!api.isInterface()) {
            throw new IllegalArgumentException("FFM API must be interface");
        }
        
        LibraryImport lib = api.getAnnotation(LibraryImport.class);
        if (lib == null) {
            throw new IllegalStateException("Missing @LibraryImport");
        }
        
        if (!isWrite) {
            try {
				// 防止缺少依赖
                ensureNativeDir();
                ensureNativeDir();
                isWrite = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        Path dllPath = Paths.get(System.getProperty("user.dir"), String.valueOf(Path.of("\\NativeDll\\" + lib.dll()))).toAbsolutePath();
        System.out.println(dllPath);
		System.load(dllPath.toString());
        SymbolLookup lookup = SymbolLookup.libraryLookup(dllPath, ARENA);
        
        try {
            MethodHandle resolverMH = getResolverHandle(dllPath.toString(), lookup);
            
            List<Method> allMethods = new ArrayList<>();
            for (Method m : api.getMethods()) {
                if (Object.class == m.getDeclaringClass()) {
                    continue;
                }
                if (!Modifier.isAbstract(m.getModifiers())) {
                    continue;
                }
                allMethods.add(m);
            }
            
            List<Method> nativeMethods = new ArrayList<>();
            for (Method m : allMethods) {
                if (!m.isAnnotationPresent(Field.class) && !m.isAnnotationPresent(FieldArray.class)) {
                    nativeMethods.add(m);
                }
            }
            
            MethodHandle[] handles = new MethodHandle[nativeMethods.size()];
            
            for (int i = 0; i < nativeMethods.size(); i++) {
                Method m = nativeMethods.get(i);
                
                Name name = m.getAnnotation(Name.class);
                if (name == null) {
                    throw new IllegalStateException("Missing @Name on " + m);
                }
                
                FunctionDescriptor fd = buildDescriptor(m);
                MemorySegment fnPtr = (MemorySegment) resolverMH.invokeExact(toCString(name.value()));
                
                if (fnPtr == MemorySegment.NULL) {
                    throw new UnsatisfiedLinkError("Symbol not found: " + name.value());
                }
                
                MethodHandle raw = LINKER.downcallHandle(fnPtr, fd, Linker.Option.critical(true));
                MethodType nativeMT = buildNativeMethodType(m);
                handles[i] = raw.asType(nativeMT);
            }
            
            CURRENT_HANDLES = handles;
            
            byte[] bytecode = StubGenerator.generate(api, allMethods);
            MethodHandles.Lookup lookup2 = MethodHandles.privateLookupIn(api, MethodHandles.lookup());
            Class<?> impl = lookup2.defineHiddenClass(bytecode, true).lookupClass();
            return (T) impl.getConstructor().newInstance();
            
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    
    private static MethodHandle getResolverHandle(String dllPath, SymbolLookup lookup) {
        return RESOLVER_CACHE.computeIfAbsent(dllPath, k -> {
            MemorySegment resolverSym = lookup.find("JAVA_ResolveFunction").orElseThrow(() -> new UnsatisfiedLinkError("JAVA_ResolveFunction not found"));
            
            return LINKER.downcallHandle(resolverSym, FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        });
    }
    
    private static boolean isCreate(Method m) {
        return m.getReturnType().isInterface();
    }
    
    private static FunctionDescriptor buildDescriptor(Method m) {
        boolean isStatic = m.isAnnotationPresent(Static.class);
        boolean create = isCreate(m);
        
        List<MemoryLayout> layouts = new ArrayList<>();
        if (!create && !isStatic) {
            layouts.add(ValueLayout.ADDRESS);
        }
        
        for (Class<?> p : m.getParameterTypes()) {
            layouts.add(map(p));
        }
        
        if (void.class == m.getReturnType()) {
            return FunctionDescriptor.ofVoid(layouts.toArray(MemoryLayout[]::new));
        }
        if (create || !m.getReturnType().isPrimitive()) {
            return FunctionDescriptor.of(ValueLayout.ADDRESS, layouts.toArray(MemoryLayout[]::new));
        }
        return FunctionDescriptor.of(map(m.getReturnType()), layouts.toArray(MemoryLayout[]::new));
    }
    
    private static MethodType buildNativeMethodType(Method m) {
        boolean create = isCreate(m);
        boolean isStatic = m.isAnnotationPresent(Static.class);
        
        List<Class<?>> pts = new ArrayList<>();
        
        if (!create && !isStatic) {
            pts.add(MemorySegment.class);
        }
        
        for (Class<?> p : m.getParameterTypes()) {
            if (p.isArray() || p == String.class || p == MemorySegment.class) {
                pts.add(MemorySegment.class);
            } else {
                pts.add(p);
            }
        }
        
        Class<?> rt = m.getReturnType();
        if (create || !rt.isPrimitive()) {
            rt = MemorySegment.class;
        } else if (String.class == rt || rt.isArray()) {
            rt = MemorySegment.class;
        }
        
        return MethodType.methodType(rt, pts.toArray(Class<?>[]::new));
    }
    
    private static MemoryLayout map(Class<?> c) {
        if (byte.class == c) {
            return ValueLayout.JAVA_BYTE;
        }
        if (short.class == c) {
            return ValueLayout.JAVA_SHORT;
        }
        if (int.class == c) {
            return ValueLayout.JAVA_INT;
        }
        if (long.class == c) {
            return ValueLayout.JAVA_LONG;
        }
        if (float.class == c) {
            return ValueLayout.JAVA_FLOAT;
        }
        if (double.class == c) {
            return ValueLayout.JAVA_DOUBLE;
        }
        if (boolean.class == c) {
            return ValueLayout.JAVA_BOOLEAN;
        }
        if (String.class == c) {
            return ValueLayout.ADDRESS;
        }
        if (MemorySegment.class == c) {
            return ValueLayout.ADDRESS;
        }
        if (c.isInterface()) {
            return ValueLayout.ADDRESS;
        }
        if (c.isArray()) {
            return ValueLayout.ADDRESS;
        }
        throw new UnsupportedOperationException("Unsupported type: " + c);
    }
    
    public static MemorySegment toCString(String s) {
        return ARENA.allocateFrom(s);
    }
    
    public static MemorySegment toNative(byte[] arr) {
        return MemorySegment.ofArray(arr);
    }
    
    public static MemorySegment toNative(short[] arr) {
        return MemorySegment.ofArray(arr);
    }
    
    public static MemorySegment toNative(int[] arr) {
        return MemorySegment.ofArray(arr);
    }
    
    public static MemorySegment toNative(long[] arr) {
        return MemorySegment.ofArray(arr);
    }
    
    public static MemorySegment toNative(float[] arr) {
        return MemorySegment.ofArray(arr);
    }
    
    public static MemorySegment toNative(double[] arr) {
        return MemorySegment.ofArray(arr);
    }
    
    public static MemorySegment toNative(MemorySegment[] arr) {
        MemorySegment seg = ARENA.allocate(ValueLayout.ADDRESS.byteSize() * arr.length);
        for (int i = 0; i < arr.length; i++) {
            seg.setAtIndex(ValueLayout.ADDRESS, i, arr[i]);
        }
        return seg;
    }
    
    public static MethodHandle getHandle(int i) {
        return CURRENT_HANDLES[i];
    }
    
    private static void ensureNativeDir() throws IOException {
        final String resourceDir = "/native/win64";
        Path outDir = Paths.get(System.getProperty("user.dir"));
        Files.createDirectories(outDir);
        
        URL url;
        try {
            url = FFMFactory.class.getResource(resourceDir);
            if (url == null) {
                throw new RuntimeException("Resource dir not found in classpath: " + resourceDir);
            }
        } catch (Exception e) {
            Path devDir = Paths.get(System.getProperty("user.dir")).resolve("../src/main/resources/native/win64").normalize();
            
            if (!Files.exists(devDir)) {
                throw new RuntimeException("Resource not found in classpath, and dev dir also not found: " + devDir, e);
            }
            
            file_filter(outDir, devDir);
            return;
        }
        
        try {
            if ("file".equals(url.getProtocol())) {
                Path dir = Paths.get(url.toURI());
                file_filter(outDir, dir);
                
            } else if ("jar".equals(url.getProtocol())) {
                var conn = (JarURLConnection) url.openConnection();
                var jar = conn.getJarFile();
                String prefix = conn.getEntryName();
                
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    var e = entries.nextElement();
                    if (e.isDirectory()) {
                        continue;
                    }
                    
                    String name = e.getName();
                    if (!name.startsWith(prefix)) {
                        continue;
                    }
                    
                    String fileName = name.substring(prefix.length() + 1);
                    try (InputStream in = jar.getInputStream(e)) {
                        copyIfDifferent(fileName, in.readAllBytes(), Path.of(outDir + "\\NativeDll\\"));
                    }
                }
                
            } else {
                throw new RuntimeException("Unsupported protocol: " + url.getProtocol());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void file_filter(Path outDir, Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                try {
					String fileName = p.getFileName().toString();
                    copyIfDifferent(fileName, Files.readAllBytes(p), Path.of(outDir + "\\NativeDll\\"));
	                if (fileName.endsWith(".dll")) {
		                try {
			                System.load(outDir + "\\NativeDll\\" + fileName);
		                } catch (UnsatisfiedLinkError | NullPointerException | IllegalCallerException ex) {
			                System.out.println("Failed to load " + fileName);
		                }
	                }
				} catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
    
    private static void copyIfDifferent(String fileName, byte[] newBytes, Path outDir) throws IOException {
        Path out = outDir.resolve(fileName);
        if (Files.exists(out)) {
            byte[] oldBytes = Files.readAllBytes(out);
            try {
                if (Arrays.equals(sha256(oldBytes), sha256(newBytes))) {
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Files.delete(out);
        }
	    
	    File dir = new File(out.getParent().toString());
	    dir.mkdirs();
        Files.write(out, newBytes);
    }
    
    private static byte[] sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(data);
    }
}
