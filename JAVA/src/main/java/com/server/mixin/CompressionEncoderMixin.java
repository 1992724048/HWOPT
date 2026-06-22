package com.server.mixin;

import library.dll.CompressNative;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.CompressionEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

@Mixin(CompressionEncoder.class)
public abstract class CompressionEncoderMixin {
	@Shadow
	private int threshold;
	
	@Unique
	private final ArrayDeque<byte[]> hwopt$batch = new ArrayDeque<>();
	@Unique
	private final ArrayDeque<io.netty.channel.ChannelPromise> hwopt$promises = new ArrayDeque<>();
	@Unique
	private int hwopt$pendingBytes = 0;
	@Unique
	private boolean hwopt$flushScheduled = false;
	
	@Unique
	private void hwopt$scheduleFlush(ChannelHandlerContext ctx) {
		if (hwopt$flushScheduled) return;
		hwopt$flushScheduled = true;
		ctx.executor().schedule(() -> hwopt$doFlush(ctx), 20, TimeUnit.MILLISECONDS);
	}
	
	@Unique
	private void hwopt$doFlush(ChannelHandlerContext ctx) {
		hwopt$flushScheduled = false;
		if (hwopt$batch.isEmpty()) return;
		
		int total = 4;
		for (byte[] p : hwopt$batch) total += 4 + p.length;
		
		ByteBuf batch = Unpooled.buffer(total);
		batch.writeInt(hwopt$batch.size());
		for (byte[] p : hwopt$batch) {
			batch.writeInt(p.length);
			batch.writeBytes(p);
		}
		
		byte[] raw = new byte[batch.readableBytes()];
		batch.readBytes(raw);
		batch.release();
		
		byte[] outBuf = new byte[raw.length + 8];
		int clen = CompressNative.INSTANCE.compress(raw, raw.length, outBuf, outBuf.length);
		
		ByteBuf result = Unpooled.buffer(clen + 8);
		net.minecraft.network.VarInt.write(result, raw.length);
		result.writeInt(clen);
		result.writeBytes(outBuf, 0, clen > 0 ? clen : 0);

		int pktCount = hwopt$batch.size();
		if (pktCount > 1 || total > 65536) {
			double ratio = clen > 0 ? (double) total / clen : 1.0;
			System.out.printf("[hwopt] batch: %d pkts, %d->%d bytes (%.1f:1)\n", pktCount, total, clen > 0 ? clen : total, ratio);
		}
		
		io.netty.channel.ChannelPromise last = hwopt$promises.peekLast();
		if (clen > 0) {
			ctx.write(result, last != null ? last : ctx.newPromise());
		} else {
			result.release();
			for (byte[] p : hwopt$batch) {
				ctx.write(Unpooled.wrappedBuffer(p), hwopt$promises.poll());
			}
		}
		
		hwopt$batch.clear();
		hwopt$promises.clear();
		hwopt$pendingBytes = 0;
	}
	
	@Overwrite
	protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
		int len = in.readableBytes();
		if (len < threshold) {
			net.minecraft.network.VarInt.write(out, 0);
			out.writeBytes(in);
			return;
		}
		
		byte[] data = new byte[len];
		in.readBytes(data);
		
		if (len > 65536) {
			byte[] buf = new byte[len + 8];
			int clen = CompressNative.INSTANCE.compress(data, len, buf, buf.length);
			if (clen > 0) {
				net.minecraft.network.VarInt.write(out, len);
				out.writeInt(clen);
				out.writeBytes(buf, 0, clen);
			} else {
				net.minecraft.network.VarInt.write(out, 0);
				out.writeBytes(data);
			}
			return;
		}
		
		hwopt$batch.add(data);
		hwopt$promises.add(ctx.newPromise());
		hwopt$pendingBytes += len;
		
		if (hwopt$pendingBytes >= 262144) {
			hwopt$doFlush(ctx);
		} else {
			hwopt$scheduleFlush(ctx);
		}
	}
}
