package com.server.entity.aabb.util;

import net.minecraft.world.entity.Entity;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

public class CollisionMapData {
	private static int[] starts = new int[512];
	private static int[] buffer = new int[4096];
	private static int entityCount = 0;

	public static void build(int[] outA, int[] outB, int pairCount, int count, int[] tempIds) {
		int maxTempId = 0;
		for (int i = 0; i < count; i++) {
			if (tempIds[i] > maxTempId) maxTempId = tempIds[i];
		}
		maxTempId++;
		entityCount = maxTempId;

		if (starts.length < maxTempId + 1) {
			int newCap = maxTempId + (maxTempId >> 1) + 1;
			starts = new int[newCap];
		}
		int bufSize = pairCount * 2;
		if (buffer.length < bufSize) {
			int newCap = bufSize + (bufSize >> 1);
			buffer = new int[newCap];
		}

		java.util.Arrays.fill(starts, 0, maxTempId, 0);
		for (int i = 0; i < pairCount; i++) {
			starts[tempIds[outA[i]]]++;
			starts[tempIds[outB[i]]]++;
		}

		int offset = 0;
		for (int i = 0; i < maxTempId; i++) {
			int cnt = starts[i];
			starts[i] = offset;
			offset += cnt;
		}
		starts[maxTempId] = offset;

		if (offset > buffer.length) {
			int newCap = offset + (offset >> 1);
			buffer = new int[newCap];
		}

		int[] pos = new int[maxTempId];
		System.arraycopy(starts, 0, pos, 0, maxTempId);
		for (int i = 0; i < pairCount; i++) {
			int aTid = tempIds[outA[i]], bTid = tempIds[outB[i]];
			buffer[pos[aTid]++] = bTid;
			buffer[pos[bTid]++] = aTid;
		}
	}

	public static int getCollisionCount(int tempId) {
		if (tempId < 0 || tempId >= entityCount) return 0;
		return starts[tempId + 1] - starts[tempId];
	}

	public static List<Entity> getCollisionList(Entity source) {
		int id = TempID.getId(source);
		if (id < 0 || id >= entityCount) return Collections.emptyList();
		int start = starts[id];
		int end = starts[id + 1];
		if (start >= end) return Collections.emptyList();
		return new EntityFlatListView(start, end - start);
	}

	private static class EntityFlatListView extends AbstractList<Entity> {
		private final int offset;
		private final int length;

		EntityFlatListView(int offset, int length) {
			this.offset = offset;
			this.length = length;
		}

		@Override
		public Entity get(int index) {
			return TempID.getEntity(buffer[offset + index]);
		}

		@Override
		public int size() {
			return length;
		}
	}
}
