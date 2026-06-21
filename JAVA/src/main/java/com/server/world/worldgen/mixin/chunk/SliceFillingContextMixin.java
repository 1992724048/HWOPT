package com.server.world.worldgen.mixin.chunk;

import com.server.world.worldgen.accessor.NoiseChunkAccessor;
import library.dll.BlendedNoiseNative;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@Mixin(targets = "net.minecraft.world.level.levelgen.NoiseChunk$1")
public abstract class SliceFillingContextMixin {

	@Shadow
	@Final
	NoiseChunk this$0;

	@Unique
	private static final VarHandle HWOPT_BLENDED_NATIVE;

	@Unique
	private static final ThreadLocal<double[]> TL_XS = ThreadLocal.withInitial(() -> new double[128]);
	@Unique
	private static final ThreadLocal<double[]> TL_YS = ThreadLocal.withInitial(() -> new double[128]);
	@Unique
	private static final ThreadLocal<double[]> TL_ZS = ThreadLocal.withInitial(() -> new double[128]);

	static {
		try {
			var lookup = MethodHandles.privateLookupIn(BlendedNoise.class, MethodHandles.lookup());
			HWOPT_BLENDED_NATIVE = lookup.findVarHandle(BlendedNoise.class, "hwopt$nativePtr", BlendedNoiseNative.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Inject(method = "fillAllDirectly", at = @At("HEAD"), cancellable = true)
	private void hwopt$fillAllDirectly(double[] output, DensityFunction function, CallbackInfo ci) {
		if (!(function instanceof BlendedNoise bn)) return;
		BlendedNoiseNative nativePtr = (BlendedNoiseNative) HWOPT_BLENDED_NATIVE.get(bn);
		if (nativePtr == null) return;

		NoiseChunkAccessor acc = (NoiseChunkAccessor) this$0;
		int cellH = acc.invokeCellHeight();
		int noiseMinY = acc.cellNoiseMinY();
		int startX = acc.cellStartBlockX();
		int startZ = acc.cellStartBlockZ();

		int n = output.length;
		double[] xs = TL_XS.get();
		double[] ys = TL_YS.get();
		double[] zs = TL_ZS.get();
		if (xs.length < n) {
			xs = new double[n];
			ys = new double[n];
			zs = new double[n];
			TL_XS.set(xs);
			TL_YS.set(ys);
			TL_ZS.set(zs);
		}

		java.util.Arrays.fill(xs, 0, n, startX);
		java.util.Arrays.fill(zs, 0, n, startZ);
		for (int i = 0; i < n; i++) {
			ys[i] = (i + noiseMinY) * cellH;
		}

		nativePtr.getValues(xs, ys, zs, output);
		ci.cancel();
	}
}
