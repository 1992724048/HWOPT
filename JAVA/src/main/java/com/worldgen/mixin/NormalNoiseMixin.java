package com.worldgen.mixin;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import library.dll.NormalNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
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
	
	private static final VarHandle HWOPT_FIRST_OCTAVE;
	private static final VarHandle HWOPT_AMPLITUDES;
	private static final VarHandle HWOPT_LOWEST_FREQ_VALUE_FACTOR;
	private static final VarHandle HWOPT_LOWEST_FREQ_INPUT_FACTOR;
	private static final VarHandle HWOPT_MAX_VALUE;
	private static final VarHandle HWOPT_NOISE_LEVELS;
	private static final VarHandle HWOPT_P;
	
	static {
		try {
			var lookup = MethodHandles.privateLookupIn(PerlinNoise.class, MethodHandles.lookup());
			HWOPT_FIRST_OCTAVE = lookup.findVarHandle(PerlinNoise.class, "firstOctave", int.class);
			HWOPT_AMPLITUDES = lookup.findVarHandle(PerlinNoise.class, "amplitudes", DoubleList.class);
			HWOPT_LOWEST_FREQ_VALUE_FACTOR = lookup.findVarHandle(PerlinNoise.class, "lowestFreqValueFactor", double.class);
			HWOPT_LOWEST_FREQ_INPUT_FACTOR = lookup.findVarHandle(PerlinNoise.class, "lowestFreqInputFactor", double.class);
			HWOPT_MAX_VALUE = lookup.findVarHandle(PerlinNoise.class, "maxValue", double.class);
			HWOPT_NOISE_LEVELS = lookup.findVarHandle(PerlinNoise.class, "noiseLevels", ImprovedNoise[].class);
			HWOPT_P = MethodHandles.privateLookupIn(ImprovedNoise.class, MethodHandles.lookup()).findVarHandle(ImprovedNoise.class, "p", byte[].class);
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
		hwopt$nativePtr = FFMFactory.trackCleaner(this, NormalNoiseNative.instance().create(valueFactor, maxValue));
		initPerlin(first, true);
		initPerlin(second, false);
	}
	
	@Unique
	private void initPerlin(PerlinNoise perlin, boolean isFirst) {
		int firstOctave = (int) HWOPT_FIRST_OCTAVE.get(perlin);
		DoubleList amplitudes = (DoubleList) HWOPT_AMPLITUDES.get(perlin);
		double lowestFreqValueFactor = (double) HWOPT_LOWEST_FREQ_VALUE_FACTOR.get(perlin);
		double lowestFreqInputFactor = (double) HWOPT_LOWEST_FREQ_INPUT_FACTOR.get(perlin);
		double maxValue = (double) HWOPT_MAX_VALUE.get(perlin);
		
		if (isFirst) {
			hwopt$nativePtr.setFirst(firstOctave, amplitudes.toDoubleArray(), lowestFreqValueFactor, lowestFreqInputFactor, maxValue);
		} else {
			hwopt$nativePtr.setSecond(firstOctave, amplitudes.toDoubleArray(), lowestFreqValueFactor, lowestFreqInputFactor, maxValue);
		}
		
		ImprovedNoise[] noiseLevels = (ImprovedNoise[]) HWOPT_NOISE_LEVELS.get(perlin);
		int index = 0;
		for (var noise : noiseLevels) {
			if (noise != null) {
				if (isFirst) {
					hwopt$nativePtr.addNoiseToFirst(index, noise.xo, noise.yo, noise.zo, (byte[]) HWOPT_P.get(noise));
				} else {
					hwopt$nativePtr.addNoiseToSecond(index, noise.xo, noise.yo, noise.zo, (byte[]) HWOPT_P.get(noise));
				}
			}
			index++;
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
