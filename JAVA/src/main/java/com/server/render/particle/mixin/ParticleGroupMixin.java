package com.server.render.particle.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import com.server.render.particle.core.AsyncTickBehavior;
import com.server.render.particle.addon.LightCachedParticleAddon;

@Mixin(ParticleGroup.class)
public abstract class ParticleGroupMixin {
	@Shadow
	@Final
	protected Queue<Particle> particles;

	@Inject(method = "tickParticles", at = @At("HEAD"), cancellable = true)
	private void hwopt$onTickParticles(CallbackInfo ci) {
		AsyncTickBehavior async = AsyncTickBehavior.getInstance();
		if (!async.isEnabled()) return;
		ci.cancel();
		ParticleGroup self = (ParticleGroup) (Object) this;
		if (particles.size() <= 64) {
			self.tickParticles();
			return;
		}
		async.dispatch(self, () -> {
			for (Particle p : particles) {
				try {
					p.tick();
					if (p instanceof LightCachedParticleAddon addon) {
						addon.hwopt$refreshLightCache();
					}
				} catch (Exception ignored) {
				}
			}
			particles.removeIf(p -> !p.isAlive());
		});
	}
}