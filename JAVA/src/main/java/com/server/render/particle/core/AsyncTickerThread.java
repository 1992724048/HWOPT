package com.server.render.particle.core;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.ForkJoinPool;

public class AsyncTickerThread extends AsyncParticleWorkerThread {
	private static final Logger LOGGER = LogUtils.getLogger();

	public AsyncTickerThread(ForkJoinPool forkJoinPool) {
		super(forkJoinPool);
	}

	protected void onTermination(Throwable throwable) {
		if (throwable != null) {
			LOGGER.warn("{} died", this.getName(), throwable);
		} else {
			LOGGER.debug("{} shutdown", this.getName());
		}
		super.onTermination(throwable);
	}
}
