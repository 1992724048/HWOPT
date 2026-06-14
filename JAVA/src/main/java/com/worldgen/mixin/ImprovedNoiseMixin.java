package com.worldgen.mixin;

import com.google.common.annotations.VisibleForTesting;
import library.dll.ImprovedNoiseNative;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.NoiseUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.Cleaner;

import static library.dll.ImprovedNoiseNative.NATIVE;

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
	@Unique
	private Cleaner.Cleanable hwopt$cleanable;
	@Unique
	private static final Cleaner hwopt$CLEANER = Cleaner.create();
	
	@Inject(method = "<init>", at = @At("RETURN"))
	private void hwopt$init(RandomSource random, CallbackInfo ci) {
		this.hwopt$nativePtr = NATIVE.create(xo, yo, zo, p, p.length);
		
		final ImprovedNoiseNative capturedPtr = this.hwopt$nativePtr;
		this.hwopt$cleanable = hwopt$CLEANER.register(this, () -> {
			if (null != capturedPtr) {
				capturedPtr.destroy();
			}
		});
	}
	
	@Overwrite
	public double noise(double _x, double _y, double _z) {
		return this.noise(_x, _y, _z, 0.0, 0.0);
	}
	
	@Overwrite
	@Deprecated
	public double noise(double _x, double _y, double _z, double yScale, double yFudge) {
		return this.hwopt$nativePtr.noise(_x, _y, _z, yScale, yFudge);
	}
	
	@Overwrite
	public double noiseWithDerivative(double _x, double _y, double _z, double[] derivativeOut) {
		return this.hwopt$nativePtr.noise_with_derivative(_x, _y, _z, derivativeOut);
	}
	
	@Overwrite
	private static double gradDot(int hash, double x, double y, double z) {
		return NATIVE.grad_dot(hash, x, y, z);
	}
	
	@Overwrite
	private int p(int x) {
		return this.hwopt$nativePtr.perm(x);
	}
	
	@Overwrite
	private double sampleAndLerp(int x, int y, int z, double xr, double yr, double zr, double yrOriginal) {
		return this.hwopt$nativePtr.sample_and_lerperm(x, y, z, xr, yr, zr, yrOriginal);
	}
	
	@Overwrite
	private double sampleWithDerivative(int x, int y, int z, double xr, double yr, double zr, double[] derivativeOut) {
		return this.hwopt$nativePtr.sample_with_derivative(x, y, z, xr, yr, zr, derivativeOut);
	}
	
	@Overwrite
	@VisibleForTesting
	public void parityConfigString(StringBuilder sb) {
		NoiseUtils.parityNoiseOctaveConfigString(sb, this.xo, this.yo, this.zo, this.p);
	}
}
