package com.server.world.misc;

import com.server.world.worldgen.accessor.NoiseChunkAccessor;
import com.server.world.worldgen.accessor.NoiseInterpolatorAccessor;
import library.dll.DensityFunctionTree;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;

import com.hwpp.mod.HWOPT;

public class DensityPrecomputer {
	
	public static void precompute(NoiseChunk noiseChunk) {
		NoiseChunkAccessor acc = (NoiseChunkAccessor) noiseChunk;
		int cellW = acc.invokeCellWidth();
		int cellH = acc.invokeCellHeight();
		int minY = acc.cellNoiseMinY() * cellH;
		int height = acc.cellCountY() * cellH;
		int sizeX = (acc.cellCountXZ() + 1) * cellW;
		int sizeZ = sizeX;
		int sizeY = height;
		int total = sizeX * sizeY * sizeZ;
		
		DensityFunction df = ((NoiseInterpolatorAccessor) acc.invokeInterpolators().get(0)).noiseFiller();
		int minX = acc.firstCellX() * cellW;
		int minZ = acc.firstCellZ() * cellW;
		
		long t0 = System.nanoTime();
		double[] densities = new double[total];
		
		try {
			DFSerializer.SerializedTree tree = DFSerializer.serialize(df);
			DensityFunctionTree.instance().compute_densities_batch(tree.nodes(), tree.bnPtrs(), tree.noisePtrs(), minX, minY, minZ, sizeX, sizeY, sizeZ, densities);
			PrecomputedDensity.set(densities, minY, sizeX, sizeY, sizeZ);
		} catch (Exception e) {
			HWOPT.LOGGER.error("DensityPrecomputer.precompute failed", e);
		}
	}
}

