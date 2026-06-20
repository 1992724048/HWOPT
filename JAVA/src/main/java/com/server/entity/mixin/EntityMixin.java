package com.server.entity.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

	@Overwrite
	private static Vec3 collideWithShapes(Vec3 movement, AABB boundingBox, List<VoxelShape> shapes) {
		if (shapes.isEmpty())
			return movement;

		Vec3 resolvedMovement = Vec3.ZERO;

		for (Direction.Axis axis : Direction.axisStepOrder(movement)) {
			double axisMovement = movement.get(axis);
			if (axisMovement != 0.0) {
				AABB movingBox = boundingBox.move(resolvedMovement);
				double collision = axisMovement;

				for (VoxelShape shape : shapes) {
					if (Math.abs(collision) < 1.0E-7) {
						collision = 0.0;
						break;
					}
					collision = shape.collide(axis, movingBox, collision);
				}

				resolvedMovement = resolvedMovement.with(axis, collision);
			}
		}

		return resolvedMovement;
	}
}
