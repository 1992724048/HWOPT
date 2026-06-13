package com.worldgen.mixin;

import com.hwpp.mod.HWOPT;
import library.dll.NormalNoiseNative;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.Cleaner;

import static library.dll.NormalNoiseNative.NATIVE;

@Mixin(NormalNoise.class)
public abstract class NormalNoiseMixin implements AutoCloseable {
	@Unique
	private NormalNoiseNative hwopt$nativePtr;
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
	private void hwopt$init(RandomSource random, NormalNoise.NoiseParameters parameters, boolean useNewInitialization, CallbackInfo ci) {
		this.hwopt$nativePtr = NATIVE.create(HWOPT.seed, parameters.firstOctave(), parameters.amplitudes().toDoubleArray(), parameters.amplitudes().size(), useNewInitialization);
		final NormalNoiseNative capturedPtr = this.hwopt$nativePtr;
		this.hwopt$cleanable = hwopt$CLEANER.register(this, () -> {
			if (null != capturedPtr) {
				capturedPtr.destroy();
			}
		});
	}
	
	@Overwrite
	public double getValue(double x, double y, double z) {
		if (null == this.hwopt$nativePtr) {
			return 0.0;
		}
		return hwopt$nativePtr.getValue(x, y, z);
	}
	
	@Overwrite
	public double maxValue() {
		if (null == this.hwopt$nativePtr) {
			return 0.0;
		}
		return hwopt$nativePtr.maxValue();
	}
}
