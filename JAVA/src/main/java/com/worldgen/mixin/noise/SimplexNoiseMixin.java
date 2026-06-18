package com.worldgen.mixin.noise;

import library.dll.SimplexNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimplexNoise.class)
public abstract class SimplexNoiseMixin {

	@Shadow
	@Final
	private int[] p;
	@Shadow
	@Final
	public double xo;
	@Shadow
	@Final
	public double yo;
	@Shadow
	@Final
	public double zo;

	@Unique
	private SimplexNoiseNative hwopt$nativePtr;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void hwopt$init(final RandomSource random, final CallbackInfo ci) {
		this.hwopt$nativePtr = FFMFactory.trackCleaner(this, SimplexNoiseNative.instance().create(this.xo, this.yo, this.zo, p));
	}

	@Inject(method = "getValue(DD)D", at = @At("HEAD"), cancellable = true)
	private void hwopt$getValue(final double xin, final double yin, final CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.getValue2(xin, yin));
		}
	}

	@Inject(method = "getValue(DDD)D", at = @At("HEAD"), cancellable = true)
	private void hwopt$getValue(final double xin, final double yin, final double zin, final CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.getValue(xin, yin, zin));
		}
	}
}
