package com.server.entity.mixin;

import com.server.entity.util.CollisionMapData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	
	@Overwrite
	protected void pushEntities() {
		LivingEntity self = (LivingEntity) (Object) this;
		List<Entity> colliding = CollisionMapData.get(self);
		if (colliding == null || colliding.isEmpty()) return;
		for (Entity other : colliding) {
			self.push(other);
		}
	}
}