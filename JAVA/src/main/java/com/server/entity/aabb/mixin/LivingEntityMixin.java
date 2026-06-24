package com.server.entity.aabb.mixin;

import com.server.entity.aabb.access.IEntityNativeId;
import com.server.entity.aabb.util.CollisionMapData;
import com.hwpp.mod.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Unique
	private int hwopt$lastClimbableTick = -1;
	@Unique
	private boolean hwopt$cachedClimbable = false;
	
	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	private void hwopt$onClimbableHead(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.tickCount == this.hwopt$lastClimbableTick) {
			cir.setReturnValue(this.hwopt$cachedClimbable);
		}
	}
	
	@Inject(method = "onClimbable", at = @At("RETURN"))
	private void hwopt$onClimbableReturn(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		this.hwopt$lastClimbableTick = self.tickCount;
		this.hwopt$cachedClimbable = cir.getReturnValueZ();
	}
	
	@Overwrite
	protected void pushEntities() {
		LivingEntity self = (LivingEntity) (Object) this;
		if (((IEntityNativeId) self).hwopt$getCollisionCount() < Config.CONFIG.entityDensityThreshold.get()) return;
		if (self.isPassenger()) return;
		List<Entity> colliding = CollisionMapData.getCollisionList(self);
		if (colliding.isEmpty()) return;
		AABB selfBox = self.getBoundingBox();
		for (Entity other : colliding) {
			if (other != null && !other.isPassenger() && selfBox.intersects(other.getBoundingBox())) {
				self.push(other);
			}
		}
	}
}
