package com.worldgen.mixin;

import com.hwpp.mod.HWOPT;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import library.dll.PerlinNoiseNative;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Cleaner;

import static library.dll.PerlinNoiseNative.NATIVE;

@Mixin(PerlinNoise.class)
public abstract class PerlinNoiseMixin implements AutoCloseable {
    
    @Unique
    private PerlinNoiseNative hwopt$nativePtr;
    @Unique
    private Cleaner.Cleanable hwopt$cleanable;
    @Unique
    private static final Cleaner hwopt$CLEANER = Cleaner.create();
    
    @Override
    public void close() {
        if (null != this.hwopt$nativePtr) {
            this.hwopt$nativePtr.destroy();
            this.hwopt$nativePtr = null;
            
            if (null != this.hwopt$cleanable) {
                this.hwopt$cleanable.clean();
            }
        }
    }
    
    @Inject(method = "<init>", at = @At("TAIL"))
    private void hwopt$init(final RandomSource random, final Pair<Integer, DoubleList> pair,
                            final boolean useNewInitialization,
                            final CallbackInfo ci) {
        this.hwopt$nativePtr = NATIVE.create(
                HWOPT.seed,
                pair.getFirst(),
                pair.getSecond().toDoubleArray(),
                pair.getSecond().size(),
                useNewInitialization
        );
        final PerlinNoiseNative capturedPtr = this.hwopt$nativePtr;
        this.hwopt$cleanable = hwopt$CLEANER.register(this, () -> {
            if (null != capturedPtr) {
                capturedPtr.destroy();
            }
        });
    }
    
    @Inject(method = "getValue(DDD)D", at = @At("HEAD"), cancellable = true)
    private void getValue(final double x, final double y, final double z, final CallbackInfoReturnable<Double> cir) {
        if (null == this.hwopt$nativePtr) {
            return;
        }
        
        cir.setReturnValue(this.hwopt$nativePtr.getValue(x, y, z));
        cir.cancel();
    }
    
    @Inject(method = "getValue(DDDDDZ)D", at = @At("HEAD"), cancellable = true)
    private void getValue(final double x, final double y, final double z, final double yScale, final double yFudge,
                          final boolean yFlatHack,
                          final CallbackInfoReturnable<Double> cir) {
        if (null == this.hwopt$nativePtr) {
            return;
        }
        
        cir.setReturnValue(
                this.hwopt$nativePtr.getValue(x, y, z, yScale, yFudge, yFlatHack)
        );
        cir.cancel();
    }
    
    @Inject(method = "edgeValue", at = @At("HEAD"), cancellable = true)
    private void edgeValue(final double noiseValue, final CallbackInfoReturnable<Double> cir) {
        if (null == this.hwopt$nativePtr) {
            return;
        }
        
        cir.setReturnValue(this.hwopt$nativePtr.edgeValue(noiseValue));
        cir.cancel();
    }
    
    @Inject(method = "firstOctave", at = @At("HEAD"), cancellable = true)
    private void firstOctave(final CallbackInfoReturnable<Integer> cir) {
        if (null == this.hwopt$nativePtr) {
            return;
        }
        
        cir.setReturnValue(this.hwopt$nativePtr.first_octave());
        cir.cancel();
    }
    
    @Inject(method = "maxValue", at = @At("HEAD"), cancellable = true)
    private void maxValue(final CallbackInfoReturnable<Double> cir) {
        if (null == this.hwopt$nativePtr) {
            return;
        }
        
        cir.setReturnValue(this.hwopt$nativePtr.max_value());
        cir.cancel();
    }
    
    @Inject(method = "amplitudes", at = @At("HEAD"), cancellable = true)
    private void amplitudes(final CallbackInfoReturnable<DoubleList> cir) {
        if (null == this.hwopt$nativePtr) {
            return;
        }
        try (final Arena arena = Arena.ofConfined()) {
            final int size = this.hwopt$nativePtr.amplitudesSize();
            final MemorySegment segment = arena.allocate(ValueLayout.JAVA_DOUBLE, size);
            final int actualSize = this.hwopt$nativePtr.amplitudes(segment, size);
            final double[] array = segment.asSlice(0, (long) actualSize * Double.BYTES).toArray(
                    ValueLayout.JAVA_DOUBLE);
            cir.setReturnValue(DoubleArrayList.wrap(array));
            cir.cancel();
        }
    }
}
