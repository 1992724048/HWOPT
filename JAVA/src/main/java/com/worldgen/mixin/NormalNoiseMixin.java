package com.worldgen.mixin;

import com.hwpp.mod.HWOPT;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import library.dll.NormalNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static library.dll.NormalNoiseNative.NATIVE;

@Mixin(NormalNoise.class)
public abstract class NormalNoiseMixin {

	@Unique
	private NormalNoiseNative hwopt$nativePtr;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void hwopt$init(RandomSource random, NormalNoise.NoiseParameters parameters, boolean useNewInitialization, CallbackInfo ci) {
		this.hwopt$nativePtr = FFMFactory.trackCleaner(this, NATIVE.create(HWOPT.seed, parameters.firstOctave(), parameters.amplitudes().toDoubleArray(), parameters.amplitudes().size(), useNewInitialization));
	}

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/synth/PerlinNoise;create(Lnet/minecraft/util/RandomSource;ILit/unimi/dsi/fastutil/doubles/DoubleList;)Lnet/minecraft/world/level/levelgen/synth/PerlinNoise;"))
	private PerlinNoise hwopt$redirectCreate(RandomSource random, int firstOctave, DoubleList amplitudes) {
		return null;
	}

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/synth/PerlinNoise;createLegacyForLegacyNetherBiome(Lnet/minecraft/util/RandomSource;ILit/unimi/dsi/fastutil/doubles/DoubleList;)Lnet/minecraft/world/level/levelgen/synth/PerlinNoise;"))
	private PerlinNoise hwopt$redirectCreateLegacy(RandomSource random, int firstOctave, DoubleList amplitudes) {
		return null;
	}

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/synth/PerlinNoise;maxValue()D"))
	private double hwopt$redirectMaxValue(PerlinNoise instance) {
		return 0.0;
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
