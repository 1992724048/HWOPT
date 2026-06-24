package com.server.entity.aabb.util;

import com.server.entity.aabb.access.IEntityNativeId;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;

public class TempID {
	private static Entity[] frameSnapshot = new Entity[512];
	private static int currentIndex = 0;
	
	public static void tickStart() {
		if (currentIndex > 0) Arrays.fill(frameSnapshot, 0, currentIndex, null);
		currentIndex = 0;
	}
	
	public static int addEntity(Entity e) {
		if (currentIndex >= frameSnapshot.length) resize();
		int id = currentIndex++;
		frameSnapshot[id] = e;
		((IEntityNativeId) e).hwopt$setNativeId(id);
		return id;
	}
	
	public static Entity getEntity(int id) {
		return (id >= 0 && id < currentIndex) ? frameSnapshot[id] : null;
	}
	
	public static int getId(Entity e) {
		return ((IEntityNativeId) e).hwopt$getNativeId();
	}
	
	private static void resize() {
		int newSize = frameSnapshot.length + (frameSnapshot.length >> 1);
		frameSnapshot = Arrays.copyOf(frameSnapshot, newSize);
	}
}
