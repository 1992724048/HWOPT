package com.server.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
	@Shadow
	@Final
	private Channel channel;
	
	@Unique
	private final ArrayDeque<Packet<?>> hwopt$batch = new ArrayDeque<>();
	@Unique
	private boolean hwopt$flushScheduled = false;
	@Unique
	private int hwopt$pendingBytes = 0;
	
	@Unique
	private void hwopt$scheduleFlush() {
		if (hwopt$flushScheduled) return;
		hwopt$flushScheduled = true;
		channel.eventLoop().schedule(this::hwopt$doFlush, 20, TimeUnit.MILLISECONDS);
	}
	
	@Unique
	private void hwopt$doFlush() {
		hwopt$flushScheduled = false;
		if (hwopt$batch.isEmpty()) return;
		for (Packet<?> pkt : hwopt$batch) {
			channel.write(pkt, channel.newPromise());
		}
		channel.flush();
		hwopt$batch.clear();
		hwopt$pendingBytes = 0;
	}
	
	@Inject(method = "send", at = @At("HEAD"), cancellable = true)
	private void hwopt$onSend(Packet<?> packet, CallbackInfo ci) {
		if (channel.getClass().getName().contains("Local")) return;
		if (packet.isTerminal()) {
			hwopt$doFlush();
			return;
		}
		hwopt$batch.add(packet);
		hwopt$pendingBytes += 256;
		
		if (hwopt$batch.size() >= 20 || hwopt$pendingBytes >= 262144) {
			hwopt$doFlush();
		} else {
			hwopt$scheduleFlush();
		}
		ci.cancel();
	}
}
