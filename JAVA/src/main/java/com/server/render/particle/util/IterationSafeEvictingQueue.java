package com.server.render.particle.util;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

public class IterationSafeEvictingQueue<E> extends AbstractQueue<E> {
	private final Object[] elements;
	private final int maxCapacity;
	private int head;
	private int tail;
	private int size;
	protected Consumer<E> onEvict;

	public IterationSafeEvictingQueue(int maxCapacity) {
		this.maxCapacity = maxCapacity;
		this.elements = new Object[maxCapacity];
	}

	public void setOnEvict(Consumer<E> onEvict) {
		this.onEvict = onEvict;
	}

	@Override
	public boolean offer(E e) {
		Objects.requireNonNull(e);
		if (size == maxCapacity) {
			E evicted = poll();
			if (onEvict != null) onEvict.accept(evicted);
		}
		elements[tail] = e;
		tail = (tail + 1) % maxCapacity;
		size++;
		return true;
	}

	@Override
	public E poll() {
		if (size == 0) return null;
		@SuppressWarnings("unchecked")
		E e = (E) elements[head];
		elements[head] = null;
		head = (head + 1) % maxCapacity;
		size--;
		return e;
	}

	@Override
	public E peek() {
		if (size == 0) return null;
		@SuppressWarnings("unchecked")
		E e = (E) elements[head];
		return e;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<>() {
			private int idx = head;
			private int remaining = size;

			@Override
			public boolean hasNext() {
				return remaining > 0;
			}

			@Override
			public E next() {
				if (remaining <= 0) throw new NoSuchElementException();
				@SuppressWarnings("unchecked")
				E e = (E) elements[idx];
				idx = (idx + 1) % maxCapacity;
				remaining--;
				return e;
			}
		};
	}

	@Override
	public void clear() {
		for (int i = 0; i < size; i++) {
			elements[(head + i) % maxCapacity] = null;
		}
		head = tail = size = 0;
	}
}
