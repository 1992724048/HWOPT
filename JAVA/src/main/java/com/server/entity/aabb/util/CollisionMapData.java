package com.server.entity.aabb.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.entity.Entity;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

public class CollisionMapData {
	private static final Int2ObjectOpenHashMap<IntArrayList> collisionMap = new Int2ObjectOpenHashMap<>(8192);
	
	public static void newTick() {
		collisionMap.clear();
	}
	
	public static void putCollision(int idA, int idB) {
		addSingle(idA, idB);
		addSingle(idB, idA);
	}
	
	private static void addSingle(int source, int target) {
		IntArrayList list = collisionMap.get(source);
		if (list == null) {
			list = new IntArrayList(2);
			collisionMap.put(source, list);
		}
		list.add(target);
	}
	
	public static List<Entity> getCollisionList(Entity source) {
		IntArrayList ids = collisionMap.get(TempID.getId(source));
		if (ids == null || ids.isEmpty()) return Collections.emptyList();
		return new EntityListView(ids);
	}
	
	private static class EntityListView extends AbstractList<Entity> {
		private final IntArrayList ids;
		
		EntityListView(IntArrayList ids) {
			this.ids = ids;
		}
		
		@Override
		public Entity get(int index) {
			return TempID.getEntity(ids.getInt(index));
		}
		
		@Override
		public int size() {
			return ids.size();
		}
	}
}
