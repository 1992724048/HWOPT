package com.server.entity.mixin;

import com.server.entity.util.CollisionMapData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	
	@Overwrite
	protected void pushEntities() {
		LivingEntity self = (LivingEntity) (Object) this;
		List<Entity> colliding = CollisionMapData.getCollisionList(self);
		if (colliding.isEmpty()) return;
		AABB selfBox = self.getBoundingBox();
		for (Entity other : colliding) {
			if (other != null && selfBox.intersects(other.getBoundingBox())) {
				self.push(other);
			}
		}
	}
}