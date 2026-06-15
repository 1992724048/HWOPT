package nativecode.dll;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * 本地库提取与加载器。
 * <p>
 * 负责从 classpath（JAR）或开发目录中提取本地 DLL 文件到工作目录，
 * 并通过多轮尝试解决 DLL 之间的依赖加载顺序问题。
 *
 * <p>加载策略：
 * <ol>
 *   <li>将所有文件提取到 {@code {user.dir}/NativeDll/}</li>
 *   <li>对每个 DLL 文件尝试 {@code System.load()}，最多 3 轮</li>
 *   <li>后续轮次中，先前因依赖未加载而失败的 DLL 可能成功</li>
 * </ol>
 */
final class NativeLibraryLoader {
    private static final String NATIVE_DIR = "/native/win64";
    private static final String KNOWN_FILE = "hwopt.dll";
    private static final Path DLL_DIR = Paths.get(System.getProperty("user.dir"), "NativeDll");
    private static volatile boolean extracted = false;

    private NativeLibraryLoader() {}

    /**
     * 提取并加载所有本地库（仅执行一次）。
     * <p>
     * 首次调用时提取 DLL 文件并加载，后续调用直接返回。
     * 使用 {@code synchronized} 保证线程安全。
     */
    static synchronized void extractOnce() {
        if (extracted) return;
        try {
            extractAllFiles();
            loadAllDlls();
            extracted = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract native libraries", e);
        }
    }

    /**
     * 获取指定 DLL 文件的完整路径。
     *
     * @param dllName DLL 文件名（如 {@code "hwlib.dll"}）
     * @return DLL 文件的绝对路径
     */
    static Path getDllPath(String dllName) {
        return DLL_DIR.resolve(dllName);
    }

    /**
     * 从 classpath 或开发目录提取所有本地文件。
     * <p>
     * 优先从 classpath 中的 {@code /native/win64} 目录提取；
     * 如果找不到，则回退到开发目录 {@code ../src/main/resources/native/win64}。
     */
    private static void extractAllFiles() throws IOException {
        Files.createDirectories(DLL_DIR);

        URL fileUrl = FFMFactory.class.getResource(NATIVE_DIR + "/" + KNOWN_FILE);
        if (fileUrl == null) {
            extractFromDevDir();
            return;
        }

        if ("file".equals(fileUrl.getProtocol())) {
            try {
                Path nativeDir = Paths.get(fileUrl.toURI()).getParent();
                extractFromDirectory(nativeDir);
            } catch (java.net.URISyntaxException e) {
                throw new RuntimeException("Invalid native resource URL: " + fileUrl, e);
            }
        } else if ("jar".equals(fileUrl.getProtocol())) {
            URL dirUrl = new URL(fileUrl.getProtocol(), fileUrl.getHost(), fileUrl.getPort(),
                    fileUrl.getFile().substring(0, fileUrl.getFile().length() - KNOWN_FILE.length() - 1));
            extractFromJar(dirUrl);
        } else {
            throw new RuntimeException("Unsupported protocol: " + fileUrl.getProtocol());
        }
    }

    /**
     * 从开发目录提取文件（非打包环境的回退方案）。
     */
    private static void extractFromDevDir() throws IOException {
        Path devDir = Paths.get(System.getProperty("user.dir"),
                "../src/main/resources/native/win64").normalize();
        if (!Files.exists(devDir)) {
            throw new RuntimeException("Native resource not found in classpath or dev dir: "
                    + NATIVE_DIR + " / " + devDir);
        }
        extractFromDirectory(devDir);
    }

    /**
     * 从本地目录提取所有文件（开发环境，非 JAR）。
     */
    private static void extractFromDirectory(Path sourceDir) throws IOException {
        try (var stream = Files.walk(sourceDir)) {
            stream.filter(Files::isRegularFile).forEach(source -> {
                try {
                    copyIfDifferent(source.getFileName().toString(), Files.readAllBytes(source));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * 从 JAR 文件中提取本地资源。
     */
    private static void extractFromJar(URL url) throws IOException {
        var conn = (JarURLConnection) url.openConnection();
        var jar = conn.getJarFile();
        String prefix = conn.getEntryName();
        var entries = jar.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (!name.startsWith(prefix)) continue;
            String fileName = name.substring(prefix.length() + 1);
            try (InputStream in = jar.getInputStream(entry)) {
                copyIfDifferent(fileName, in.readAllBytes());
            }
        }
    }

    /**
     * 加载目录下所有 DLL，最多尝试 3 轮以解决依赖顺序问题。
     * <p>
     * 某些 DLL 可能依赖同目录下的其他 DLL，首轮加载时因依赖未就绪而失败。
     * 后续轮次中依赖已加载，之前失败的 DLL 可能成功。
     */
    private static void loadAllDlls() {
        List<Path> dllFiles;
        try (var stream = Files.list(DLL_DIR)) {
            dllFiles = stream
                    .filter(p -> p.toString().endsWith(".dll"))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list DLL directory: " + DLL_DIR, e);
        }

        if (dllFiles.isEmpty()) return;

        Set<String> loaded = new HashSet<>();

        for (int attempt = 0; attempt < 3; attempt++) {
            for (Path dll : dllFiles) {
                String name = dll.getFileName().toString();
                if (loaded.contains(name)) continue;
                try {
                    System.load(dll.toAbsolutePath().toString());
                    loaded.add(name);
                } catch (UnsatisfiedLinkError ignored) {
                }
            }
        }

        for (Path dll : dllFiles) {
            String name = dll.getFileName().toString();
            if (!loaded.contains(name)) {
                System.err.println("Warning: Failed to load " + name);
            }
        }
    }

    private static void copyIfDifferent(String fileName, byte[] newBytes) throws IOException {
        Path target = DLL_DIR.resolve(fileName);
        if (Files.exists(target)) {
            byte[] oldBytes = Files.readAllBytes(target);
            if (oldBytes.length == newBytes.length) {
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] oldHash = md.digest(oldBytes);
                    byte[] newHash = md.digest(newBytes);
                    if (Arrays.equals(oldHash, newHash)) {
                        return;
                    }
                } catch (java.security.NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
            Files.delete(target);
        }
        Files.createDirectories(target.getParent());
        Files.write(target, newBytes);
    }
}
