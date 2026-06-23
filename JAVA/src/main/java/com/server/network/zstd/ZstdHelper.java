package com.server.network.zstd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import library.dll.CompressNative;
import net.minecraft.network.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ZstdHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZstdHelper");
    private static final boolean COMPRESS_NATIVE_AVAIL = detectNative();
    private static final boolean ZSTD_JNI_AVAIL = detectZstdJni();
    private static final Map<Connection, Object> contexts = new ConcurrentHashMap<>();

    private static boolean detectNative() {
        try {
            CompressNative.INSTANCE.getClass();
            LOGGER.info("CompressNative (hwopt.dll) available");
            return true;
        } catch (Throwable t) { return false; }
    }

    private static boolean detectZstdJni() {
        try { Class.forName("com.github.luben.zstd.ZstdCompressCtx"); return true; }
        catch (Throwable t) { return false; }
    }

    public static boolean isAvailable() { return COMPRESS_NATIVE_AVAIL || ZSTD_JNI_AVAIL; }

    public static ByteBuf compress(Connection connection, ByteBuf raw) {
        byte[] in = new byte[raw.readableBytes()];
        raw.getBytes(raw.readerIndex(), in);
        if (COMPRESS_NATIVE_AVAIL) {
            byte[] out = new byte[in.length + 1024];
            int len = CompressNative.INSTANCE.compress(in, out);
            if (len > 0) return Unpooled.wrappedBuffer(out, 0, len);
        }
        Deflater def = new Deflater(Deflater.BEST_SPEED);
        def.setInput(in); def.finish();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(in.length);
        byte[] buf = new byte[8192];
        while (!def.finished()) { int n = def.deflate(buf); baos.write(buf, 0, n); }
        def.end();
        return Unpooled.wrappedBuffer(baos.toByteArray());
    }

    public static ByteBuf decompress(Connection connection, ByteBuf compressed, int originalSize) {
        byte[] in = new byte[compressed.readableBytes()];
        compressed.getBytes(compressed.readerIndex(), in);
        if (COMPRESS_NATIVE_AVAIL) {
            byte[] out = new byte[originalSize > 0 ? originalSize : 65536];
            int len = CompressNative.INSTANCE.decompress(in, out);
            if (len > 0) return Unpooled.wrappedBuffer(out, 0, len);
        }
        Inflater inf = new Inflater(); inf.setInput(in);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(originalSize > 0 ? originalSize : 65536);
        byte[] buf = new byte[8192];
        try { while (!inf.finished()) { int n = inf.inflate(buf); baos.write(buf, 0, n); } }
        catch (Exception e) { LOGGER.error("Decompress fail", e); return null; }
        finally { inf.end(); }
        return Unpooled.wrappedBuffer(baos.toByteArray());
    }

    public static byte[] compress(byte[] raw) {
        ByteBuf r = compress(null, Unpooled.wrappedBuffer(raw));
        byte[] out = new byte[r.readableBytes()];
        r.getBytes(r.readerIndex(), out); return out;
    }

    public static byte[] decompress(byte[] compressed, int originalSize) {
        ByteBuf r = decompress(null, Unpooled.wrappedBuffer(compressed), originalSize);
        if (r == null) return null;
        byte[] out = new byte[r.readableBytes()];
        r.getBytes(r.readerIndex(), out); return out;
    }

    public static void clearCache(Connection connection) { contexts.remove(connection); }
}
