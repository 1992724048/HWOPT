package com.worldgen.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import library.dll.PerlinNoiseNative;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static library.dll.PerlinNoiseNative.NATIVE;
import static net.neoforged.neoforgespi.ILaunchContext.LOGGER;

@Mixin(PerlinNoise.class)
public abstract class PerlinNoiseMixin implements AutoCloseable {

    @Unique
    private PerlinNoiseNative hwopt$nativePtr;

    @Override
    public void close() {
        if (hwopt$nativePtr != null) {
            hwopt$nativePtr.destroy();
            hwopt$nativePtr = null;
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hwopt$init(RandomSource random, Pair<Integer, DoubleList> pair, boolean useNewInitialization, CallbackInfo ci) {
        hwopt$nativePtr =
                NATIVE.create(
                        pair.getFirst(),
                        pair.getSecond().toDoubleArray(),
                        pair.getSecond().size(),
                        useNewInitialization
                );

        LOGGER.debug("nextDouble: {}", random.nextDouble());
    }

    @Inject(method = "getValue(DDD)D", at = @At("HEAD"), cancellable = true)
    private void getValue(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
        if (hwopt$nativePtr == null) return;

        cir.setReturnValue(hwopt$nativePtr.getValue(x, y, z));
        cir.cancel();
    }

    @Inject(method = "getValue(DDDDDZ)D", at = @At("HEAD"), cancellable = true)
    private void getValue(double x, double y, double z, double yScale, double yFudge, boolean yFlatHack, CallbackInfoReturnable<Double> cir) {
        if (hwopt$nativePtr == null) return;

        cir.setReturnValue(
                hwopt$nativePtr.getValue(x, y, z, yScale, yFudge, yFlatHack)
        );
        cir.cancel();
    }

    @Inject(method = "edgeValue", at = @At("HEAD"), cancellable = true)
    private void edgeValue(double noiseValue, CallbackInfoReturnable<Double> cir) {
        if (hwopt$nativePtr == null) return;

        cir.setReturnValue(hwopt$nativePtr.edgeValue(noiseValue));
        cir.cancel();
    }

    @Inject(method = "firstOctave", at = @At("HEAD"), cancellable = true)
    private void firstOctave(CallbackInfoReturnable<Integer> cir) {
        if (hwopt$nativePtr == null) return;

        cir.setReturnValue(hwopt$nativePtr.firstOctave());
        cir.cancel();
    }

    @Inject(method = "maxValue", at = @At("HEAD"), cancellable = true)
    private void maxValue(CallbackInfoReturnable<Double> cir) {
        if (hwopt$nativePtr == null) return;

        cir.setReturnValue(hwopt$nativePtr.maxValue());
        cir.cancel();
    }

    @Inject(method = "amplitudes", at = @At("HEAD"), cancellable = true)
    private void amplitudes(CallbackInfoReturnable<DoubleList> cir) {
        if (hwopt$nativePtr == null) return;

        int size = hwopt$nativePtr.amplitudesSize();
        double[] amplitudes = new double[size];
        int relSize = hwopt$nativePtr.amplitudes(amplitudes, size);

        DoubleList list = new DoubleArrayList(relSize);
        for (int i = 0; i < relSize; i++) {
            list.add(amplitudes[i]);
        }

        cir.setReturnValue(list);
        cir.cancel();
    }
}
