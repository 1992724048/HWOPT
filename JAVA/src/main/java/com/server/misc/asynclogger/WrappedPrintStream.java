package com.server.misc.asynclogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class WrappedPrintStream extends PrintStream {
    private final Logger LOGGER;
    private final StringBuffer buffer;
    private final AtomicInteger buffering;

    public WrappedPrintStream(String name, OutputStream out) {
        super(out);
        this.LOGGER = LogManager.getLogger(name);
        this.buffer = new StringBuffer();
        this.buffering = new AtomicInteger();
    }

    private void wrappedPrint(String s) {
        if (this.buffering.get() > 0) {
            this.buffer.append(s);
            return;
        }
        LOGGER.info(s);
    }

    @Override
    public void print(boolean b) {
        wrappedPrint(String.valueOf(b));
    }

    @Override
    public void print(char c) {
        wrappedPrint(String.valueOf(c));
    }

    @Override
    public void print(int i) {
        wrappedPrint(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        wrappedPrint(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        wrappedPrint(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        wrappedPrint(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        wrappedPrint(String.valueOf(s));
    }

    @Override
    public void print(String s) {
        wrappedPrint(String.valueOf(s));
    }

    @Override
    public void print(Object obj) {
        wrappedPrint(String.valueOf(obj));
    }

    @Override
    public void println() {
    }

    @Override
    public void println(boolean x) {
        this.print(x);
    }

    @Override
    public void println(char x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(int x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(long x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(float x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(double x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(char[] x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(String x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(Object x) {
        this.print(x);
        this.println();
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        buffering.getAndIncrement();
        var result = super.format(l, format, args);
        buffering.getAndDecrement();
        flushBuffer();
        return result;
    }

    @Override
    public PrintStream format(String format, Object... args) {
        buffering.getAndIncrement();
        var result = super.format(format, args);
        buffering.getAndDecrement();
        flushBuffer();
        return result;
    }

    private void flushBuffer() {
        if (buffering.get() == 0 && !buffer.isEmpty()) {
            print(buffer.toString());
            buffer.setLength(0);
        }
    }
}
