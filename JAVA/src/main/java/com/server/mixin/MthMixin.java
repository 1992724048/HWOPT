package com.server.mixin;

import library.dll.MathNative;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Mth.class)
public abstract class MthMixin {
	@Shadow
	@Deprecated
	public static double fastInvSqrt(double x) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}
	
	@Overwrite
	public static double lerp3(double alpha1, double alpha2, double alpha3, double x000, double x100, double x010, double x110, double x001, double x101, double x011, double x111) {
		return MathNative.INSTANCE.lerp3(alpha1, alpha2, alpha3, x000, x100, x010, x110, x001, x101, x011, x111);
	}
	
	@Overwrite
	public static boolean rayIntersectsAABB(Vec3 rayStart, Vec3 rayDir, AABB aabb) {
		return MathNative.INSTANCE.rayIntersectsAABB(rayStart.x, rayStart.y, rayStart.z, rayDir.x, rayDir.y, rayDir.z, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
	}
	
	@Overwrite
	public static double atan2(double y, double x) {
		return MathNative.INSTANCE.atan2(y, x);
	}
	
	@Overwrite
	public static double clampedLerp(double factor, double min, double max) {
		return MathNative.INSTANCE.clamped_lerp(factor, min, max);
	}
}
