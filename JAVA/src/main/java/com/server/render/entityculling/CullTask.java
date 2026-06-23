package com.server.render.entityculling;

import com.hwpp.mod.Config;
import com.server.render.entityculling.access.Cullable;
import com.server.render.entityculling.occlusion.OcclusionCullingInstance;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CullTask implements Runnable {
	private final OcclusionCullingInstance culling;
	private volatile List<Entity> entities = List.of();
	private volatile Vec3 cameraPos = Vec3.ZERO;
	private volatile BlockGetter level;

	public CullTask(OcclusionCullingInstance culling) {
		this.culling = culling;
	}

	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}

	public void setCameraPos(Vec3 pos) {
		this.cameraPos = pos;
	}

	public void setLevel(BlockGetter level) {
		this.level = level;
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			List<Entity> currentEntities = this.entities;
			Vec3 cam = this.cameraPos;
			BlockGetter world = this.level;
			var cfg = Config.get();
			if (world != null && !currentEntities.isEmpty()) {
				try {
					culling.resetCache();
					Long2ObjectOpenHashMap<List<Entity>> cells = new Long2ObjectOpenHashMap<>();
					for (Entity entity : currentEntities) {
						if (Thread.interrupted()) return;
						Cullable cullable = (Cullable) entity;
						if (cullable.isForcedVisible()) continue;

						Vec3 pos = entity.position();
						double distSq = pos.distanceToSqr(cam);
						if (distSq > (double) cfg.tracingDistance * cfg.tracingDistance) {
							cullable.setCulled(false);
							continue;
						}
						AABB box = entity.getBoundingBox();
						if (box.getSize() > cfg.hitboxLimit) {
							cullable.setCulled(false);
							continue;
						}

						long cellKey = (((long) ((int) Math.floor(pos.x / 2.0))) << 42)
							| (((long) ((int) Math.floor(pos.y / 2.0))) << 21)
							| ((long) ((int) Math.floor(pos.z / 2.0)));
						cells.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(entity);
					}

					for (var entry : cells.long2ObjectEntrySet()) {
						if (Thread.interrupted()) return;
						List<Entity> group = entry.getValue();
						Entity first = group.get(0);
						Vec3 pos = first.position();
						int cx = ((int) Math.floor(pos.x / 2.0)) * 2 + 1;
						int cy = ((int) Math.floor(pos.y / 2.0)) * 2 + 1;
						int cz = ((int) Math.floor(pos.z / 2.0)) * 2 + 1;
						AABB cellBox = new AABB(cx - 1, cy - 1, cz - 1, cx + 1, cy + 1, cz + 1);
						boolean visible = culling.isAABBVisible(cellBox, cam, world);
						for (Entity entity : group) {
							((Cullable) entity).setCulled(!visible);
						}
					}
				} catch (Exception ignored) {
				}
			}
			try {
				Thread.sleep(cfg.sleepDelay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
}
