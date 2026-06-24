package com.server.misc.asynclogger;

import java.io.IOException;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static com.server.misc.asynclogger.ReflectionHelper.unreflectGetter;

public class ClassLoaderHandlerImpl extends ClassLoaderHandler {
    public ClassLoaderHandlerImpl(ClassLoader targetClassLoader, ClassLoader modClassLoader) {
        super(targetClassLoader, modClassLoader);
    }

    @Override
    public void removeModClassesFromServiceLayer(String packageName) {
        try {
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private FileSystem fileSystem;

    @Override
    protected Stream<Path> walkResource(URI resource) throws IOException {
        var s = resource.toString().split("!");
        fileSystem = FileSystems.newFileSystem(URI.create(s[0]), Map.of());
        var path = fileSystem.getPath(s[1]);
        return Files.walk(path);
    }

    @Override
    public void close() {
        if (fileSystem != null) {
            try {
                fileSystem.close();
            } catch (IOException ignored) {
            }
        }
    }
}
