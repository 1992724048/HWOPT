package com.server.render.particle.config;

import com.hwpp.mod.Config;

public class ConfigHelper {
	public static boolean particleLightCache() {
		return Config.CONFIG.particleLightCache.get();
	}

	public static boolean isAsyncTickParticle() {
		return Config.CONFIG.asyncParticleTick.get();
	}

	public static boolean isSplitParticleTick() {
		return Config.CONFIG.asyncParticleTick.get();
	}

	public static boolean isRemoveIfMissedTick() {
		return Config.CONFIG.removeIfMissedTick.get();
	}

	public static boolean isParallelQueueRemoval() {
		return Config.CONFIG.parallelQueueRemoval.get();
	}

	public static boolean isParallelQueueEviction() {
		return Config.CONFIG.parallelQueueEviction.get();
	}

	public static int getParticleLimit() {
		return Config.CONFIG.particleLimit.get();
	}
}
