package com.worldgen.mixin;

import library.dll.NormalNoiseNative;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NormalNoise.class)
public abstract class NormalNoiseMixin {

	@Shadow @Final
	private PerlinNoise first;

	@Shadow @Final
	private PerlinNoise second;

	@Unique
	private NormalNoiseNative hwopt$nativePtr;
	
	@Inject(method = "<init>", at = @At("RETURN"))
	private void hwopt$init(CallbackInfo ci) {
	
	}

	@Inject(method = "parityConfigString", at = @At("HEAD"), cancellable = true)
	private void hwopt$parityConfigString(StringBuilder sb, CallbackInfo ci) {
		if (this.hwopt$nativePtr != null) {
			ci.cancel();
		}
	}

	@Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
	private void hwopt$getValue(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.getValue(x, y, z));
		}
	}

	@Inject(method = "maxValue", at = @At("HEAD"), cancellable = true)
	private void hwopt$maxValue(CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.maxValue());
		}
	}
}
