package com.worldgen.mixin.noise;

import library.dll.ImprovedNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.NoiseUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ImprovedNoise.class)
public abstract class ImprovedNoiseMixin {

	@Shadow
	@Final
	public double xo;
	@Shadow
	@Final
	public double yo;
	@Shadow
	@Final
	public double zo;
	@Shadow
	@Final
	private byte[] p;

	@Unique
	private ImprovedNoiseNative hwopt$nativePtr;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void hwopt$init(RandomSource random, CallbackInfo ci) {
		this.hwopt$nativePtr = FFMFactory.trackCleaner(this, ImprovedNoiseNative.create(xo, yo, zo, p));
	}

	@Inject(method = "noise(DDD)D", at = @At("HEAD"), cancellable = true)
	private void hwopt$noise(double _x, double _y, double _z, CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(hwopt$nativePtr.noise(_x, _y, _z));
		}
	}

	@Inject(method = "noise(DDDDD)D", at = @At("HEAD"), cancellable = true)
	private void hwopt$noise(double _x, double _y, double _z, double yScale, double yFudge, CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.noise(_x, _y, _z, yScale, yFudge));
		}
	}

	@Inject(method = "noiseWithDerivative", at = @At("HEAD"), cancellable = true)
	private void hwopt$noiseWithDerivative(double _x, double _y, double _z, double[] derivativeOut, CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.noise_with_derivative(_x, _y, _z, derivativeOut));
		}
	}

	@Inject(method = "gradDot", at = @At("HEAD"), cancellable = true)
	private static void hwopt$gradDot(int hash, double x, double y, double z, CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(ImprovedNoiseNative.instance().grad_dot(hash, x, y, z));
	}

	@Inject(method = "p(I)I", at = @At("HEAD"), cancellable = true)
	private void hwopt$p(int x, CallbackInfoReturnable<Integer> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.perm(x));
		}
	}

	@Inject(method = "sampleAndLerp", at = @At("HEAD"), cancellable = true)
	private void hwopt$sampleAndLerp(int x, int y, int z, double xr, double yr, double zr, double yrOriginal, CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.sample_and_lerperm(x, y, z, xr, yr, zr, yrOriginal));
		}
	}

	@Inject(method = "sampleWithDerivative", at = @At("HEAD"), cancellable = true)
	private void hwopt$sampleWithDerivative(int x, int y, int z, double xr, double yr, double zr, double[] derivativeOut, CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.sample_with_derivative(x, y, z, xr, yr, zr, derivativeOut));
		}
	}

	@Inject(method = "parityConfigString", at = @At("HEAD"), cancellable = true)
	private void hwopt$parityConfigString(StringBuilder sb, CallbackInfo ci) {
		if (this.hwopt$nativePtr != null) {
			NoiseUtils.parityNoiseOctaveConfigString(sb, this.xo, this.yo, this.zo, this.p);
			ci.cancel();
		}
	}
}
