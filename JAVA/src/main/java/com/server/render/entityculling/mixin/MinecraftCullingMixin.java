package com.server.render.entityculling.mixin;

import com.server.render.entityculling.EntityCullingMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftCullingMixin {
	static {
		new EntityCullingMod();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void hwopt$onTick(CallbackInfo ci) {
		EntityCullingMod.getInstance().clientTick();
	}
}
