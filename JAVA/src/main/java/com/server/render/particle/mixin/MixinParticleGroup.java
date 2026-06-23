package com.server.render.particle.mixin;

import com.server.render.particle.addon.AsyncTickableParticleGroup;
import com.server.render.particle.addon.LightCachedParticleAddon;
import com.server.render.particle.addon.ParticleAddon;
import com.server.render.particle.addon.ParticleGroupAddition;
import com.server.render.particle.config.ConfigHelper;
import com.server.render.particle.core.AsyncTickBehavior;
import com.server.render.particle.core.AsyncTickParticleGroupBehavior;
import com.server.render.particle.core.TickParticleRecursiveAction;
import com.server.render.particle.util.IterationSafeEvictingQueue;
import com.server.render.particle.util.ThreadUtil;
import com.server.render.particle.util.Utils;
import net.minecraft.ReportedException;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.TrackingEmitter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

@Mixin(ParticleGroup.class)
public abstract class MixinParticleGroup implements ParticleGroupAddition {
	@Mutable
	@Shadow
	@Final
	protected Queue<? extends Particle> particles;
	@Unique
	private int asyncparticles$particleLimit;
	@Unique
	private boolean asyncparticles$canRemoveInParallel;

	@Shadow
	protected abstract void tickParticle(Particle particle);
	@Unique
	private final java.util.function.Consumer<Particle> hwopt$ticker = this::tickParticle;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void hwopt$replaceQueue(CallbackInfo ci) {
		int initSize = Math.max(16, Math.min(particles.size() + 64, ConfigHelper.getParticleLimit()));
		IterationSafeEvictingQueue<Particle> replaced = IterationSafeEvictingQueue.newInstance(
			initSize, ConfigHelper.getParticleLimit(),
			AsyncTickBehavior.getInstance()::onEvict);
		replaced.addAll(particles);
		this.particles = replaced;
	}

	@Overwrite
	public void tickParticles() {
		this.asyncparticles$canRemoveInParallel = true;
		if (particles.isEmpty()) {
			return;
		}
		if (ThreadUtil.isOnParticleTickerThread() && ConfigHelper.isSplitParticleTick()
			&& AsyncTickParticleGroupBehavior.canTickAsync((ParticleGroup<?>) (Object) this)) {
			new TickParticleRecursiveAction<>(particles.spliterator(), hwopt$ticker)
				.compute();
			return;
		}
		boolean enableLightCache = ConfigHelper.particleLightCache();
		boolean isOnMainThread = ThreadUtil.isOnMainThread();
		for (Particle particle : particles) {
			if (!particle.isAlive()) {
				Utils.DUMMY_ITERATOR.remove();
				continue;
			}
			ParticleAddon particleAddon = (ParticleAddon) particle;
			boolean shouldTick;
			boolean shouldRefresh;
			if (isOnMainThread) {
				shouldTick = true;
				shouldRefresh = false;
			} else if (particleAddon.asyncparticles$isTicked()) {
				shouldTick = false;
				shouldRefresh = enableLightCache;
			} else if (particleAddon.asyncparticles$isTickSync()) {
				((AsyncTickableParticleGroup) this).asyncparticles$recordSync(particle);
				continue;
			} else {
				shouldTick = true;
				shouldRefresh = enableLightCache;
			}
			if (shouldTick) {
				try {
					tickParticle(particle);
				} catch (Throwable t) {
					ReportedException re = AsyncTickBehavior.getInstance().onTickParticleException(particle, t);
					if (re != null) {
						throw re;
					}
				}
				if (!isOnMainThread) {
					particleAddon.asyncparticles$setTicked();
				}
			}
			if (enableLightCache) {
				LightCachedParticleAddon light = (LightCachedParticleAddon) particle;
				if (shouldRefresh) {
					light.asyncparticles$refresh();
					light.asyncparticles$enableLightCache();
				} else {
					light.asyncparticles$disableLightCache();
				}
			}
		}
	}

	@Override
	public void asyncparticles$removeDeadParticles() {
		if (!asyncparticles$canRemoveInParallel) {
			return;
		}
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends Particle>) particles)
				.parallelRemoveIf(particle ->
						AsyncTickBehavior.getInstance().shouldRemove(particle),
					ConfigHelper.isParallelQueueEviction(),
					AsyncTickBehavior.THREADS,
					AsyncTickBehavior.getInstance().getExecutor());
		} else {
			particles.removeIf(particle ->
				AsyncTickBehavior.getInstance().shouldRemove(particle));
		}
	}

	@Override
	public void asyncparticles$clear() {
		particles.forEach(AsyncTickBehavior.getInstance()::onEvict);
		particles.clear();
	}

	@Override
	public void asyncparticles$tickSyncParticles() {
		if (!ConfigHelper.isAsyncTickParticle()
			|| !(this instanceof AsyncTickableParticleGroup asyncGroup)) {
			return;
		}
		Set<Particle> syncParticles = asyncGroup.asyncparticles$getSyncParticles();
		if (syncParticles.isEmpty()) {
			return;
		}
		boolean enableLightCache = ConfigHelper.particleLightCache();
		for (Iterator<Particle> iterator = syncParticles.iterator(); iterator.hasNext(); ) {
			Particle particle = iterator.next();
			try {
				tickParticle(particle);
				if (!(particle instanceof TrackingEmitter)) {
					if (enableLightCache) {
						((LightCachedParticleAddon) particle).asyncparticles$refresh();
					}
					((ParticleAddon) particle).asyncparticles$setTicked();
				}
			} catch (Throwable e) {
				throw AsyncTickBehavior.getInstance().constructCrashReport(particle, e);
			}
			if (!particle.isAlive()) {
				iterator.remove();
			}
		}
	}
}
