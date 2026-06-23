package com.server.render.particle.mixin;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.server.render.particle.core.AsyncTickBehavior;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
	static {
		AsyncTickBehavior.getInstance().setEnabled(true);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void hwopt$onTick(CallbackInfo ci) {
		AsyncTickBehavior async = AsyncTickBehavior.getInstance();
		if (async.isEnabled()) {
			async.preTick();
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void hwopt$afterTick(CallbackInfo ci) {
		AsyncTickBehavior async = AsyncTickBehavior.getInstance();
		if (async.isEnabled()) {
			async.postTick();
		}
	}
}
