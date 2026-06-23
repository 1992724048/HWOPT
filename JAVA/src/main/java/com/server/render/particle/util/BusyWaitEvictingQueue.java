package com.server.render.particle.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import com.server.render.particle.util.IterationSafeEvictingQueue;
import com.server.render.particle.util.ReentrantSpinLock;
import com.server.render.particle.util.SpinLock;

public class BusyWaitEvictingQueue<E> extends IterationSafeEvictingQueue<E> {
	private final ReentrantSpinLock lock = new ReentrantSpinLock();

	public BusyWaitEvictingQueue(int maxCapacity) {
		super(maxCapacity);
	}

	@Override
	public boolean offer(E e) {
		lock.lock();
		try {
			return super.offer(e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E poll() {
		lock.lock();
		try {
			return super.poll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E peek() {
		lock.lock();
		try {
			return super.peek();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean remove(Object o) {
		lock.lock();
		try {
			return super.remove(o);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void clear() {
		lock.lock();
		try {
			super.clear();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int size() {
		lock.lock();
		try {
			return super.size();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isEmpty() {
		lock.lock();
		try {
			return super.isEmpty();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Iterator<E> iterator() {
		lock.lock();
		try {
			return super.iterator();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		lock.lock();
		try {
			return super.removeIf(filter);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		lock.lock();
		try {
			return super.retainAll(c);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		lock.lock();
		try {
			return super.removeAll(c);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		lock.lock();
		try {
			super.forEach(action);
		} finally {
			lock.unlock();
		}
	}
}
