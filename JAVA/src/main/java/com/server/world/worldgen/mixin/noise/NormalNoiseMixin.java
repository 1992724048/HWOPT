package com.server.world.worldgen.mixin.noise;

import library.dll.NormalNoiseNative;
import library.dll.PerlinNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@Mixin(NormalNoise.class)
public abstract class NormalNoiseMixin {
	
	@Unique
	private static final VarHandle HWOPT_PERLIN_NATIVE;
	
	static {
		try {
			var lookup = MethodHandles.privateLookupIn(PerlinNoise.class, MethodHandles.lookup());
			HWOPT_PERLIN_NATIVE = lookup.findVarHandle(PerlinNoise.class, "hwopt$nativePtr", PerlinNoiseNative.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Shadow
	@Final
	private PerlinNoise first;
	@Shadow
	@Final
	private PerlinNoise second;
	@Shadow
	@Final
	private double valueFactor;
	@Shadow
	@Final
	private double maxValue;
	
	@Unique
	private NormalNoiseNative hwopt$nativePtr;
	
	@Inject(method = "<init>", at = @At("RETURN"))
	private void hwopt$init(CallbackInfo ci) {
		if (first == null || second == null) return;
		hwopt$nativePtr = FFMFactory.trackCleaner(this, NormalNoiseNative.instance().create(valueFactor, maxValue));
		PerlinNoiseNative firstNative = (PerlinNoiseNative) HWOPT_PERLIN_NATIVE.get(first);
		PerlinNoiseNative secondNative = (PerlinNoiseNative) HWOPT_PERLIN_NATIVE.get(second);
		hwopt$nativePtr.setPerlinNoise(firstNative, secondNative);
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
	
	@Overwrite
	private static double expectedDeviation(int octaveSpan) {
		return NormalNoiseNative.instance().expected_deviation(octaveSpan);
	}
	
	@Inject(method = "parityConfigString", at = @At("HEAD"), cancellable = true)
	private void hwopt$parityConfigString(StringBuilder sb, CallbackInfo ci) {
		if (this.hwopt$nativePtr != null) {
			ci.cancel();
		}
	}
}
