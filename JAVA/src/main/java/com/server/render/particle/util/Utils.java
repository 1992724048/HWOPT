package com.server.render.particle.util;

import java.util.Iterator;
import java.util.function.Consumer;

public class Utils {
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static final Iterator<?> DUMMY_ITERATOR = new Iterator() {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			return null;
		}

		@Override
		public void remove() {
		}

		@Override
		public void forEachRemaining(Consumer action) {
		}
	};
}
