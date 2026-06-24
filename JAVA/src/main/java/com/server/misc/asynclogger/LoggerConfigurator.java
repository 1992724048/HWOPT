package com.server.misc.asynclogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.List;


public class LoggerConfigurator {
    static {
        var config = AsyncLogger.config;
        if (config.ringBufferSize > 0) {
            System.setProperty("log4j2.asyncLoggerRingBufferSize", String.valueOf(config.ringBufferSize));
        }
        else if (config.ringBufferSize == 0) {
            System.setProperty("log4j2.asyncLoggerRingBufferSize", String.valueOf(config.testPerformance ? 256 * 1024 : 8 * 1024));
        }
        if (!config.waitStrategy.isEmpty()) {
            System.setProperty("log4j2.asyncLoggerWaitStrategy", config.waitStrategy);
        }
        if (!config.synchronizeEnqueueWhenQueueFull.isEmpty()) {
            System.setProperty("log4j2.asyncLoggerSynchronizeEnqueueWhenQueueFull", config.synchronizeEnqueueWhenQueueFull);
        }
        if (!config.formatMsgAsync.isEmpty()) {
            System.setProperty("log4j2.formatMsgAsync", config.formatMsgAsync);
        }
        if (!config.asyncQueueFullPolicy.isEmpty()) {
            System.setProperty("log4j2.asyncQueueFullPolicy", config.asyncQueueFullPolicy);
        }
        if (!config.discardThreshold.isEmpty()) {
            System.setProperty("log4j2.discardThreshold", config.discardThreshold);
        }
        if (config.useColors) {
            System.setProperty("log4j2.locationInfo", "true");
        }
    }

    static void configure() {
        var test = AsyncLogger.config.testPerformance;
        List<LoggerTester.Result> before = null;
        List<LoggerTester.Result> after = null;
        if (test) {
            before = LoggerTester.testAll();
        }

        var selector = new BasicAsyncLoggerContextSelector();
        LogManager.setFactory(new Log4jContextFactory(selector));

        if (AsyncLogger.config.useColors) {
            configureColors();
        }

        var logger = LogManager.getLogger("AsyncLogger");
        if (AsyncLogger.config.wrapSysOutSysErr) {
            configureSysOutErr();
            logger.info("Successfully configured async logger context and wrapped System.out and System.err");
        }
        else {
            logger.info("Successfully configured async logger context");
        }

        if (test) {
            after = LoggerTester.testAll();
            logger.info("--- Test Results before applying AsyncLogger ---");
            for (var result : before) {
                logger.info("{}: {}ms", result.item(), result.elapsedTimeInMs());
            }
            logger.info("--- Test Results after applying AsyncLogger ---");
            for (var result : after) {
                logger.info("{}: {}ms", result.item(), result.elapsedTimeInMs());
            }
        }
    }

    static void configureSysOutErr() {
        System.setOut(new WrappedPrintStream("STDOUT", System.out));
        System.setErr(new WrappedPrintStream("STDERR", System.err));
    }

    static void configureColors() {
        var ctx = (LoggerContext) LogManager.getContext(false);
        var config = ctx.getConfiguration();
        var rootConfig = config.getRootLogger();

        var pattern = "◆ [%highlight{%level}] [%style{%logger{1}}{magenta}] %style{[%t]}{black} %style{%d{HH:mm:ss}}{black}%n%style{%msg}{bright_black}%n";
        var layout = PatternLayout.newBuilder()
                .withPattern(pattern)
                .withConfiguration(config)
                .build();

        var coloredAppender = ConsoleAppender.newBuilder()
                .setName("ColoredConsole")
                .setLayout(layout)
                .build();

        config.addAppender(coloredAppender);

        var it = rootConfig.getAppenders().entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue() instanceof ConsoleAppender) {
                rootConfig.removeAppender(entry.getKey());
            }
        }
        rootConfig.addAppender(coloredAppender, null, null);

        ctx.updateLoggers();
    }
}
