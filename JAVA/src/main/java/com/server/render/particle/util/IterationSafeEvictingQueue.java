package com.server.render.particle.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class IterationSafeEvictingQueue<E> implements Queue<E> {
	public static final int MAX_CAPACITY = Integer.MIN_VALUE >>> 1;
	protected Object[] queue;
	protected final int maxCapacity;
	protected final int maxCapacityPowerOfTwo;
	protected final Consumer<E> onEvict;
	protected int head;
	protected int size;

	public IterationSafeEvictingQueue(int initialCapacity, int maxCapacity) {
		this(initialCapacity, maxCapacity, e -> {
		});
	}

	public IterationSafeEvictingQueue(int initialCapacity, int maxCapacity, Consumer<E> onEvict) {
		if (initialCapacity < 0 || maxCapacity <= 0 || initialCapacity > maxCapacity) {
			throw new IllegalArgumentException("Invalid capacities, initialCapacity: " + initialCapacity + ", maxCapacity: " + maxCapacity);
		}
		this.queue = new Object[roundUpToPowerOfTwo(Math.max(8, initialCapacity))];
		this.maxCapacity = maxCapacity;
		this.maxCapacityPowerOfTwo = roundUpToPowerOfTwo(maxCapacity);
		this.onEvict = onEvict;
		this.head = 0;
		this.size = 0;
	}

	public static <E> IterationSafeEvictingQueue<E> newInstance(int initialCapacity, int maxCapacity) {
		return new IterationSafeEvictingQueue<>(Math.min(initialCapacity, maxCapacity), maxCapacity);
	}

	public static <E> IterationSafeEvictingQueue<E> newInstance(int initialCapacity, int maxCapacity, Consumer<E> onEvict) {
		return new IterationSafeEvictingQueue<>(Math.min(initialCapacity, maxCapacity), maxCapacity, onEvict);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean offer(E item) {
		if (item == null) {
			throw new NullPointerException("Item cannot be null");
		}
		Object[] q = queue;
		int capacity = q.length;
		int size = this.size;
		if (size >= maxCapacity) {
			int head = this.head;
			int mask = capacity - 1;
			this.head = (head + 1) & mask;
			E evicted = (E) q[head];
			if (evicted != null) {
				onEvict.accept(evicted);
			}
			q[head] = null;
			q[(head + size) & mask] = item;
		} else {
			if (capacity == size) {
				q = resize(capacity << 1);
			}
			q[(head + size) & (capacity - 1)] = item;
			this.size++;
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E poll() {
		if (size == 0) {
			return null;
		}
		Object[] q = queue;
		int head = this.head;
		E item = (E) q[head];
		this.head = (head + 1) & (q.length - 1);
		q[head] = null;
		size--;
		return item;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public E peek() {
		if (size == 0) {
			return null;
		}
		Object o;
		while (true) {
			Object[] queue = this.queue;
			int head = this.head;
			if (head >= queue.length) {
				Thread.yield();
				continue;
			}
			if ((o = queue[head]) != null) {
				return (E) o;
			}
			if (size == 0) {
				return null;
			}
		}
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public Spliterator<E> spliterator() {
		return new aSpliterator();
	}

	public Iterator<E> conditionalIterator(Predicate<E> predicate) {
		return new anIterator() {
			@Override
			public boolean hasNext() {
				if (next != null) {
					return true;
				}
				final Object e = curr;
				while (cursor < tail) {
					next = a[cursor++ & mask];
					if (next != null && next != e && predicate.test((E) next)) {
						return true;
					}
				}
				return false;
			}
		};
	}

	private final class aSpliterator implements Spliterator<E> {
		int pos;
		int max;

		private aSpliterator(int pos, int max) {
			assert pos <= max : "pos " + pos + " must be <= max " + max;
			this.pos = pos;
			this.max = max;
		}

		private aSpliterator() {
			Object[] a = IterationSafeEvictingQueue.this.queue;
			this.pos = Math.min(a.length - 1, IterationSafeEvictingQueue.this.head);
			this.max = Math.min(a.length, IterationSafeEvictingQueue.this.size) + this.pos;
		}

		@Override
		public int characteristics() {
			return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
		}

		@Override
		public long estimateSize() {
			return max - pos;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean tryAdvance(final Consumer<? super E> action) {
			final Object[] a = queue;
			final int mask = a.length - 1;
			E e = (E) a[pos++ & mask];
			if (e != null) {
				action.accept(e);
			}
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void forEachRemaining(final Consumer<? super E> action) {
			final Object[] a = queue;
			int pos = this.pos;
			final int max = this.max;
			final int mask = a.length - 1;
			this.pos = max;
			for (; pos < max; ++pos) {
				E e = (E) a[pos & mask];
				if (e != null) {
					action.accept(e);
				}
			}
		}

		@Override
		public Spliterator<E> trySplit() {
			final int max = this.max;
			final int pos = this.pos;
			int retLen = (max - pos) >> 1;
			if (retLen <= 1) {
				return null;
			}
			int newPos = pos + retLen;
			this.pos = newPos;
			return new aSpliterator(pos, newPos);
		}
	}

	private Object[] resize(int newCapacity) {
		if (newCapacity > this.maxCapacityPowerOfTwo) {
			throw new IllegalStateException("Cannot increase capacity beyond max capacity " + maxCapacityPowerOfTwo + " : " + newCapacity);
		}
		Object[] q = this.queue;
		int head = this.head;
		int tail = head + this.size;
		int capacity = q.length;
		Object[] a = new Object[newCapacity];
		if (tail <= capacity) {
			System.arraycopy(q, head, a, head, this.size);
		} else {
			int l = capacity - head;
			System.arraycopy(q, head, a, 0, l);
			System.arraycopy(q, 0, a, l, tail - capacity);
		}
		this.queue = a;
		this.head = 0;
		return a;
	}

	public int arraySize() {
		return queue.length;
	}

	@Override
	public Iterator<E> iterator() {
		return new anIterator();
	}

	private class anIterator implements Iterator<E> {
		protected final Object[] a = queue;
		protected final int mask = a.length - 1;
		protected final int head = Math.min(mask, IterationSafeEvictingQueue.this.head);
		protected int tail = Math.min(a.length, IterationSafeEvictingQueue.this.size) + head;
		protected int cursor = head;
		protected Object curr;
		protected Object next;

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			final Object e = curr;
			while (cursor < tail) {
				next = a[cursor++ & mask];
				if (next != null && next != e) {
					return true;
				}
			}
			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		public E next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			Object next = this.next;
			this.next = null;
			return (E) (curr = next);
		}

		@Override
		public void remove() {
			if (curr == null) {
				throw new IllegalStateException();
			}
			int i = cursor - 1;
			while (i >= head && a[i & mask] != curr) {
				--i;
			}
			if (i < 0) {
				throw new IllegalStateException();
			}
			IterationSafeEvictingQueue.this.removeIndex(a, i, tail);
			tail--;
			curr = null;
		}
	}

	@Override
	public boolean add(E e) {
		return offer(e);
	}

	@Override
	public E remove() {
		E item = poll();
		if (item == null) {
			throw new NoSuchElementException("Queue is empty");
		}
		return item;
	}

	@Override
	public E element() {
		E item = peek();
		if (item == null) {
			throw new NoSuchElementException("Queue is empty");
		}
		return item;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object e : c) {
			if (!contains(e)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (c.isEmpty()) {
			return false;
		}
		for (E e : c) {
			add(e);
		}
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return removeIf(c::contains);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return removeIf(e -> !c.contains(e));
	}

	public void parallelRemoveIf(Predicate<? super E> filter, boolean parallelEvicting, int threads, ExecutorService executor) {
		if (parallelEvicting) {
			parallelRemoveIfParallelEvicting(filter, threads, executor);
		} else {
			parallelRemoveIfSequencedEvicting(filter, threads, executor);
		}
	}

	@SuppressWarnings("unchecked")
	private void parallelRemoveIfSequencedEvicting(Predicate<? super E> filter, int threads, ExecutorService executor) {
		final Object[] a = this.queue;
		final int mask = a.length - 1;
		final int size = this.size;
		final int head = this.head;
		int chunkSize = ((size + threads - 1) / threads);

		Queue<E> queue = new ConcurrentLinkedQueue<>();
		List<Future<?>> futures = new ArrayList<>(threads);
		for (int i = 0; i < threads; i++) {
			int finalI = i;
			futures.add(executor.submit(() -> {
				int start = finalI * chunkSize;
				int end = Math.min(start + chunkSize, size);
				for (int j = start; j < end; j++) {
					int index = (head + j) & mask;
					E e = (E) a[index];
					if (e == null) continue;
					if (filter.test(e)) {
						a[index] = null;
						queue.add(e);
					}
				}
			}));
		}

		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		for (E e : queue) {
			onEvict.accept(e);
		}

		int newSize = 0;
		for (int i = 0; i < size; i++) {
			int pos = (head + i) & mask;
			E e = (E) a[pos];
			if (e != null) {
				a[(head + newSize++) & mask] = e;
			}
		}

		for (int i = newSize; i < size; i++) {
			int pos = (head + i) & mask;
			a[pos] = null;
		}

		this.size = newSize;
	}

	@SuppressWarnings("unchecked")
	private void parallelRemoveIfParallelEvicting(Predicate<? super E> filter, int threads, ExecutorService executor) {
		final Object[] a = this.queue;
		final int mask = a.length - 1;
		final int size = this.size;
		final int head = this.head;
		int chunkSize = ((size + threads - 1) / threads);

		List<Future<?>> futures = new ArrayList<>(threads);
		for (int i = 0; i < threads; i++) {
			int finalI = i;
			futures.add(executor.submit(() -> {
				int start = finalI * chunkSize;
				int end = Math.min(start + chunkSize, size);
				for (int j = start; j < end; j++) {
					int index = (head + j) & mask;
					E e = (E) a[index];
					if (e == null) continue;
					if (filter.test(e)) {
						a[index] = null;
						onEvict.accept(e);
					}
				}
			}));
		}

		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		int newSize = 0;
		for (int i = 0; i < size; i++) {
			int pos = (head + i) & mask;
			E e = (E) a[pos];
			if (e != null) {
				a[(head + newSize++) & mask] = e;
			}
		}

		for (int i = newSize; i < size; i++) {
			int pos = (head + i) & mask;
			a[pos] = null;
		}

		this.size = newSize;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean removeIf(Predicate<? super E> filter) {
		final Object[] a = this.queue;
		final int mask = a.length - 1;
		int i = head;
		int to = size + i;
		for (; i < to; i++) {
			E e = (E) a[i & mask];
			if (e == null) continue;
			if (filter.test(e)) {
				onEvict.accept(e);
				break;
			}
		}
		if (i == to) {
			return false;
		}
		a[i & mask] = null;
		int j = i++;
		for (; i < to; i++) {
			int i1 = i & mask;
			E e = (E) a[i1];
			if (e == null) continue;
			if (!filter.test(e)) {
				a[j++ & mask] = e;
			} else {
				onEvict.accept(e);
			}
			a[i1] = null;
		}
		this.size = j - head;
		return true;
	}

	private void removeIndex(Object[] q, int toRemove, int tail) {
		int l = tail - toRemove;
		int mask = q.length - 1;
		if (l > 0) {
			if (tail <= q.length) {
				System.arraycopy(q, toRemove + 1, q, toRemove, l - 1);
			} else if (toRemove <= mask) {
				if (toRemove < mask) {
					System.arraycopy(q, toRemove + 1, q, toRemove, q.length - toRemove - 1);
				}
				q[mask] = q[0];
				System.arraycopy(q, 1, q, 0, tail - q.length - 1);
			} else {
				System.arraycopy(q, (toRemove + 1) & mask, q, toRemove & mask, l - 1);
			}
		}
		q[tail - 1 & mask] = null;
		size--;
	}

	@Override
	public void clear() {
		Object[] q = queue;
		int head = this.head;
		int tail = head + this.size;
		int capacity = q.length;
		if (tail <= capacity) {
			Arrays.fill(q, head, tail, null);
		} else {
			Arrays.fill(q, head, capacity, null);
			Arrays.fill(q, 0, tail - capacity, null);
		}
		this.head = 0;
		this.size = 0;
	}

	@Override
	public boolean contains(Object o) {
		if (o == null) {
			return false;
		}
		Object[] q = queue;
		int mask = q.length - 1;
		for (int i = head, to = i + size; i < to; i++) {
			if (o.equals(q[i & mask])) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object[] toArray() {
		return this.toArray(new Object[size]);
	}

	@Override
	@SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
	public <T> T[] toArray(T[] a) {
		int size = this.size;
		if (size > a.length) {
			a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		}
		Object[] q = queue;
		int head = this.head;
		int tail = head + size;
		int capacity = q.length;
		if (tail <= capacity) {
			System.arraycopy(q, head, a, 0, size);
		} else {
			int l = capacity - head;
			System.arraycopy(q, head, a, 0, l);
			System.arraycopy(q, 0, a, l, tail - capacity);
		}
		if (size < a.length) {
			a[size] = null;
		}
		return a;
	}

	@Override
	public boolean remove(Object o) {
		if (o == null) {
			return false;
		}
		Object[] q = queue;
		int mask = q.length - 1;
		for (int i = this.head, to = this.size + i; i < to; i++) {
			int index = i & mask;
			if (!o.equals(q[index])) {
				continue;
			}
			removeIndex(q, i, to);
			return true;
		}
		return false;
	}

	private static int roundUpToPowerOfTwo(int n) {
		if (n <= 0) {
			throw new IllegalArgumentException("n must be positive: " + n);
		}
		if (n > MAX_CAPACITY) {
			throw new IllegalArgumentException("n cannot larger than " + MAX_CAPACITY + " : " + n);
		}
		n--;
		n |= n >> 1;
		n |= n >> 2;
		n |= n >> 4;
		n |= n >> 8;
		n |= n >> 16;
		return n + 1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		Iterator<E> it = iterator();
		if (it.hasNext()) {
			sb.append(it.next());
			while (it.hasNext()) {
				sb.append(", ");
				sb.append(it.next());
			}
		}
		sb.append(']');
		return sb.toString();
	}
}
