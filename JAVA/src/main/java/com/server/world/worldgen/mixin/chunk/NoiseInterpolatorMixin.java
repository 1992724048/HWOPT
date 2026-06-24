package com.server.world.worldgen.mixin.chunk;

import com.server.world.worldgen.accessor.NoiseChunkAccessor;
import library.dll.MathNative;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseChunk.NoiseInterpolator.class)
public abstract class NoiseInterpolatorMixin {
	
	@Shadow
	@Final
	NoiseChunk this$0;
	
	@Shadow
	private double noise000;
	@Shadow
	private double noise100;
	@Shadow
	private double noise010;
	@Shadow
	private double noise110;
	@Shadow
	private double noise001;
	@Shadow
	private double noise101;
	@Shadow
	private double noise011;
	@Shadow
	private double noise111;
	
	@Unique
	private double[] hwopt$trilerpCache;
	
	@Inject(method = "selectCellYZ(II)V", at = @At("TAIL"))
	private void hwopt$afterSelectCellYZ(int cellYIndex, int cellZIndex, CallbackInfo ci) {
		NoiseChunkAccessor acc = (NoiseChunkAccessor) this$0;
		int cellW = acc.invokeCellWidth();
		int cellH = acc.invokeCellHeight();
		int total = cellW * cellW * cellH;
		if (hwopt$trilerpCache == null || hwopt$trilerpCache.length != total) {
			hwopt$trilerpCache = new double[total];
		}
		MathNative.INSTANCE.batch_trilerp(noise000, noise100, noise010, noise110, noise001, noise101, noise011, noise111, cellW, cellH, hwopt$trilerpCache);
	}
	
	@Inject(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D", at = @At("HEAD"), cancellable = true)
	private void hwopt$onCompute(DensityFunction.FunctionContext context, CallbackInfoReturnable<Double> cir) {
		NoiseChunkAccessor acc = (NoiseChunkAccessor) this$0;
		if (context != this$0) return;
		if (!acc.interpolating()) return;
		if (!acc.fillingCell()) return;
		
		int cellW = acc.invokeCellWidth();
		int cellH = acc.invokeCellHeight();
		int idx = ((cellH - 1 - acc.inCellY()) * cellW + acc.inCellX()) * cellW + acc.inCellZ();
		if (idx >= 0 && idx < hwopt$trilerpCache.length) {
			cir.setReturnValue(hwopt$trilerpCache[idx]);
		}
	}
}
