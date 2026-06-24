package com.server.network.mixin;

import com.server.network.util.NetworkTrafficTracker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import library.dll.CompressNative;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.VarInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(CompressionDecoder.class)
public abstract class CompressionDecoderMixin {
	
	@Overwrite
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
		int rawLen = VarInt.read(in);
		if (rawLen == 0) {
			NetworkTrafficTracker.recordReceived(in.readableBytes(), in.readableBytes());
			out.add(in.readBytes(in.readableBytes()));
			return;
		}
		if (rawLen > 8388608) {
			throw new IllegalArgumentException("Badly compressed packet - size of " + (rawLen / 1024) + "KB exceeds max");
		}
		
		int compressedLen = in.readableBytes();
		byte[] compressed = new byte[compressedLen];
		in.readBytes(compressed);
		
		NetworkTrafficTracker.recordReceived(compressedLen, rawLen);
		byte[] decompressed = new byte[rawLen];
		int actualLen = CompressNative.INSTANCE.decompress(compressed, decompressed);
		if (actualLen <= 0) {
			throw new IllegalArgumentException("Decompression failed");
		}
		
		out.add(Unpooled.wrappedBuffer(decompressed, 0, actualLen));
	}
}
