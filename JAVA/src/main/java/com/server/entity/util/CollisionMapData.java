package com.server.entity.util;

import net.minecraft.world.entity.Entity;

import java.util.*;

public class CollisionMapData {
	private static final Map<Entity, List<Entity>> collisionMap = new IdentityHashMap<>();
	
	public static void newTick() {
		collisionMap.clear();
	}
	
	public static void flush() {
		// no-op: map is ready for reads
	}
	
	public static void put(Entity a, Entity b) {
		addSingle(a, b);
		addSingle(b, a);
	}
	
	private static void addSingle(Entity key, Entity value) {
		collisionMap.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
	}
	
	public static List<Entity> get(Entity entity) {
		return collisionMap.get(entity);
	}
}