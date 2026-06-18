package com.worldgen.mixin.noise;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import library.dll.ImprovedNoiseNative;
import library.dll.PerlinNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@Mixin(PerlinNoise.class)
public abstract class PerlinNoiseMixin {
	
	private static final VarHandle HWOPT_P;
	private static final VarHandle HWOPT_IMPROVED_NOISE_NATIVE;
	
	static {
		try {
			var improvedLookup = MethodHandles.privateLookupIn(ImprovedNoise.class, MethodHandles.lookup());
			HWOPT_P = improvedLookup.findVarHandle(ImprovedNoise.class, "p", byte[].class);
			HWOPT_IMPROVED_NOISE_NATIVE = improvedLookup.findVarHandle(ImprovedNoise.class, "hwopt$nativePtr", ImprovedNoiseNative.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Shadow
	@Final
	private ImprovedNoise[] noiseLevels;
	
	@Shadow
	@Final
	private int firstOctave;
	
	@Shadow
	@Final
	private DoubleList amplitudes;
	
	@Shadow
	@Final
	private double lowestFreqValueFactor;
	@Shadow
	@Final
	private double lowestFreqInputFactor;
	@Shadow
	@Final
	private double maxValue;
	
	@Shadow
	protected abstract double edgeValue(double noiseValue);
	
	@Unique
	private PerlinNoiseNative hwopt$nativePtr;
	
	@Inject(method = "<init>", at = @At("RETURN"))
	private void hwopt$init(final RandomSource random, final Pair<Integer, DoubleList> pair, final boolean useNewInitialization, final CallbackInfo ci) {
		this.hwopt$nativePtr = FFMFactory.trackCleaner(this, PerlinNoiseNative.instance().create(this.firstOctave, this.amplitudes.toDoubleArray(), this.lowestFreqValueFactor, this.lowestFreqInputFactor, this.maxValue));
		int index = 0;
		for (var noise : this.noiseLevels) {
			if (noise != null) {
				ImprovedNoiseNative noiseNative = (ImprovedNoiseNative) HWOPT_IMPROVED_NOISE_NATIVE.get(noise);
				this.hwopt$nativePtr.addNoise(index, noiseNative);
			}
			index++;
		}
	}
	
	@Inject(method = "getValue(DDD)D", at = @At("HEAD"), cancellable = true)
	private void hwopt$getValue(final double x, final double y, final double z, final CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			double a = this.hwopt$nativePtr.getValue(x, y, z);
			cir.setReturnValue(a);
		}
	}
	
	@Inject(method = "getValue(DDDDD)D", at = @At("HEAD"), cancellable = true)
	private void hwopt$getValue(double x, double y, double z, double yScale, double yFudge, final CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.getValue(x, y, z, yScale, yFudge));
		}
	}
	
	@Inject(method = "edgeValue", at = @At("HEAD"), cancellable = true)
	private void hwopt$edgeValue(final double noiseValue, final CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.edgeValue(noiseValue));
		}
	}
	
	@Inject(method = "firstOctave", at = @At("HEAD"), cancellable = true)
	private void hwopt$firstOctave(final CallbackInfoReturnable<Integer> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.first_octave());
		}
	}
	
	@Inject(method = "maxValue", at = @At("HEAD"), cancellable = true)
	private void hwopt$maxValue(final CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.max_value());
		}
	}
	
	@Inject(method = "maxBrokenValue", at = @At("HEAD"), cancellable = true)
	public void maxBrokenValue(double yScale, CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.max_broken_value(yScale));
		}
	}
	
	@Inject(method = "amplitudes", at = @At("HEAD"), cancellable = true)
	private void hwopt$amplitudes(final CallbackInfoReturnable<DoubleList> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(DoubleList.of(this.hwopt$nativePtr.amplitudes()));
		}
	}
	
	@Inject(method = "getOctaveNoise", at = @At("HEAD"), cancellable = true)
	private void hwopt$getOctaveNoise(int i, final CallbackInfoReturnable<ImprovedNoise> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(null);
		}
	}
	
	@Inject(method = "parityConfigString", at = @At("HEAD"), cancellable = true)
	private void hwopt$parityConfigString(StringBuilder sb, final CallbackInfo ci) {
		if (this.hwopt$nativePtr != null) {
			ci.cancel();
		}
	}
}
