package com.server.render.particle.core;

import com.hwpp.mod.Config;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleEngine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import com.server.render.particle.core.AsyncParticleWorkerThread;

public class AsyncTickBehavior {
	private static final AsyncTickBehavior INSTANCE = new AsyncTickBehavior();
	private ForkJoinPool pool;
	private final List<Runnable> pendingTasks = new ArrayList<>();
	private final List<ForkJoinTask<?>> submittedTasks = new ArrayList<>();
	private boolean particlePhase;
	private AsyncTickBehavior() {}

	public static AsyncTickBehavior getInstance() {
		return INSTANCE;
	}

	public void setEnabled(boolean enabled) {
		Config.get().asyncParticleTick = enabled;
		if (enabled && pool == null) {
			int threads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() - 1, 6));
			pool = new ForkJoinPool(threads, AsyncParticleWorkerThread::new, null, false);
		} else if (!enabled && pool != null) {
			pool.shutdown();
			pool = null;
		}
	}

	public boolean isEnabled() {
		return Config.get().asyncParticleTick && pool != null;
	}

	public void preTick() {
		if (!isEnabled()) return;
		waitForCompletion();
	}

	public void postTick() {
		if (!isEnabled()) return;
		submitAll();
	}

	public void dispatch(ParticleGroup group, Runnable task) {
		if (!isEnabled()) {
			task.run();
			return;
		}
		pendingTasks.add(task);
	}

	private void submitAll() {
		if (pendingTasks.isEmpty()) return;
		for (Runnable task : pendingTasks) {
			submittedTasks.add(pool.submit(task));
		}
		pendingTasks.clear();
	}

	private void waitForCompletion() {
		if (submittedTasks.isEmpty()) return;
		for (ForkJoinTask<?> task : submittedTasks) {
			task.join();
		}
		submittedTasks.clear();
	}

	public void shutdown() {
		if (pool != null) {
			pool.shutdown();
			try {
				pool.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			pool = null;
		}
	}
}
