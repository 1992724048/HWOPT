package com.worldgen.mixin;

import com.hwpp.mod.HWOPT;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import library.dll.PerlinNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static library.dll.PerlinNoiseNative.NATIVE;

@Mixin(PerlinNoise.class)
public abstract class PerlinNoiseMixin {

	@Unique
	private PerlinNoiseNative hwopt$nativePtr;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void hwopt$init(final RandomSource random, final Pair<Integer, DoubleList> pair, final boolean useNewInitialization, final CallbackInfo ci) {
		this.hwopt$nativePtr = FFMFactory.trackCleaner(this, NATIVE.create(HWOPT.seed, pair.getFirst(), pair.getSecond().toDoubleArray(), pair.getSecond().size(), useNewInitialization));
	}

	@ModifyVariable(method = "<init>", at = @At("HEAD"), index = 2, argsOnly = true)
	private static Pair<Integer, DoubleList> hwopt$zeroAmplitudes(final Pair<Integer, DoubleList> pair) {
		final DoubleList zeroed = new DoubleArrayList(new double[pair.getSecond().size()]);
		return Pair.of(pair.getFirst(), zeroed);
	}

	@Inject(method = "getValue(DDD)D", at = @At("HEAD"), cancellable = true)
	private void hwopt$getValue(final double x, final double y, final double z, final CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.getValue(x, y, z));
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

	@Inject(method = "amplitudes", at = @At("HEAD"), cancellable = true)
	private void hwopt$amplitudes(final CallbackInfoReturnable<DoubleList> cir) {
		if (this.hwopt$nativePtr != null) {
			try (final Arena arena = Arena.ofConfined()) {
				final int size = this.hwopt$nativePtr.amplitudesSize();
				final MemorySegment segment = arena.allocate(ValueLayout.JAVA_DOUBLE, size);
				final int actualSize = this.hwopt$nativePtr.amplitudes(segment, size);
				final double[] array = segment.asSlice(0, (long) actualSize * Double.BYTES).toArray(ValueLayout.JAVA_DOUBLE);
				cir.setReturnValue(DoubleArrayList.wrap(array));
			}
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
