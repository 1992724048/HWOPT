package com.server.render.particle.mixin;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Queue;
import com.server.render.particle.util.BusyWaitEvictingQueue;

@Mixin(ParticleEngine.class)
public class ParticleEngineQueueMixin {
	private static int hwopt$queueInitCount;
	
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Queues;newArrayDeque()Ljava/util/ArrayDeque;", remap = false))
	@SuppressWarnings("MixinRedirect")
	private <E> Queue<E> hwopt$replaceQueue() {
		int idx = hwopt$queueInitCount++;
		if (idx == 0) {
			return new BusyWaitEvictingQueue<>(16384);
		}
		return new BusyWaitEvictingQueue<>(256);
	}
}
