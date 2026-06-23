package com.server.render.particle.mixin;

import com.server.render.particle.util.BusyWaitEvictingQueue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ParticleEngine.class)
public class ParticleEngineQueueMixin {
    @Shadow @Mutable
    private Queue<?> trackingEmitters;
    @Shadow @Mutable
    private Queue<Particle> particlesToAdd;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hwopt$replaceQueues(CallbackInfo ci) {
        this.trackingEmitters = new BusyWaitEvictingQueue<>(256, 16384);
        this.particlesToAdd = new BusyWaitEvictingQueue<>(64, 256);
    }
}
