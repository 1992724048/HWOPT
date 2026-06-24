package com.server.render.particle.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.server.render.particle.addon.ParticleEngineAddon;
import com.server.render.particle.core.AsyncTickBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Inject(method = "runTick", at = @At("HEAD"))
	private void hwopt$onRunTickHead(CallbackInfo ci) {
		AsyncTickBehavior.getInstance().onRunTick(Minecraft.getInstance().isPaused());
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
	private void hwopt$onPreTick(boolean advanceGameTime, CallbackInfo ci,
		@Local(ordinal = 0) int ticksToDo, @Local(ordinal = 1) int i) {
		AsyncTickBehavior.getInstance().preTick(i == 0, i == ticksToDo - 1);
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V", shift = At.Shift.AFTER))
	private void hwopt$onPostTick(boolean advanceGameTime, CallbackInfo ci) {
		AsyncTickBehavior.getInstance().postTick();
	}

	@Inject(method = "setLevel", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
		target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;"))
	private void hwopt$onSetLevel(CallbackInfo ci) {
		AsyncTickBehavior.getInstance().reset();
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
	private void hwopt$redirectParticleEngineTick(ParticleEngine instance) {
		if (AsyncTickBehavior.getInstance().isEnabled()) {
			((ParticleEngineAddon) instance).asyncparticle$tickSyncParticles();
		} else {
			instance.tick();
		}
	}
}
