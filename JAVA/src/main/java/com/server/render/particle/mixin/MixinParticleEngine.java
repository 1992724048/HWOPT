package com.server.render.particle.mixin;

import com.server.render.particle.addon.LightCachedParticleAddon;
import com.server.render.particle.addon.ParticleEngineAddon;
import com.server.render.particle.addon.ParticleGroupAddition;
import com.server.render.particle.config.ConfigHelper;
import com.server.render.particle.core.AsyncTickBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Shadow
	@Final
	private Queue<TrackingEmitter> trackingEmitters;

	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	@Shadow
	@Final
	public Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Shadow
	protected abstract ParticleGroup<?> createParticleGroup(ParticleRenderType type);

	@Inject(method = "add", at = @At(value = "HEAD"))
	public void hwopt$add(Particle p, CallbackInfo ci) {
		if (ConfigHelper.particleLightCache()) {
			((LightCachedParticleAddon) p).asyncparticles$refresh();
			((LightCachedParticleAddon) p).asyncparticles$enableLightCache();
		}
	}

	@Overwrite
	public void tick() {
		if (!AsyncTickBehavior.getInstance().shouldTickParticleEngine()) return;

		Particle particle;
		boolean tickAsync = AsyncTickBehavior.getInstance().isEnabled();

		if (!particlesToAdd.isEmpty()) {
			for (Iterator<Particle> iterator = particlesToAdd.iterator(); iterator.hasNext(); ) {
				particle = iterator.next();
				ParticleRenderType renderType = particle.getGroup();
				ParticleGroup<?> group = this.particles.computeIfAbsent(renderType, this::createParticleGroup);
				group.add(particle);
			}
			particlesToAdd.clear();
		}

		for (Map.Entry<ParticleRenderType, ParticleGroup<?>> entry : this.particles.entrySet()) {
			ParticleGroup<?> group = entry.getValue();
			if (group.isEmpty()) continue;
			ParticleRenderType renderType = entry.getKey();
			Profiler.get().push(renderType.name());
			if (tickAsync) {
				AsyncTickBehavior.getInstance().dispatch(group::tickParticles);
			} else {
				group.tickParticles();
			}
			Profiler.get().pop();
		}
		if (!this.trackingEmitters.isEmpty()) {
			for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
				trackingEmitter.tick();
			}
		}
		if (!tickAsync) {
			this.hwopt$removeDeadParticles();
			trackingEmitters.removeIf(te -> !te.isAlive());
		}
	}

	private void hwopt$removeDeadParticles() {
		for (ParticleGroup<?> g : particles.values()) {
			((ParticleGroupAddition) g).asyncparticles$removeDeadParticles();
		}
	}

	@Override
	public void asyncparticle$tickSyncParticles() {
		for (ParticleGroup<?> g : particles.values()) {
			((ParticleGroupAddition) g).asyncparticles$tickSyncParticles();
		}
		AsyncTickBehavior.getInstance().submitCleanup(this::hwopt$removeDeadParticles);
		AsyncTickBehavior.getInstance().submitCleanup(() -> trackingEmitters.removeIf(te -> !te.isAlive()));
		AsyncTickBehavior.getInstance().waitCleanup();
	}
}
