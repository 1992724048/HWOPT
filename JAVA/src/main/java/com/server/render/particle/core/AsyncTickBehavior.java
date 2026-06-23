package com.server.render.particle.core;

import com.hwpp.mod.Config;
import com.server.render.particle.addon.ParticleAddon;
import com.server.render.particle.config.ConfigHelper;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncTickBehavior {
	static final Logger LOGGER = LoggerFactory.getLogger("AsyncParticles");
	public static final int THREADS = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 6);
	public static final String THREAD_PREFIX = "AsyncParticleTickWorker";
	private static final AsyncTickBehavior INSTANCE = new AsyncTickBehavior();

	private final ForkJoinPool EXECUTOR;

	{
		AtomicInteger workerCount = new AtomicInteger(1);
		EXECUTOR = new ForkJoinPool(THREADS, forkJoinPool -> {
			ForkJoinWorkerThread forkJoinWorkerThread = new AsyncTickerThread(forkJoinPool);
			forkJoinWorkerThread.setName(THREAD_PREFIX + "-" + workerCount.getAndIncrement());
			forkJoinWorkerThread.setDaemon(true);
			return forkJoinWorkerThread;
		}, (thread, throwable) -> {
			LOGGER.error("Uncaught exception in thread {}", thread, throwable);
		}, true);
	}

	private final TaskHelper tickTaskHelper = new TaskHelper(EXECUTOR, e -> {
		throw new RuntimeException(e);
	});
	private final TaskHelper cleanupTaskHelper = new TaskHelper(EXECUTOR, e -> {
		throw new RuntimeException(e);
	});
	private boolean reloadLater;
	private boolean particlePhase;
	private boolean isTailTick;
	private final Set<Class<?>> syncParticleTypes = new ReferenceOpenHashSet<>();
	private final Map<Class<?>, long[]> exceptionCounts = new ConcurrentHashMap<>();

	public static AsyncTickBehavior getInstance() {
		return INSTANCE;
	}

	public boolean isEnabled() {
		return Config.CONFIG.asyncParticleTick.get();
	}

	public void preTick() {
		preTick(true, true);
	}

	public void preTick(boolean isHeadTick, boolean isTailTick) {
		if (isHeadTick) {
			tickTaskHelper.waitForCompletion();
		}
		this.isTailTick = isTailTick;
		if (!isEnabled()) return;
		if (cleanupTaskHelper.isRunning()) {
			throw new IllegalStateException("cleanup tasks are still running!");
		}
	}

	public void submitCleanup(Runnable task) {
		if (!isEnabled()) return;
		cleanupTaskHelper.submitImmediately(task);
	}

	public void waitCleanup() {
		cleanupTaskHelper.waitForCompletion();
	}

	public void postTick() {
		cleanupTaskHelper.waitForCompletion();
		Minecraft mc = Minecraft.getInstance();
		boolean levelRunning = mc.level != null && mc.player != null && !mc.isPaused();
		if (!isEnabled()) {
			tryReload();
			if (levelRunning) {
				tickTaskHelper.runAllTasks();
			}
			return;
		}
		if (!levelRunning) return;
		if (!isTailTick) {
			tickTaskHelper.disposeTasks();
			// In reference: also evicts particlesToAdd
		} else {
			particlePhase = true;
			mc.particleEngine.tick();
			particlePhase = false;
			tryReload();
			tickTaskHelper.submitAllSequentially();
		}
	}

	public boolean shouldTickParticleEngine() {
		if (particlePhase || !isEnabled()) {
			return true;
		}
		throw new IllegalStateException("ParticleEngine.tick() called outside particle phase.");
	}

	public void dispatch(Runnable task) {
		tickTaskHelper.addTask(task);
	}

	public boolean shouldRemove(Particle particle) {
		if (!particle.isAlive()) return true;
		ParticleAddon addon = (ParticleAddon) particle;
		if (addon.asyncparticles$isTickSync()) return false;
		if (isEnabled() && addon.asyncparticles$isTicked()) {
			addon.asyncparticles$resetTicked();
			return false;
		}
		return ConfigHelper.isRemoveIfMissedTick();
	}

	public <T extends Particle> void onEvict(T particle) {
		if (particle.isAlive()) {
			particle.remove();
		}
	}

	public boolean isTolerable(Throwable e) {
		if (!(e instanceof Exception)) {
			return false;
		}
		Throwable rootCause = e;
		while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
			rootCause = rootCause.getCause();
		}
		return rootCause instanceof NullPointerException
			|| rootCause instanceof IndexOutOfBoundsException;
	}

	public ReportedException onTickParticleException(Particle particle, Throwable t) {
		return constructCrashReport(particle, t);
	}

	public ReportedException constructCrashReport(Particle particle, Throwable t) {
		while (t instanceof CompletionException || t instanceof ExecutionException) {
			t = t.getCause();
		}
		if (t instanceof ReportedException re) {
			return re;
		}
		CrashReport crashReport = CrashReport.forThrowable(t, "Ticking Particle");
		CrashReportCategory category = crashReport.addCategory("Particle being ticked");
		category.setDetail("Particle", particle::toString);
		category.setDetail("Particle Type", () -> String.valueOf(particle.getGroup()));
		return new ReportedException(crashReport);
	}

	public void reloadLater() {
		reloadLater = true;
	}

	private void tryReload() {
		if (reloadLater) {
			Minecraft.getInstance().particleEngine.clearParticles();
			reloadLater = false;
		}
	}

	public void reset() {
		tickTaskHelper.waitForCompletion();
		tickTaskHelper.disposeTasks();
		cleanupTaskHelper.waitForCompletion();
		cleanupTaskHelper.disposeTasks();
		syncParticleTypes.clear();
	}

	public ForkJoinPool getExecutor() {
		return tickTaskHelper.executor();
	}
}
