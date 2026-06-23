package com.server.render.particle.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class TaskHelper {
	private final ForkJoinPool executor;
	private final List<Runnable> tasks = new ArrayList<>();
	private final List<ForkJoinTask<?>> futures = new ArrayList<>();
	private final Consumer<Exception> exceptionHandler;

	public TaskHelper(ForkJoinPool executor, Consumer<Exception> exceptionHandler) {
		this.executor = executor;
		this.exceptionHandler = exceptionHandler;
	}

	public void addTask(Runnable task) {
		tasks.add(task);
	}

	public void submitImmediately(Runnable task) {
		futures.add(executor.submit(task));
	}

	public void submitAllSequentially() {
		if (tasks.isEmpty()) {
			return;
		}
		Runnable[] tasksArray = tasks.toArray(new Runnable[tasks.size()]);
		tasks.clear();
		futures.add(executor.submit(() -> {
			for (Runnable runnable : tasksArray) {
				runnable.run();
			}
		}));
	}

	public void submitAll() {
		if (tasks.isEmpty()) {
			return;
		}
		for (Runnable runnable : tasks) {
			futures.add(executor.submit(runnable));
		}
		tasks.clear();
	}

	public void waitForCompletion() {
		waitForCompletion(exceptionHandler);
	}

	public void waitForCompletion(Consumer<Exception> exceptionHandler) {
		if (futures.isEmpty()) {
			return;
		}
		for (ForkJoinTask<?> task : futures) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				exceptionHandler.accept(e);
			}
		}
		futures.clear();
	}

	public ForkJoinPool executor() {
		return executor;
	}

	public boolean isRunning() {
		return !futures.isEmpty();
	}

	public void runAllTasks() {
		if (tasks.isEmpty()) {
			return;
		}
		for (Runnable task : tasks) {
			task.run();
		}
		tasks.clear();
	}

	public void disposeTasks() {
		tasks.clear();
	}

	public int taskCount() {
		return tasks.size();
	}
}
