package com.server.entity.aabb.mixin;

import com.server.entity.aabb.access.IEntityNativeId;
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

import static net.minecraft.core.Direction.Axis.*;

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
	private int hwopt$nativeId = -1;
	@Unique
	private int hwopt$collisionCount = 0;
	
	@Override
	public int hwopt$getNativeId() {
		return hwopt$nativeId;
	}
	
	@Override
	public void hwopt$setNativeId(int id) {
		this.hwopt$nativeId = id;
	}
	
	@Override
	public int hwopt$getCollisionCount() {
		return hwopt$collisionCount;
	}
	
	@Override
	public void hwopt$setCollisionCount(int count) {
		this.hwopt$collisionCount = count;
	}
	
	@Override
	public void hwopt$extractBoundingBox(double[] arr, int offset) {
		arr[offset] = this.hwopt$bbMinX;
		arr[offset + 1] = this.hwopt$bbMinY;
		arr[offset + 2] = this.hwopt$bbMinZ;
		arr[offset + 3] = this.hwopt$bbMaxX;
		arr[offset + 4] = this.hwopt$bbMaxY;
		arr[offset + 5] = this.hwopt$bbMaxZ;
	}
	
	@Override
	public void hwopt$extractPosition(double[] arr, int offset) {
		arr[offset] = ((Entity) (Object) this).getX();
		arr[offset + 1] = ((Entity) (Object) this).getY();
		arr[offset + 2] = ((Entity) (Object) this).getZ();
	}
	
	@Unique
	private static final ThreadLocal<double[]> hwopt$boxCacheTL = ThreadLocal.withInitial(() -> new double[384]);
	
	@Unique
	private static int hwopt$flattenShapes(List<VoxelShape> shapes) {
		double[] cache = hwopt$boxCacheTL.get();
		int pos = 0;
		for (VoxelShape shape : shapes) {
			List<AABB> aabbs = shape.toAabbs();
			int need = pos + aabbs.size() * 6;
			if (cache.length < need) {
				cache = new double[need * 2];
				hwopt$boxCacheTL.set(cache);
			}
			for (AABB aabb : aabbs) {
				cache[pos++] = aabb.minX;
				cache[pos++] = aabb.minY;
				cache[pos++] = aabb.minZ;
				cache[pos++] = aabb.maxX;
				cache[pos++] = aabb.maxY;
				cache[pos++] = aabb.maxZ;
			}
		}
		for (int i = pos; i < cache.length; i += 6) {
			cache[i] = Double.MAX_VALUE;
			cache[i + 1] = Double.MAX_VALUE;
			cache[i + 2] = Double.MAX_VALUE;
			cache[i + 3] = -Double.MAX_VALUE;
			cache[i + 4] = -Double.MAX_VALUE;
			cache[i + 5] = -Double.MAX_VALUE;
		}
		return pos;
	}
	
	@Overwrite
	private static Vec3 collideWithShapes(Vec3 movement, AABB boundingBox, List<VoxelShape> shapes) {
		if (shapes.isEmpty()) return movement;
		
		int boxCount = hwopt$flattenShapes(shapes);
		if (boxCount == 0) return movement;
		
		double rx = 0.0, ry = 0.0, rz = 0.0;
		
		double mx = movement.x, my = movement.y, mz = movement.z;

		for (Direction.Axis axis : Direction.axisStepOrder(movement)) {
			double axisMovement = axis == X ? mx : axis == Y ? my : mz;
			if (axisMovement == 0.0) continue;
			
			double movingMinX = boundingBox.minX + rx;
			double movingMinY = boundingBox.minY + ry;
			double movingMinZ = boundingBox.minZ + rz;
			double movingMaxX = boundingBox.maxX + rx;
			double movingMaxY = boundingBox.maxY + ry;
			double movingMaxZ = boundingBox.maxZ + rz;
			
			double collision = AABBNative.INSTANCE.batchCollideAxis(axis.ordinal(), movingMinX, movingMinY, movingMinZ, movingMaxX, movingMaxY, movingMaxZ, hwopt$boxCacheTL.get(), axisMovement);
			
			if (axis == X) rx = collision;
			else if (axis == Y) ry = collision;
			else rz = collision;
		}
		
		return new Vec3(rx, ry, rz);
	}
}
