package com.server.render.particle.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import com.server.render.particle.util.SpinLock;

public class ReentrantSpinLock implements SpinLock {
	private Thread owner;
	private int holdCount;

	private static final VarHandle OWNER;

	static {
		try {
			OWNER = MethodHandles.lookup().findVarHandle(ReentrantSpinLock.class, "owner", Thread.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Override
	public void lock() {
		Thread current = Thread.currentThread();
		if (current == owner) {
			holdCount++;
			return;
		}
		for (int spins = 0; ; spins++) {
			if (OWNER.compareAndSet(this, null, current)) {
				holdCount = 1;
				return;
			}
			if (spins > 50) {
				Thread.yield();
			} else {
				Thread.onSpinWait();
			}
		}
	}

	@Override
	public void unlock() {
		Thread current = Thread.currentThread();
		if (current != owner) return;
		int count = --holdCount;
		if (count == 0) {
			owner = null;
		}
	}
}
