package com.server.render.particle.core;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncParticleWorkerThread extends ForkJoinWorkerThread {
	private static final AtomicInteger indexGenerator = new AtomicInteger(0);
	private Object[] data;

	protected AsyncParticleWorkerThread(ForkJoinPool pool) {
		super(pool);
		int initialSize = Math.max(Integer.highestOneBit(indexGenerator.get()) << 1, 4);
		this.data = new Object[initialSize];
	}

	public static int nextThreadLocalIndex() {
		return indexGenerator.getAndIncrement();
	}

	public Object getThreadLocalValue(int index) {
		ensureIndex(index);
		return data[index];
	}

	private void ensureIndex(int index) {
		int length = data.length;
		if (index >= length) {
			int newLength = Integer.highestOneBit(index) << 1;
			Object[] newArray = new Object[Math.max(newLength, length)];
			System.arraycopy(data, 0, newArray, 0, length);
			data = newArray;
		}
	}

	public void setThreadLocalValue(int index, Object value) {
		ensureIndex(index);
		data[index] = value;
	}
}
