package com.server.entity.mixin;

import com.server.entity.util.CollisionMapData;
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
	private void hwopt$onGetEntities(Entity entity, AABB aabb, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
		if (entity == null) return;
		List<Entity> cached = CollisionMapData.getCollisionList(entity);
		if (cached.isEmpty()) return;
		List<Entity> filtered = new ArrayList<>(cached.size());
		for (Entity e : cached) {
			if (e != null && predicate.test(e)) filtered.add(e);
		}
		cir.setReturnValue(filtered);
	}
}