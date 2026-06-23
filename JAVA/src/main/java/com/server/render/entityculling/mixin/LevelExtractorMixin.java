package com.server.render.entityculling.mixin;

import com.server.render.entityculling.EntityCullingMod;
import com.server.render.entityculling.access.Cullable;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
	@Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
	private void hwopt$onExtractEntity(Entity entity, CallbackInfo ci) {
		if (!EntityCullingMod.getInstance().isEnabled()) return;
		Cullable cullable = (Cullable) entity;
		if (cullable.isCulled()) {
			ci.cancel();
		}
	}
}
