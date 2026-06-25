package com.server.entity.aabb.util;

import com.server.entity.aabb.access.IEntityNativeId;
import library.dll.AABBNative;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EntityPushSystem {
	private static final ThreadLocal<ReusableBuffers> TL_BUFFERS = ThreadLocal.withInitial(ReusableBuffers::new);

	private static class ReusableBuffers {
		Entity[] entities = new Entity[256];
		double[] aabbs = new double[256 * 6];
		int[] tempIds = new int[256];
		int[] outA = new int[1024];
		int[] outB = new int[1024];

		void ensureSize(int count) {
			int need = count * 6;
			if (aabbs.length < need) {
				int newCap = count + (count >> 1);
				entities = new Entity[newCap];
				aabbs = new double[newCap * 6];
				tempIds = new int[newCap];
				int pairCap = newCap * 4;
				outA = new int[pairCap];
				outB = new int[pairCap];
			}
		}
	}

	public static void tick(ServerLevel level) {
		ReusableBuffers buf = TL_BUFFERS.get();
		Entity[] all = buf.entities;
		double[] aabbs = buf.aabbs;
		int[] tempIds = buf.tempIds;

		int count = 0;
		for (Entity e : level.getAllEntities()) {
			if (e instanceof LivingEntity le && le.isAlive() && !e.isRemoved()) {
				if (count >= all.length) {
					int newCap = all.length + (all.length >> 1);
					all = buf.entities = java.util.Arrays.copyOf(all, newCap);
					aabbs = buf.aabbs = java.util.Arrays.copyOf(aabbs, newCap * 6);
					tempIds = buf.tempIds = java.util.Arrays.copyOf(tempIds, newCap);
				}
				all[count] = le;
				tempIds[count] = TempID.getId(le);
				((IEntityNativeId) le).hwopt$extractBoundingBox(aabbs, count * 6);
				count++;
			}
		}
		if (count == 0) return;

		buf.ensureSize(count);
		int[] outA = buf.outA;
		int[] outB = buf.outB;

		int pairCount = AABBNative.INSTANCE.batchFindCollisions(aabbs, outA, outB, count, outA.length);

		CollisionMapData.build(outA, outB, pairCount, count, tempIds);

		for (int i = 0; i < count; i++) {
			((IEntityNativeId) all[i]).hwopt$setCollisionCount(CollisionMapData.getCollisionCount(tempIds[i]));
		}
	}
}
