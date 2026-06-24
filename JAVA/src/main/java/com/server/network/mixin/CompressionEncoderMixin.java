package com.server.network.mixin;

import com.server.network.util.NetworkTrafficTracker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import library.dll.CompressNative;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.VarInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CompressionEncoder.class)
public abstract class CompressionEncoderMixin {
	@Shadow
	private int threshold;
	
	@Overwrite
	protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
		int len = in.readableBytes();
		if (len < threshold) {
			NetworkTrafficTracker.recordSent(len, len);
			VarInt.write(out, 0);
			out.writeBytes(in);
			return;
		}
		
		byte[] data = new byte[len];
		in.readBytes(data);
		
		byte[] buf = new byte[data.length * 2 + 64];
		int clen = CompressNative.INSTANCE.compress(data, buf);
		
		if (clen > 0 && clen < len) {
			NetworkTrafficTracker.recordSent(clen, len);
			VarInt.write(out, len);
			out.writeBytes(buf, 0, clen);
		} else {
			NetworkTrafficTracker.recordSent(len, len);
			VarInt.write(out, 0);
			out.writeBytes(data);
		}
	}
}
