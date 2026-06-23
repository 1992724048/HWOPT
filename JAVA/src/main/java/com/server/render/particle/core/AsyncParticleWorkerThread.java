package com.server.render.particle.core;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public class AsyncParticleWorkerThread extends ForkJoinWorkerThread {
	private Object[] threadLocalStorage = new Object[8];

	public AsyncParticleWorkerThread(ForkJoinPool pool) {
		super(pool);
		setName("AsyncParticle-" + getName());
	}

	public Object getThreadLocalValue(int index) {
		if (index < threadLocalStorage.length) {
			return threadLocalStorage[index];
		}
		return null;
	}

	public void setThreadLocalValue(int index, Object value) {
		if (index >= threadLocalStorage.length) {
			int newLen = Integer.highestOneBit(index) << 1;
			Object[] newStorage = new Object[Math.max(newLen, 8)];
			System.arraycopy(threadLocalStorage, 0, newStorage, 0, threadLocalStorage.length);
			threadLocalStorage = newStorage;
		}
		threadLocalStorage[index] = value;
	}
}
