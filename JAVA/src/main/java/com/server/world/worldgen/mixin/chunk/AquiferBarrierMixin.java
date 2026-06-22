package com.server.world.worldgen.mixin.chunk;

import com.server.world.worldgen.accessor.NoiseChunkAccessor;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Aquifer.NoiseBasedAquifer.class)
public abstract class AquiferBarrierMixin {
	
	@Shadow
	@Final
	private NoiseChunk noiseChunk;
	
	@Shadow
	@Final
	protected DensityFunction barrierNoise;
	
	@Unique
	private double[] hwopt$barrierCache;
	@Unique
	private long hwopt$lastCellKey = Long.MIN_VALUE;
	
	@Unique
	private void hwopt$fillCache() {
		NoiseChunkAccessor acc = (NoiseChunkAccessor) this.noiseChunk;
		int cellW = acc.invokeCellWidth();
		int cellH = acc.invokeCellHeight();
		int size = cellW * cellH * cellW;
		if (hwopt$barrierCache == null || hwopt$barrierCache.length != size) {
			hwopt$barrierCache = new double[size];
		}
		this.barrierNoise.fillArray(hwopt$barrierCache, this.noiseChunk);
	}
	
	@Inject(method = "calculatePressure(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;Lorg/apache/commons/lang3/mutable/MutableDouble;Lnet/minecraft/world/level/levelgen/Aquifer$FluidStatus;Lnet/minecraft/world/level/levelgen/Aquifer$FluidStatus;)D", at = @At("HEAD"), cancellable = true)
	private void hwopt$cachedCalculatePressure(DensityFunction.FunctionContext ctx, org.apache.commons.lang3.mutable.MutableDouble bnv, Aquifer.FluidStatus s1, Aquifer.FluidStatus s2, CallbackInfoReturnable<Double> cir) {
		if (hwopt$barrierCache == null) return;

		NoiseChunkAccessor acc = (NoiseChunkAccessor) this.noiseChunk;
		int blockX = ctx.blockX();
		int blockY = ctx.blockY();
		int blockZ = ctx.blockZ();
		int sx = acc.cellStartBlockX();
		int sy = acc.cellStartBlockY();
		int sz = acc.cellStartBlockZ();
		int cellW = acc.invokeCellWidth();
		int cellH = acc.invokeCellHeight();

		int ix = blockX - sx;
		int iy = blockY - sy;
		int iz = blockZ - sz;
		if (ix < 0 || ix >= cellW || iy < 0 || iy >= cellH || iz < 0 || iz >= cellW) return;

		long key = (long) sx << 42 | (long) sy << 21 | sz;
		if (key != hwopt$lastCellKey) {
			hwopt$lastCellKey = key;
			hwopt$fillCache();
		}

		int idx = ((cellH - 1 - iy) * cellW + ix) * cellW + iz;
		double cachedNoise = hwopt$barrierCache[idx];
		bnv.setValue(cachedNoise);

		int fluidYDiff = Math.abs(s1.fluidLevel() - s2.fluidLevel());
		if (fluidYDiff == 0) { cir.setReturnValue(0.0); return; }

		double posY = blockY + 0.5;
		double avgFluidY = (s1.fluidLevel() + s2.fluidLevel()) * 0.5;
		double aboveAvg = posY - avgFluidY;
		double base = fluidYDiff / 2.0;
		double dist = base - Math.abs(aboveAvg);
		double grad;
		if (aboveAvg > 0.0) {
			grad = dist > 0.0 ? dist / 1.5 : dist / 2.5;
		} else {
			double cp = 3.0 + dist;
			grad = cp > 0.0 ? cp / 3.0 : cp / 10.0;
		}

		if (grad < -2.0 || grad > 2.0) {
			cir.setReturnValue(2.0 * grad);
		} else {
			cir.setReturnValue(2.0 * (cachedNoise + grad));
		}
	}
}
