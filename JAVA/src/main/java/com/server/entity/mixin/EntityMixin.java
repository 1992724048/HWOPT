package com.server.entity.mixin;

import library.dll.AABBNative;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {
	
	// ── cached AABB for fast extraction ────────────────────────
	@Unique
	private double hwopt$bbMinX;
	@Unique
	private double hwopt$bbMinY;
	@Unique
	private double hwopt$bbMinZ;
	@Unique
	private double hwopt$bbMaxX;
	@Unique
	private double hwopt$bbMaxY;
	@Unique
	private double hwopt$bbMaxZ;
	
	@Inject(method = "setBoundingBox", at = @At("RETURN"))
	private void hwopt$onSetBoundingBox(AABB bb, CallbackInfo ci) {
		this.hwopt$bbMinX = bb.minX;
		this.hwopt$bbMinY = bb.minY;
		this.hwopt$bbMinZ = bb.minZ;
		this.hwopt$bbMaxX = bb.maxX;
		this.hwopt$bbMaxY = bb.maxY;
		this.hwopt$bbMaxZ = bb.maxZ;
	}
	
	@Unique
	public void hwopt$extractBoundingBox(double[] out, int offset, double inflate) {
		out[offset] = this.hwopt$bbMinX - inflate;
		out[offset + 1] = this.hwopt$bbMinY - inflate;
		out[offset + 2] = this.hwopt$bbMinZ - inflate;
		out[offset + 3] = this.hwopt$bbMaxX + inflate;
		out[offset + 4] = this.hwopt$bbMaxY + inflate;
		out[offset + 5] = this.hwopt$bbMaxZ + inflate;
	}
	
	// ── Block collision batch ──────────────────────────────────
	@Unique
	private static double[] hwopt$boxCache = new double[384];
	
	@Unique
	private static int hwopt$flattenShapes(List<VoxelShape> shapes) {
		int pos = 0;
		for (VoxelShape shape : shapes) {
			List<AABB> aabbs = shape.toAabbs();
			int need = pos + aabbs.size() * 6;
			if (hwopt$boxCache.length < need) {
				hwopt$boxCache = new double[need * 2];
			}
			for (AABB aabb : aabbs) {
				hwopt$boxCache[pos++] = aabb.minX;
				hwopt$boxCache[pos++] = aabb.minY;
				hwopt$boxCache[pos++] = aabb.minZ;
				hwopt$boxCache[pos++] = aabb.maxX;
				hwopt$boxCache[pos++] = aabb.maxY;
				hwopt$boxCache[pos++] = aabb.maxZ;
			}
		}
		return pos;
	}
	
	@Overwrite
	private static Vec3 collideWithShapes(Vec3 movement, AABB boundingBox, List<VoxelShape> shapes) {
		if (shapes.isEmpty()) return movement;
		
		int boxCount = hwopt$flattenShapes(shapes);
		if (boxCount == 0) return movement;
		
		Vec3 resolvedMovement = Vec3.ZERO;
		AABBNative nativeAABB = AABBNative.instance();
		
		for (Direction.Axis axis : Direction.axisStepOrder(movement)) {
			double axisMovement = movement.get(axis);
			if (axisMovement == 0.0) continue;
			
			AABB movingBox = boundingBox.move(resolvedMovement);
			double collision = nativeAABB.batchCollideAxis(axis.ordinal(), movingBox.minX, movingBox.minY, movingBox.minZ, movingBox.maxX, movingBox.maxY, movingBox.maxZ, hwopt$boxCache, axisMovement);
			
			resolvedMovement = resolvedMovement.with(axis, collision);
		}
		
		return resolvedMovement;
	}
}