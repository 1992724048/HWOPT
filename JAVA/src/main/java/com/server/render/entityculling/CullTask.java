package com.server.render.entityculling;

import com.hwpp.mod.Config;
import com.hwpp.mod.HWOPT;
import com.server.render.entityculling.access.Cullable;
import com.server.render.entityculling.occlusion.OcclusionCullingInstance;
import com.server.render.entityculling.occlusion.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;

public class CullTask implements Runnable {
	public boolean requestCull = false;
	public boolean disableEntityCulling = false;
	public boolean disableBlockEntityCulling = false;
	
	private final OcclusionCullingInstance culling;
	private final Minecraft client = Minecraft.getInstance();
	private final int sleepDelay;
	private final int hitboxLimit;
	private final Set<BlockEntityType<?>> blockEntityWhitelist;
	private final Set<EntityType<?>> entityWhitelist;
	public double lastTime = 0;
	
	private final Vec3d lastPos = new Vec3d(0, 0, 0);
	private final Vec3d aabbMin = new Vec3d(0, 0, 0);
	private final Vec3d aabbMax = new Vec3d(0, 0, 0);
	
	private boolean ingame = false;
	private List<Entity> entitiesForRendering = new ArrayList<>();
	private Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
	private Vec3 cameraMC = new Vec3(0, 0, 0);
	
	public CullTask(OcclusionCullingInstance culling, Set<BlockEntityType<?>> blockEntityWhitelist, Set<EntityType<?>> entityWhitelist) {
		this.culling = culling;
		this.blockEntityWhitelist = blockEntityWhitelist;
		this.entityWhitelist = entityWhitelist;
		this.sleepDelay = Config.CONFIG.sleepDelay.get();
		this.hitboxLimit = Config.CONFIG.hitboxLimit.get();
	}
	
	public void setIngame(boolean ingame) {
		this.ingame = ingame;
	}
	
	public void setEntitiesForRendering(List<Entity> entities) {
		this.entitiesForRendering = entities;
	}
	
	public void setBlockEntities(Map<BlockPos, BlockEntity> blockEntities) {
		this.blockEntities = blockEntities;
	}
	
	public void setCameraMC(Vec3 pos) {
		this.cameraMC = pos;
	}
	
	@Override
	public void run() {
		while (client.isRunning()) {
			try {
				Thread.sleep(sleepDelay);
				if (EntityCulling.enabled && ingame && !client.isPaused()) {
					if (requestCull || !(cameraMC.x == lastPos.x && cameraMC.y == lastPos.y && cameraMC.z == lastPos.z)) {
						long start = System.nanoTime();
						requestCull = false;
						lastPos.set(cameraMC.x, cameraMC.y, cameraMC.z);
						Vec3d camera = lastPos;
						culling.resetCache();
						cullBlockEntities(cameraMC, camera);
						cullEntities(cameraMC, camera);
						lastTime = (System.nanoTime() - start) / 1_000_000.0;
					}
				} else {
					lastTime = 0;
				}
			} catch (Exception e) {
				HWOPT.LOGGER.error(e.toString());
			}
		}
	}
	
	private void cullEntities(Vec3 cameraMC, Vec3d camera) {
		if (disableEntityCulling) return;
		EntityCulling mod = EntityCulling.getInstance();
		int tracingDist = Math.max(client.options.renderDistance().get() * 16, 16);
		
		Int2ObjectOpenHashMap<Group> groupMap = new Int2ObjectOpenHashMap<>();
		
		for (Entity entity : entitiesForRendering) {
			if (entity == null) break;
			if (!(entity instanceof Cullable cullable)) continue;
			if (entityWhitelist.contains(entity.getType())) continue;
			if (mod.isDynamicWhitelisted(entity)) continue;
			if (cullable.isForcedVisible()) continue;
			if (client.shouldEntityAppearGlowing(entity)) {
				cullable.setCulled(false);
				continue;
			}
			if (!entity.position().closerThan(cameraMC, tracingDist)) {
				cullable.setCulled(false);
				continue;
			}
			AABB boundingBox = NMSCullingHelper.getCullingBox(entity);
			if (boundingBox == null || boundingBox.getXsize() > hitboxLimit || boundingBox.getYsize() > hitboxLimit || boundingBox.getZsize() > hitboxLimit) {
				cullable.setCulled(false);
				continue;
			}
			
			int gx = (int) Math.floor(entity.position().x / 2);
			int gy = (int) Math.floor(entity.position().y / 2);
			int gz = (int) Math.floor(entity.position().z / 2);
			int key = gx * 163 + gy * 389 + gz;
			
			Group group = groupMap.get(key);
			if (group == null) {
				group = new Group();
				groupMap.put(key, group);
			}
			group.add(entity, boundingBox);
		}
		
		for (var entry : groupMap.int2ObjectEntrySet()) {
			Group group = entry.getValue();
			aabbMin.set(group.minX, group.minY, group.minZ);
			aabbMax.set(group.maxX, group.maxY, group.maxZ);
			boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
			for (Entity entity : group.entities) {
				((Cullable) entity).setCulled(!visible);
			}
		}
	}
	
	private static final class Group {
		final List<Entity> entities = new ArrayList<>(4);
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		
		void add(Entity entity, AABB box) {
			entities.add(entity);
			if (box.minX < minX) minX = box.minX;
			if (box.minY < minY) minY = box.minY;
			if (box.minZ < minZ) minZ = box.minZ;
			if (box.maxX > maxX) maxX = box.maxX;
			if (box.maxY > maxY) maxY = box.maxY;
			if (box.maxZ > maxZ) maxZ = box.maxZ;
		}
	}
	
	private void cullBlockEntities(Vec3 cameraMC, Vec3d camera) {
		if (disableBlockEntityCulling) return;
		EntityCulling mod = EntityCulling.getInstance();
		Iterator<Map.Entry<BlockPos, BlockEntity>> iterator = blockEntities.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<BlockPos, BlockEntity> entry;
			try {
				entry = iterator.next();
			} catch (NullPointerException | ConcurrentModificationException ex) {
				break;
			}
			if (entry == null) break;
			if (blockEntityWhitelist.contains(entry.getValue().getType())) continue;
			if (client.getBlockEntityRenderDispatcher().getRenderer(entry.getValue()) == null) continue;
			if (mod.isDynamicWhitelisted(entry.getValue())) continue;
			Cullable cullable = (Cullable) entry.getValue();
			if (!cullable.isForcedVisible()) {
				BlockPos pos = entry.getKey();
				int blockDist = Math.max(client.options.renderDistance().get() * 16, 16);
				if (closerThan(pos, cameraMC, blockDist)) {
					AABB boundingBox = mod.setupAABB(entry.getValue(), pos);
					if (boundingBox.getXsize() > hitboxLimit || boundingBox.getYsize() > hitboxLimit || boundingBox.getZsize() > hitboxLimit) {
						cullable.setCulled(false);
						continue;
					}
					aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
					aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
					boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
					cullable.setCulled(!visible);
				}
			}
		}
	}
	
	private static boolean closerThan(BlockPos blockPos, Position position, double d) {
		return distSqr(blockPos, position.x(), position.y(), position.z(), true) < d * d;
	}
	
	private static double distSqr(BlockPos blockPos, double d, double e, double f, boolean bl) {
		double g = bl ? 0.5D : 0.0D;
		double h = (double) blockPos.getX() + g - d;
		double i = (double) blockPos.getY() + g - e;
		double j = (double) blockPos.getZ() + g - f;
		return h * h + i * i + j * j;
	}
}
