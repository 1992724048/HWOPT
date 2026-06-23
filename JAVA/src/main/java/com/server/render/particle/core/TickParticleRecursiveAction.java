package com.server.render.particle.core;

import com.server.render.particle.addon.LightCachedParticleAddon;
import com.server.render.particle.addon.ParticleAddon;
import com.server.render.particle.config.ConfigHelper;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.ReportedException;
import net.minecraft.client.particle.Particle;

import java.util.Spliterator;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.function.Consumer;

public class TickParticleRecursiveAction<T extends Particle> extends RecursiveAction {
	private static final int MAX_DEPTH = (int) Math.round(Math.log(HashCommon.nextPowerOfTwo(AsyncTickBehavior.THREADS)) / Math.log(2)) + 2;
	private static final boolean ENABLE_LIGHT_CACHE = ConfigHelper.particleLightCache();
	private final Spliterator<T> spliterator;
	private final int depth;
	private final Consumer<Particle> ticker;

	public TickParticleRecursiveAction(Spliterator<T> spliterator, Consumer<Particle> ticker) {
		this(spliterator, 0, ticker);
	}

	private TickParticleRecursiveAction(Spliterator<T> spliterator, int depth, Consumer<Particle> ticker) {
		this.spliterator = spliterator;
		this.depth = depth;
		this.ticker = ticker;
	}

	@Override
	public void compute() {
		Spliterator<T> sub;
		if (spliterator.estimateSize() > 192 && depth < MAX_DEPTH && (sub = spliterator.trySplit()) != null) {
			ForkJoinTask<Void> left = new TickParticleRecursiveAction<>(sub, depth + 1, ticker).fork();
			ForkJoinTask<Void> right = new TickParticleRecursiveAction<>(spliterator, depth + 1, ticker).fork();
			left.join();
			right.join();
		} else {
			spliterator.forEachRemaining(this::process);
		}
	}

	private void process(Particle particle) {
		if (!particle.isAlive()) {
			return;
		}
		ParticleAddon particleAddon = (ParticleAddon) particle;
		boolean shouldTick;
		boolean shouldRefresh;
		if (particleAddon.asyncparticles$isTicked()) {
			shouldTick = false;
			shouldRefresh = ENABLE_LIGHT_CACHE;
		} else if (particleAddon.asyncparticles$isTickSync()) {
			return;
		} else {
			shouldTick = true;
			shouldRefresh = ENABLE_LIGHT_CACHE;
		}
		if (shouldTick) {
			try {
				ticker.accept(particle);
			} catch (Throwable t) {
				ReportedException re = AsyncTickBehavior.getInstance().onTickParticleException(particle, t);
				if (re != null) {
					throw re;
				}
				particleAddon.asyncparticles$setTickSync();
				shouldRefresh = false;
			}
			particleAddon.asyncparticles$setTicked();
		}
		if (ENABLE_LIGHT_CACHE) {
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
