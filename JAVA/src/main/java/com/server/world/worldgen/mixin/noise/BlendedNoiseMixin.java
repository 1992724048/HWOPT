package com.server.world.worldgen.mixin.noise;

import com.google.common.annotations.VisibleForTesting;
import library.dll.BlendedNoiseNative;
import library.dll.PerlinNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Locale;

@Mixin(BlendedNoise.class)
public abstract class BlendedNoiseMixin {
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
	private double xzScale;
	@Shadow
	@Final
	private double yScale;
	@Shadow
	@Final
	private double xzFactor;
	@Shadow
	@Final
	private double yFactor;
	@Shadow
	@Final
	private double smearScaleMultiplier;
	@Shadow
	@Final
	private double xzMultiplier;
	@Shadow
	@Final
	private double yMultiplier;
	@Shadow
	@Final
	private PerlinNoise mainNoise;
	@Shadow
	@Final
	private PerlinNoise minLimitNoise;
	@Shadow
	@Final
	private PerlinNoise maxLimitNoise;
	@Shadow
	@Final
	private double maxValue;
	@Shadow
	@Final
	public static KeyDispatchDataCodec<BlendedNoise> CODEC;
	
	@Unique
	private BlendedNoiseNative hwopt$nativePtr;
	
	@Overwrite
	public static BlendedNoise createUnseeded(double xzScale, double yScale, double xzFactor, double yFactor, double smearScaleMultiplier) {
		return new BlendedNoise(new XoroshiroRandomSource(0L), xzScale, yScale, xzFactor, yFactor, smearScaleMultiplier);
	}
	
	@Overwrite
	public BlendedNoise withNewRandom(RandomSource terrainRandom) {
		return new BlendedNoise(terrainRandom, this.xzScale, this.yScale, this.xzFactor, this.yFactor, this.smearScaleMultiplier);
	}
	
	@Inject(method = "<init>(Lnet/minecraft/world/level/levelgen/synth/PerlinNoise;Lnet/minecraft/world/level/levelgen/synth/PerlinNoise;Lnet/minecraft/world/level/levelgen/synth/PerlinNoise;DDDDD)V", at = @At("RETURN"))
	private void hwopt$init(PerlinNoise minLimitNoise, PerlinNoise maxLimitNoise, PerlinNoise mainNoise, double xzScale, double yScale, double xzFactor, double yFactor, double smearScaleMultiplier, CallbackInfo ci) {
		this.hwopt$nativePtr = FFMFactory.trackCleaner(this, BlendedNoiseNative.instance().create((PerlinNoiseNative) HWOPT_PERLIN_NATIVE.get(minLimitNoise), (PerlinNoiseNative) HWOPT_PERLIN_NATIVE.get(maxLimitNoise), (PerlinNoiseNative) HWOPT_PERLIN_NATIVE.get(mainNoise), xzScale, yScale, xzFactor, yFactor, smearScaleMultiplier));
	}
	
	@Inject(method = "compute", at = @At("HEAD"), cancellable = true)
	public void compute(DensityFunction.FunctionContext context, CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.compute(context.blockX(), context.blockY(), context.blockZ()));
		}
	}
	
	@Inject(method = "maxValue", at = @At("HEAD"), cancellable = true)
	public void maxValue(CallbackInfoReturnable<Double> cir) {
		if (this.hwopt$nativePtr != null) {
			cir.setReturnValue(this.hwopt$nativePtr.max_value());
		}
	}
	
	@Overwrite
	@VisibleForTesting
	public void parityConfigString(StringBuilder sb) {
		sb.append("BlendedNoise{minLimitNoise=");
		this.minLimitNoise.parityConfigString(sb);
		sb.append(", maxLimitNoise=");
		this.maxLimitNoise.parityConfigString(sb);
		sb.append(", mainNoise=");
		this.mainNoise.parityConfigString(sb);
		sb.append(String.format(Locale.ROOT, ", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=4, cellHeight=8", 684.412, 684.412, 8.555150000000001, 4.277575000000001)).append('}');
	}
	
	@Overwrite
	public KeyDispatchDataCodec<? extends DensityFunction> codec() {
		return CODEC;
	}
}
