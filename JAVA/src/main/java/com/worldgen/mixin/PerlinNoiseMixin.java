package com.worldgen.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import library.dll.PerlinNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PerlinNoise.class)
public abstract class PerlinNoiseMixin {
    @Unique
    private static final java.lang.ref.Cleaner hwopt$CLEANER = java.lang.ref.Cleaner.create();

    @Unique
    private java.lang.ref.Cleaner.Cleanable hwopt$cleanable;

    @Unique
    private long hwopt$nativePtr;

    @Unique
    private static final PerlinNoiseNative hwopt$NATIVE = FFMFactory.load(PerlinNoiseNative.class);

    @Inject(method = "<init>", require = 1, at = @At("TAIL"))
    private void hwopt$init(RandomSource random, Pair<Integer, DoubleList> pair, boolean useNewInitialization, CallbackInfo ci) {
        this.hwopt$nativePtr = hwopt$NATIVE.create(pair.getFirst(), pair.getSecond().toDoubleArray(), pair.getSecond().size(), useNewInitialization);
        this.hwopt$cleanable = hwopt$CLEANER.register(this, () -> {
            hwopt$NATIVE.destroy(hwopt$nativePtr);
        });
    }

    @Inject(method = "getValue(DDD)D", at = @At("HEAD"), cancellable = true)
    private void getValue(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
        if (this.hwopt$nativePtr == 0) {
            return;
        }

        cir.setReturnValue(hwopt$NATIVE.getValue(hwopt$nativePtr, x, y, z));
        cir.cancel();
    }

    @Inject(method = "getValue(DDDDDZ)D", at = @At("HEAD"), cancellable = true)
    private void getValue(double x, double y, double z, double yScale, double yFudge, boolean yFlatHack, CallbackInfoReturnable<Double> cir) {
        if (this.hwopt$nativePtr == 0) {
            return;
        }

        cir.setReturnValue(hwopt$NATIVE.getValue(hwopt$nativePtr, x, y, z, yScale, yFudge, yFlatHack));
        cir.cancel();
    }

    @Inject(method = "edgeValue", at = @At("HEAD"), cancellable = true)
    private void edgeValue(double noiseValue, CallbackInfoReturnable<Double> cir) {
        if (this.hwopt$nativePtr == 0) {
            return;
        }

        cir.setReturnValue(hwopt$NATIVE.edgeValue(hwopt$nativePtr, noiseValue));
        cir.cancel();
    }

    @Inject(method = "firstOctave", at = @At("HEAD"), cancellable = true)
    private void firstOctave(CallbackInfoReturnable<Integer> cir) {
        if (this.hwopt$nativePtr == 0) {
            return;
        }

        cir.setReturnValue(hwopt$NATIVE.firstOctave(hwopt$nativePtr));
        cir.cancel();
    }

    @Inject(method = "maxValue", at = @At("HEAD"), cancellable = true)
    private void maxValue(CallbackInfoReturnable<Double> cir) {
        if (this.hwopt$nativePtr == 0) {
            return;
        }

        cir.setReturnValue(hwopt$NATIVE.maxValue(hwopt$nativePtr));
        cir.cancel();
    }

    @Inject(method = "amplitudes", at = @At("HEAD"), cancellable = true)
    private void amplitudes(CallbackInfoReturnable<DoubleList> cir) {
        if (this.hwopt$nativePtr == 0) {
            return;
        }

        int size = hwopt$NATIVE.amplitudesSize(hwopt$nativePtr);
        double[] amplitudes = new double[size];
        int relSize = hwopt$NATIVE.amplitudes(hwopt$nativePtr, amplitudes, size);
        DoubleList list = new DoubleArrayList(relSize);
        for (int i = 0; i < relSize; i++) {
            list.add(amplitudes[i]);
        }
        cir.setReturnValue(list);
        cir.cancel();
    }
}
