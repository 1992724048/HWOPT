package com.hwpp.noise;

import com.server.world.worldgen.accessor.NoiseChunkAccessor;
import com.server.world.worldgen.accessor.NoiseInterpolatorAccessor;
import com.server.world.worldgen.mixin.chunk.NoiseChunkMixin;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;

public class CornerPrecomputer {

	public static void precompute(NoiseChunk noiseChunk) {
		NoiseChunkAccessor acc = (NoiseChunkAccessor) noiseChunk;
		int cellW = acc.invokeCellWidth();
		int cellH = acc.invokeCellHeight();
		int firstX = acc.firstCellX();
		int firstZ = acc.firstCellZ();
		int noiseMinY = acc.cellNoiseMinY();
		int numZ = acc.cellCountXZ() + 1;
		int numY = acc.cellCountY() + 1;
		int numX = acc.cellCountXZ() + 2;

		double[] corners = new double[numX * numZ * numY];
		int idx = 0;
		for (int cx = 0; cx < numX; cx++) {
			int wx = (firstX + cx) * cellW;
			for (int cz = 0; cz < numZ; cz++) {
				int wz = (firstZ + cz) * cellW;
				for (int cy = 0; cy < numY; cy++) {
					int wy = (cy + noiseMinY) * cellH;
					Object interp = acc.invokeInterpolators().get(0);
					DensityFunction df = ((NoiseInterpolatorAccessor) interp).noiseFiller();
					corners[idx] = df.compute(new SimplePos(wx, wy, wz));
					idx++;
				}
			}
		}

		NoiseChunkMixin.hwopt$setPrecomputedCache(corners);
	}

	private record SimplePos(int x, int y, int z) implements DensityFunction.FunctionContext {
		@Override public int blockX() { return x; }
		@Override public int blockY() { return y; }
		@Override public int blockZ() { return z; }
	}
}
