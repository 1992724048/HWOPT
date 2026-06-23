package com.server.render.particle.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import com.server.render.particle.core.AsyncParticleWorkerThread;

public class ParticleThreadLocal<T> {
	private static final AtomicInteger INDEX_COUNTER = new AtomicInteger();
	private final int index;
	private T mainValue;
	private final Supplier<T> initialValue;

	public ParticleThreadLocal(Supplier<T> initialValue) {
		this.index = INDEX_COUNTER.getAndIncrement();
		this.initialValue = initialValue;
		this.mainValue = initialValue.get();
	}

	@SuppressWarnings("unchecked")
	public T get() {
		Thread t = Thread.currentThread();
		if (t instanceof AsyncParticleWorkerThread awt) {
			return (T) awt.getThreadLocalValue(index);
		}
		return mainValue;
	}

	public void set(T value) {
		Thread t = Thread.currentThread();
		if (t instanceof AsyncParticleWorkerThread awt) {
			awt.setThreadLocalValue(index, value);
		} else {
			mainValue = value;
		}
	}

	public static int nextIndex() {
		return INDEX_COUNTER.getAndIncrement();
	}
}
