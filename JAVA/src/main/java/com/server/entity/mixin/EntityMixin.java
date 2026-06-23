package com.server.entity.mixin;

import com.server.entity.access.IEntityNativeId;
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

import static net.minecraft.core.Direction.Axis.X;
import static net.minecraft.core.Direction.Axis.Y;
import static net.minecraft.core.Direction.Axis.Z;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntityNativeId {
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
	
	@Unique
	private int hwopt$nativeId = -1;
	
	@Override
	public int hwopt$getNativeId() {
		return hwopt$nativeId;
	}
	
	@Override
	public void hwopt$setNativeId(int id) {
		this.hwopt$nativeId = id;
	}
	
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
		
		AABBNative nativeAABB = AABBNative.instance();
		double rx = 0.0, ry = 0.0, rz = 0.0;
		
		double mx = movement.x, my = movement.y, mz = movement.z;
		double ax = Math.abs(mx), ay = Math.abs(my), az = Math.abs(mz);
		
		Direction.Axis a1, a2, a3;
		if (ax > ay) {
			if (az > ax) {
				a1 = Z;
				a2 = X;
				a3 = Y;
			} else {
				a1 = X;
				a2 = az > ay ? Z : Y;
				a3 = az > ay ? Y : Z;
			}
		} else {
			if (az > ay) {
				a1 = Z;
				a2 = Y;
				a3 = X;
			} else {
				a1 = Y;
				a2 = az > ax ? Z : X;
				a3 = az > ax ? X : Z;
			}
		}
		
		for (int i = 0; i < 3; i++) {
			Direction.Axis axis = switch (i) {
				case 0 -> a1;
				case 1 -> a2;
				default -> a3;
			};
			double axisMovement = axis == X ? mx : axis == Y ? my : mz;
			if (axisMovement == 0.0) continue;
			
			double movingMinX = boundingBox.minX + rx;
			double movingMinY = boundingBox.minY + ry;
			double movingMinZ = boundingBox.minZ + rz;
			double movingMaxX = boundingBox.maxX + rx;
			double movingMaxY = boundingBox.maxY + ry;
			double movingMaxZ = boundingBox.maxZ + rz;
			
			double collision = nativeAABB.batchCollideAxis(axis.ordinal(), movingMinX, movingMinY, movingMinZ, movingMaxX, movingMaxY, movingMaxZ, hwopt$boxCache, axisMovement);
			
			if (axis == X) rx = collision;
			else if (axis == Y) ry = collision;
			else rz = collision;
		}
		
		return new Vec3(rx, ry, rz);
	}
}