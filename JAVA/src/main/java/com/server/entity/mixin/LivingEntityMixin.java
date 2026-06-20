package com.server.entity.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Shadow
	protected abstract void doPush(Entity entity);

	@Overwrite
	protected void pushEntities() {
		LivingEntity self = (LivingEntity) (Object) this;
		List<Entity> pushable = self.level().getPushableEntities(self, self.getBoundingBox());

		if (pushable.isEmpty()) return;

		if (self.level() instanceof ServerLevel serverLevel) {
			int maxCramming = serverLevel.getGameRules().get(GameRules.MAX_ENTITY_CRAMMING);
			if (maxCramming > 0 && pushable.size() > maxCramming - 1 && self.getRandom().nextInt(4) == 0) {
				int cram = 0;
				for (Entity entity : pushable) {
					if (!entity.isPassenger()) cram++;
				}
				if (cram > maxCramming - 1) {
					self.hurtServer(serverLevel, self.damageSources().cramming(), 6.0F);
				}
			}
		}

		for (Entity entity : pushable) {
			this.doPush(entity);
		}
	}
}
