package com.server.render.particle.util;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BusyWaitEvictingQueue<E> extends IterationSafeEvictingQueue<E> {
	private final ReentrantSpinLock lock = new ReentrantSpinLock();

	public BusyWaitEvictingQueue(int initialCapacity, int maxCapacity) {
		super(initialCapacity, maxCapacity);
	}

	public BusyWaitEvictingQueue(int initialCapacity, int maxCapacity, Consumer<E> onEvict) {
		super(initialCapacity, maxCapacity, onEvict);
	}

	public static <E> BusyWaitEvictingQueue<E> newInstance(int initialCapacity, int maxCapacity) {
		return new BusyWaitEvictingQueue<>(Math.min(initialCapacity, maxCapacity), maxCapacity);
	}

	public static <E> BusyWaitEvictingQueue<E> newInstance(int initialCapacity, int maxCapacity, Consumer<E> onEvict) {
		return new BusyWaitEvictingQueue<>(Math.min(initialCapacity, maxCapacity), maxCapacity, onEvict);
	}

	public boolean add0(E e) {
		lock.lock();
		try { return super.add(e); } finally { lock.unlock(); }
	}

	public boolean offer0(E e) {
		lock.lock();
		try { return super.offer(e); } finally { lock.unlock(); }
	}

	public E poll0() {
		lock.lock();
		try { return super.poll(); } finally { lock.unlock(); }
	}

	public boolean remove0(Object o) {
		lock.lock();
		try { return super.remove(o); } finally { lock.unlock(); }
	}

	public void clear0() {
		lock.lock();
		try { super.clear(); } finally { lock.unlock(); }
	}

	@Override
	public boolean add(E e) {
		return lock.wrap(() -> super.add(e));
	}

	@Override
	public boolean remove(Object o) {
		return lock.wrap(() -> super.remove(o));
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return lock.wrap(() -> super.addAll(c));
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return lock.wrap(() -> super.removeAll(c));
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		return lock.wrap(() -> super.removeIf(filter));
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return lock.wrap(() -> super.retainAll(c));
	}

	@Override
	public void clear() {
		lock.wrap(super::clear);
	}

	@Override
	public boolean offer(E e) {
		return lock.wrap(() -> super.offer(e));
	}

	@Override
	public E remove() {
		return lock.wrap(() -> super.remove());
	}

	@Override
	public E poll() {
		return lock.wrap(super::poll);
	}
}
