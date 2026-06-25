package com.server.render.entityculling.occlusion;

import com.hwpp.mod.Config;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HardwareOcclusionEngine {
	private boolean enabled = true;
	private int frameCounter = 0;
	
	private final Int2BooleanOpenHashMap entityCulled = new Int2BooleanOpenHashMap();
	private final Int2IntOpenHashMap entityLastUpdated = new Int2IntOpenHashMap();
	private final Int2IntOpenHashMap entitySkipCounter = new Int2IntOpenHashMap();
	private final Int2IntOpenHashMap entitySkipThreshold = new Int2IntOpenHashMap();
	
	private final Long2BooleanOpenHashMap beCulled = new Long2BooleanOpenHashMap();
	private final Long2IntOpenHashMap beLastUpdated = new Long2IntOpenHashMap();
	private final Long2IntOpenHashMap beSkipCounter = new Long2IntOpenHashMap();
	private final Long2IntOpenHashMap beSkipThreshold = new Long2IntOpenHashMap();
	private final Long2ObjectOpenHashMap<AABB> beAABBCache = new Long2ObjectOpenHashMap<>();
	
	private final java.util.ArrayList<BlockEntity> renderedBlockEntities = new java.util.ArrayList<>();
	
	private static HardwareOcclusionEngine instance;
	
	public static HardwareOcclusionEngine getInstance() {
		if (instance == null) {
			instance = new HardwareOcclusionEngine();
		}
		return instance;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void ensurePool() {
	}
	
	public void processEntityResults() {
	}
	
	public void processBlockEntityResults() {
	}
	
	public void submitEntityQuery(Entity entity, Camera camera) {
		submitEntityQuery(entity, camera.position());
	}
	
	public void submitEntityQuery(Entity entity, Vec3 cameraPos) {
		if (!enabled) return;
		if (entity instanceof Player) return;
		if (entity.isCurrentlyGlowing()) return;
		if (entity.isPassenger()) return;
		
		int entityId = entity.getId();
		
		int threshold = entitySkipThreshold.getOrDefault(entityId, 1);
		int skipped = entitySkipCounter.getOrDefault(entityId, 0);
		skipped++;
		if (skipped < threshold) {
			entitySkipCounter.put(entityId, skipped);
			return;
		}
		
		double dx = entity.getX() - cameraPos.x;
		double dy = entity.getY() - cameraPos.y;
		double dz = entity.getZ() - cameraPos.z;
		double distSq = dx * dx + dy * dy + dz * dz;
		
        int nearSq = Config.CONFIG.nearDist.get() * Config.CONFIG.nearDist.get();
        int midSq = Config.CONFIG.midDist.get() * Config.CONFIG.midDist.get();
        if (distSq < nearSq) entitySkipThreshold.put(entityId, (int)1);
        else if (distSq < midSq) entitySkipThreshold.put(entityId, (int)Config.CONFIG.freqMid.get());
        else entitySkipThreshold.put(entityId, (int)Config.CONFIG.freqFar.get());
		
		AABB aabb = entity.getBoundingBox();
		boolean visible = isAABBVisible(cameraPos, aabb);
		entityCulled.put(entityId, !visible);
		entityLastUpdated.put(entityId, frameCounter);
		entitySkipCounter.put(entityId, 0);
		if (!visible) entitySkipThreshold.put(entityId, 1);
	}
	
	public void submitBlockEntityQuery(BlockEntity blockEntity, Camera camera) {
		submitBlockEntityQuery(blockEntity, camera.position());
	}
	
	public void submitBlockEntityQuery(BlockEntity blockEntity, Vec3 cameraPos) {
		if (!enabled) return;
		long beId = blockEntity.getBlockPos().asLong();
		
		int threshold = beSkipThreshold.getOrDefault(beId, 1);
		int skipped = beSkipCounter.getOrDefault(beId, 0);
		skipped++;
		if (skipped < threshold) {
			beSkipCounter.put(beId, skipped);
			return;
		}
		
		BlockPos pos = blockEntity.getBlockPos();
		double dx = pos.getX() + 0.5 - cameraPos.x;
		double dy = pos.getY() + 0.5 - cameraPos.y;
		double dz = pos.getZ() + 0.5 - cameraPos.z;
		double distSq = dx * dx + dy * dy + dz * dz;
		int nearSq = Config.CONFIG.nearDist.get() * Config.CONFIG.nearDist.get();
		int midSq = Config.CONFIG.midDist.get() * Config.CONFIG.midDist.get();
		if (distSq < nearSq) beSkipThreshold.put(beId, (int)1);
		else if (distSq < midSq) beSkipThreshold.put(beId, (int)Config.CONFIG.freqMid.get());
		else beSkipThreshold.put(beId, (int)Config.CONFIG.freqFar.get());
		boolean wasInvisible = beCulled.containsKey(beId) && beCulled.get(beId);
		if (wasInvisible) beSkipThreshold.put(beId, (int)1);
		
		AABB aabb = getBlockEntityAABB(blockEntity);
		boolean visible = isAABBVisible(cameraPos, aabb);
		beCulled.put(beId, !visible);
		beLastUpdated.put(beId, frameCounter);
		beSkipCounter.put(beId, 0);
	}
	
	private boolean isAABBVisible(Vec3 from, AABB aabb) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return true;
		
		double dist = Math.sqrt(aabb.getCenter().distanceToSqr(from));
		if (dist < 1.5) return true;
		
		// Sample multiple points on the AABB and check if any ray reaches them
		if (traceRay(from, aabb.getCenter(), level)) return true;
		
		double minX = aabb.minX, minY = aabb.minY, minZ = aabb.minZ;
		double maxX = aabb.maxX, maxY = aabb.maxY, maxZ = aabb.maxZ;
		Vec3[] corners = new Vec3[]{new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ), new Vec3(minX, minY, maxZ), new Vec3(maxX, minY, maxZ), new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ)};
		for (Vec3 corner : corners) {
			if (corner.distanceToSqr(from) < 2.25) return true;
			if (traceRay(from, corner, level)) return true;
		}
		return false;
	}
	
	private boolean traceRay(Vec3 from, Vec3 to, ClientLevel level) {
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double dz = to.z - from.z;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (dist < 0.5) return true;
		if (dist > getTraceDist()) return true;
		
		// 3D DDA (Digital Differential Analyzer) ray marching
		double dirX = dx / dist, dirY = dy / dist, dirZ = dz / dist;
		
		int stepX = dirX > 0 ? 1 : -1;
		int stepY = dirY > 0 ? 1 : -1;
		int stepZ = dirZ > 0 ? 1 : -1;
		
		int bX = (int) Math.floor(from.x);
		int bY = (int) Math.floor(from.y);
		int bZ = (int) Math.floor(from.z);
		
		double tDeltaX = Math.abs(1.0 / dirX);
		double tDeltaY = Math.abs(1.0 / dirY);
		double tDeltaZ = Math.abs(1.0 / dirZ);
		
		double tMaxX;
		if (dirX > 0) tMaxX = ((bX + 1) - from.x) / dirX;
		else if (dirX < 0) tMaxX = (from.x - bX) / -dirX;
		else tMaxX = Double.MAX_VALUE;
		
		double tMaxY;
		if (dirY > 0) tMaxY = ((bY + 1) - from.y) / dirY;
		else if (dirY < 0) tMaxY = (from.y - bY) / -dirY;
		else tMaxY = Double.MAX_VALUE;
		
		double tMaxZ;
		if (dirZ > 0) tMaxZ = ((bZ + 1) - from.z) / dirZ;
		else if (dirZ < 0) tMaxZ = (from.z - bZ) / -dirZ;
		else tMaxZ = Double.MAX_VALUE;
		
		int chunkX = Integer.MIN_VALUE;
		int maxSteps = getTraceDist();
		for (int i = 0; i < maxSteps; i++) {
			// Check if we've reached or passed the target
			boolean reachedX = dirX > 0 ? (bX >= Math.floor(to.x)) : (bX <= Math.floor(to.x));
			boolean reachedY = dirY > 0 ? (bY >= Math.floor(to.y)) : (bY <= Math.floor(to.y));
			boolean reachedZ = dirZ > 0 ? (bZ >= Math.floor(to.z)) : (bZ <= Math.floor(to.z));
			if (reachedX && reachedY && reachedZ) return true;
			
			// Advance to next block
			if (tMaxX < tMaxY) {
				if (tMaxX < tMaxZ) {
					bX += stepX;
					tMaxX += tDeltaX;
				} else {
					bZ += stepZ;
					tMaxZ += tDeltaZ;
				}
			} else {
				if (tMaxY < tMaxZ) {
					bY += stepY;
					tMaxY += tDeltaY;
				} else {
					bZ += stepZ;
					tMaxZ += tDeltaZ;
				}
			}
			
			// Check chunk availability
			int cX = bX >> 4;
			if (cX != chunkX) {
				chunkX = cX;
				if (!level.hasChunk(cX, bZ >> 4)) return true;
			}
			
			BlockPos pos = new BlockPos(bX, bY, bZ);
			BlockState state = level.getBlockState(pos);
			if (state.isSolidRender() || (Config.CONFIG.solidLeaves.get() && state.getBlock() instanceof LeavesBlock)) {
				return false;
			}
		}
		return true;
	}
	
	private int getTraceDist() {
		int renderDist = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
		return Math.min(Config.CONFIG.maxTraceDist.get(), Math.max(renderDist, 16));
	}

	public void trackRenderedBlockEntity(BlockEntity be) {
		if (enabled) renderedBlockEntities.add(be);
	}
	
	public java.util.ArrayList<BlockEntity> getRenderedBlockEntities() {
		return renderedBlockEntities;
	}
	
	public void clearRenderedBlockEntities() {
		renderedBlockEntities.clear();
	}
	
	private AABB getBlockEntityAABB(BlockEntity blockEntity) {
		long beId = blockEntity.getBlockPos().asLong();
		AABB cached = beAABBCache.get(beId);
		if (cached != null) return cached;
		
		BlockPos pos = blockEntity.getBlockPos();
		var level = blockEntity.getLevel();
		AABB aabb;
		if (level == null) {
			aabb = new AABB(pos);
		} else {
			var state = blockEntity.getBlockState();
			VoxelShape shape = state.getShape(level, pos);
			if (shape.isEmpty()) aabb = new AABB(pos);
			else aabb = shape.bounds().move(pos);
		}
		if (beAABBCache.size() > Config.CONFIG.maxBeCache.get()) beAABBCache.clear();
		beAABBCache.put(beId, aabb);
		return aabb;
	}
	
	public boolean isEntityCulled(int entityId) {
		if (!enabled) return false;
		if (!entityCulled.containsKey(entityId)) return false;
		int updated = entityLastUpdated.getOrDefault(entityId, 0);
		if (frameCounter - updated > Config.CONFIG.staleFrames.get()) {
			entityCulled.remove(entityId);
			entityLastUpdated.remove(entityId);
			entitySkipCounter.remove(entityId);
			entitySkipThreshold.remove(entityId);
			return false;
		}
		return entityCulled.get(entityId);
	}
	
	public boolean isBlockEntityCulled(long beId) {
		if (!enabled) return false;
		if (!beCulled.containsKey(beId)) return false;
		int updated = beLastUpdated.getOrDefault(beId, 0);
		if (frameCounter - updated > Config.CONFIG.staleFrames.get()) {
			beCulled.remove(beId);
			beLastUpdated.remove(beId);
			beSkipCounter.remove(beId);
			beSkipThreshold.remove(beId);
			return false;
		}
		return beCulled.get(beId);
	}
	
	public void advanceFrame() {
		frameCounter++;
	}
	
	public void onWorldChange() {
		entityCulled.clear();
		entityLastUpdated.clear();
		entitySkipCounter.clear();
		entitySkipThreshold.clear();
		beCulled.clear();
		beLastUpdated.clear();
		beSkipCounter.clear();
		beSkipThreshold.clear();
		beAABBCache.clear();
		renderedBlockEntities.clear();
		frameCounter = 0;
	}
	
	public void cleanup() {
		onWorldChange();
	}
}
