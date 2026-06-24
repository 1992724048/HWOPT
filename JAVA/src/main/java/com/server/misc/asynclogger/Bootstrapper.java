package com.server.misc.asynclogger;

import com.hwpp.mod.Config;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

public class Bootstrapper {
    private static boolean bootstrapped;

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        try (var classLoaderHandler = new ClassLoaderHandlerImpl(Logger.class.getClassLoader(), Bootstrapper.class.getClassLoader())) {
            classLoaderHandler.expandModuleReads(Logger.class.getModule(), org.apache.logging.log4j.core.Logger.class.getModule());
        }

        initConfig();

        if (AsyncLogger.config.enabled) {
            LoggerConfigurator.configure();
        }

        cleanupOldLogs();
    }

    private static void cleanupOldLogs() {
        try {
            var logDir = Paths.get("logs");
            if (!Files.isDirectory(logDir)) return;
            var deadline = Instant.now().minus(Duration.ofDays(1));
            try (var files = Files.list(logDir)) {
                files.filter(p -> p.toString().endsWith(".log"))
                        .filter(p -> {
                            try {
                                return Files.getLastModifiedTime(p).toInstant().isBefore(deadline);
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }

    public static void applyConfig() {
        try {
            readModConfig();
        } catch (IllegalStateException e) {
            return;
        }

        if (!AsyncLogger.config.enabled) return;

        if (AsyncLogger.config.wrapSysOutSysErr) {
            System.setOut(new WrappedPrintStream("STDOUT", System.out));
            System.setErr(new WrappedPrintStream("STDERR", System.err));
        }
        if (AsyncLogger.config.useColors) {
            LoggerConfigurator.configureColors();
        }
    }

    private static void initConfig() {
        AsyncLogger.config = new AsyncLoggerConfig();
        try {
            readModConfig();
        } catch (IllegalStateException e) {
            // Config not loaded yet - defaults will be used until applyConfig() is called
        }
    }

    private static void readModConfig() {
        var cfg = Config.CONFIG;
        AsyncLogger.config.enabled = cfg.asyncLoggerEnabled.get();
        AsyncLogger.config.ringBufferSize = cfg.asyncLoggerRingBufferSize.get();
        AsyncLogger.config.waitStrategy = cfg.asyncLoggerWaitStrategy.get();
        AsyncLogger.config.synchronizeEnqueueWhenQueueFull = cfg.asyncLoggerSynchronizeEnqueueWhenQueueFull.get();
        AsyncLogger.config.formatMsgAsync = cfg.asyncLoggerFormatMsgAsync.get();
        AsyncLogger.config.asyncQueueFullPolicy = cfg.asyncLoggerAsyncQueueFullPolicy.get();
        AsyncLogger.config.discardThreshold = cfg.asyncLoggerDiscardThreshold.get();
        AsyncLogger.config.wrapSysOutSysErr = cfg.asyncLoggerWrapSysOutSysErr.get();
        AsyncLogger.config.testPerformance = cfg.asyncLoggerTestPerformance.get();
        AsyncLogger.config.useColors = cfg.asyncLoggerUseColors.get();
    }
}
