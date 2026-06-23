package com.server.entity.util;

import library.dll.AABBNative;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class EntityPushSystem {
	
	public static void tick(ServerLevel level) {
		CollisionMapData.newTick();
		
		List<LivingEntity> all = new ArrayList<>();
		for (Entity e : level.getAllEntities()) {
			if (e instanceof LivingEntity le && le.isAlive() && !e.isRemoved()) {
				all.add(le);
			}
		}
		if (all.isEmpty()) return;
		
		int count = all.size();
		double[] aabbs = new double[count * 6];
		
		for (int i = 0; i < count; i++) {
			AABB bb = all.get(i).getBoundingBox();
			int off = i * 6;
			aabbs[off] = bb.minX;
			aabbs[off + 1] = bb.minY;
			aabbs[off + 2] = bb.minZ;
			aabbs[off + 3] = bb.maxX;
			aabbs[off + 4] = bb.maxY;
			aabbs[off + 5] = bb.maxZ;
		}
		
		int maxPairs = count * 4;
		int[] outA = new int[maxPairs];
		int[] outB = new int[maxPairs];
		
		AABBNative nativeAABB = AABBNative.instance();
		int pairCount = nativeAABB.batchFindCollisions(aabbs, outA, outB, count, maxPairs);
		
		for (int i = 0; i < pairCount; i++) {
			int ia = outA[i];
			int ib = outB[i];
			if (ia >= count || ib >= count) continue;
			
			CollisionMapData.putCollision(ia, ib);
			
			LivingEntity a = all.get(ia);
			LivingEntity b = all.get(ib);
			if (a.getBoundingBox().intersects(b.getBoundingBox())) {
				a.push(b);
			}
		}
	}
}
