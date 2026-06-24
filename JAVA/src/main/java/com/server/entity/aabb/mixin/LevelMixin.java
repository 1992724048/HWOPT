package com.server.entity.aabb.mixin;

import com.server.entity.aabb.util.CollisionMapData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(Level.class)
public abstract class LevelMixin {
	
	@Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
	private void hwopt$onGetEntities(Entity except, AABB bb, Predicate<? super Entity> selector, CallbackInfoReturnable<List<Entity>> cir) {
		if (except == null) return;
		List<Entity> cached = CollisionMapData.getCollisionList(except);
		if (cached.isEmpty()) return;
		List<Entity> filtered = new ArrayList<>(cached.size());
		for (Entity e : cached) {
			if (e != null && selector.test(e)) filtered.add(e);
		}
		cir.setReturnValue(filtered);
	}
}
