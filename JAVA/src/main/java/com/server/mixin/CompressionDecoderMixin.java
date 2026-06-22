package com.server.mixin;

import library.dll.CompressNative;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.VarInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(CompressionDecoder.class)
public abstract class CompressionDecoderMixin {
    @Shadow private int threshold;

    @Overwrite
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int rawLen = VarInt.read(in);
        if (rawLen == 0) {
            out.add(in.readBytes(in.readableBytes()));
            return;
        }
        if (rawLen > 8388608) {
            throw new IllegalArgumentException("Badly compressed packet - size of " + (rawLen / 1024) + "KB exceeds max");
        }

        int compressedLen = in.readInt();
        if (compressedLen > rawLen * 2) {
            throw new IllegalArgumentException("Corrupt compressed data");
        }

        byte[] compressed = new byte[compressedLen];
        in.readBytes(compressed);

        byte[] decompressed = new byte[rawLen];
        int actualLen = CompressNative.INSTANCE.decompress(compressed, compressedLen, decompressed, rawLen);
        if (actualLen <= 0) {
            throw new IllegalArgumentException("Decompression failed");
        }

        ByteBuf buf = Unpooled.wrappedBuffer(decompressed, 0, actualLen);
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            int pktLen = buf.readInt();
            out.add(buf.readBytes(pktLen));
        }
        buf.release();
    }
}
