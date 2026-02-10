package com.worldgen.mixin;

import com.google.common.collect.Sets;
import com.worldgen.accessor.NoiseChunkAccessor;
import com.worldgen.util.BlockIdRegistry;
import library.dll.BlockIdRegistryNative;
import library.dll.NoiseChunkGeneratorNative;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import util.TimeCost;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.util.concurrent.Futures.submit;
import static com.worldgen.util.BlockIdRegistry.AIR_ID;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
	@Shadow
	@Final
	private Holder<NoiseGeneratorSettings> settings;
	
	private static final Executor WORLDGEN_EXECUTOR = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() * 2));
	
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	
	@Shadow
	protected abstract ChunkAccess doFill(Blender blender, StructureManager structureManager, RandomState randomState, ChunkAccess centerChunk, int cellMinY, int cellCountY);
	
	@Inject(method = "fillFromNoise", at = @At("HEAD"), cancellable = true)
	private void fillFromNoise(final Blender blender, final RandomState randomState, final StructureManager structureManager, final ChunkAccess centerChunk, final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
		final NoiseSettings noiseSettings = settings.value().noiseSettings().clampToHeightAccessor(centerChunk.getHeightAccessorForGeneration());
		final int minY = noiseSettings.minY();
		final int cellYMin = Mth.floorDiv(minY, noiseSettings.getCellHeight());
		final int cellCountY = Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
		cir.setReturnValue(0 >= cellCountY ? CompletableFuture.completedFuture(centerChunk) : CompletableFuture.supplyAsync(() -> {
			final int topSectionIndex = centerChunk.getSectionIndex(cellCountY * noiseSettings.getCellHeight() - 1 + minY);
			final int bottomSectionIndex = centerChunk.getSectionIndex(minY);
			final Set<LevelChunkSection> sections = Sets.newHashSet();
			
			for (int sectionIndex = topSectionIndex; sectionIndex >= bottomSectionIndex; sectionIndex--) {
				final LevelChunkSection section = centerChunk.getSection(sectionIndex);
				section.acquire();
				sections.add(section);
			}
			
			ChunkAccess var20;
			try {
				var20 = doFill(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY);
			} finally {
				for (final LevelChunkSection section : sections) {
					section.release();
				}
			}
			return var20;
		}, NoiseBasedChunkGeneratorMixin.WORLDGEN_EXECUTOR));
	}
	
	@Unique
	private static final AtomicLong TOTAL_STATE_TIME = new AtomicLong();
	@Unique
	private static final AtomicLong TOTAL_WRITE_TIME = new AtomicLong();
	@Unique
	private static final AtomicLong TOTAL_CHUNK_COUNT = new AtomicLong();
	
	@Inject(method = "doFill", at = @At("HEAD"), cancellable = true)
	private void doFill(final Blender blender, final StructureManager structureManager, final RandomState randomState, final ChunkAccess centerChunk, final int cellMinY, final int cellCountY, final CallbackInfoReturnable<ChunkAccess> cir) {
		
		final NoiseChunk noiseChunk = centerChunk.getOrCreateNoiseChunk(chunk -> this.createNoiseChunk(chunk, structureManager, blender, randomState));
		
		final Heightmap oceanFloor = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		final Heightmap worldSurface = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		
		final ChunkPos chunkPos = centerChunk.getPos();
		final int chunkStartBlockX = chunkPos.getMinBlockX();
		final int chunkStartBlockZ = chunkPos.getMinBlockZ();
		
		final Aquifer aquifer = noiseChunk.aquifer();
		noiseChunk.initializeForFirstCellX();
		
		final int cellWidth = ((NoiseChunkAccessor) noiseChunk).invokeCellWidth();
		final int cellHeight = ((NoiseChunkAccessor) noiseChunk).invokeCellHeight();
		final int cellCountX = 16 / cellWidth;
		final int cellCountZ = 16 / cellWidth;
		
		final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		
		final int sizeX = cellCountX * cellWidth;
		final int sizeY = cellCountY * cellHeight;
		final int sizeZ = cellCountZ * cellWidth;
		
		final int noiseArraySize = sizeX * sizeY * sizeZ;
		final short[] noiseCache = new short[noiseArraySize];
		final int strideY = sizeX * sizeZ;
		final int baseY = cellMinY * cellHeight;
		
		long stateTime = 0;
		long writeTime = 0;
        
        long s0 = System.nanoTime();
        NoiseChunkGeneratorNative.NATIVE.getInterpolatedState(noiseCache, noiseArraySize, sizeX, sizeY, sizeZ);
        long s1 = System.nanoTime();
        stateTime += (s1 - s0);
		
/*		final long s0 = System.nanoTime();
		for (int cellX = 0; cellX < cellCountX; cellX++) {
			noiseChunk.advanceCellX(cellX);
			
			for (int cellZ = 0; cellZ < cellCountZ; cellZ++) {
				for (int cellY = cellCountY - 1; 0 <= cellY; cellY--) {
					noiseChunk.selectCellYZ(cellY, cellZ);
					
					for (int yInCell = cellHeight - 1; 0 <= yInCell; yInCell--) {
						final int posY = (cellMinY + cellY) * cellHeight + yInCell;
						final double fy = (double) yInCell / cellHeight;
						noiseChunk.updateForY(posY, fy);
						
						final int arrayY = posY - baseY;
						
						for (int xInCell = 0; xInCell < cellWidth; xInCell++) {
							final int worldX = chunkStartBlockX + cellX * cellWidth + xInCell;
							final int arrayX = cellX * cellWidth + xInCell;
							final double fx = (double) xInCell / cellWidth;
							noiseChunk.updateForX(worldX, fx);
							
							for (int zInCell = 0; zInCell < cellWidth; zInCell++) {
								final int worldZ = chunkStartBlockZ + cellZ * cellWidth + zInCell;
								final int arrayZ = cellZ * cellWidth + zInCell;
								final double fz = (double) zInCell / cellWidth;
								noiseChunk.updateForZ(worldZ, fz);
								
								BlockState state = ((NoiseChunkAccessor) noiseChunk).invokeGetInterpolatedState();
								
								if (null == state) {
									state = this.settings.value().defaultBlock();
								}
								
								final int idx = arrayX + sizeX * (arrayZ + sizeZ * arrayY);
								noiseCache[idx] = BlockIdRegistry.getId(state.getBlock());
							}
						}
					}
				}
			}
			noiseChunk.swapSlices();
		}
		noiseChunk.stopInterpolation();*//*
		
		final long s1 = System.nanoTime();
		stateTime += (s1 - s0);*/
		
		final long w0 = System.nanoTime();
		final boolean scheduleFluid = aquifer.shouldScheduleFluidUpdate();
		
		final int baseLocalX = chunkStartBlockX & 15;
		final int baseLocalZ = chunkStartBlockZ & 15;
		
		int lastSectionIndex = -1;
		LevelChunkSection section = null;
		
		for (int y = sizeY - 1; 0 <= y; y--) {
			final int yOff = y * strideY;
			
			final int posY = baseY + y;
			final int sectionIndex = centerChunk.getSectionIndex(posY);
			if (sectionIndex != lastSectionIndex) {
				section = centerChunk.getSection(sectionIndex);
				lastSectionIndex = sectionIndex;
			}
			
			if (null == section) {
				continue;
			}
			
			final int localY = posY & 15;
			for (int z = 0; z < sizeZ; z++) {
				final int localZ = (baseLocalZ + z) & 15;
				final int worldZ = chunkStartBlockZ + z;
				final int zOff = z * sizeX;
				final int idx = yOff + zOff;
				
				for (int x = 0; x < sizeX; x++) {
					final short blockId = noiseCache[idx + x];
					if (blockId == AIR_ID) {
						continue;
					}
					
					final BlockState state = BlockIdRegistry.blockStates[blockId];
					
					final int localX = (baseLocalX + x) & 15;
					final int worldX = chunkStartBlockX + x;
					
					section.setBlockState(localX, localY, localZ, state, false);
					
					oceanFloor.update(localX, posY, localZ, state);
					worldSurface.update(localX, posY, localZ, state);
					
					if (scheduleFluid && !state.getFluidState().isEmpty()) {
						pos.set(worldX, posY, worldZ);
						centerChunk.markPosForPostprocessing(pos);
					}
				}
			}
		}
		
		final long w1 = System.nanoTime();
		writeTime = w1 - w0;
		
		NoiseBasedChunkGeneratorMixin.TOTAL_STATE_TIME.addAndGet(stateTime);
		NoiseBasedChunkGeneratorMixin.TOTAL_WRITE_TIME.addAndGet(writeTime);
		
		final long chunks = NoiseBasedChunkGeneratorMixin.TOTAL_CHUNK_COUNT.incrementAndGet();
		
		if (0 == (chunks & 64)) {
			final long stTime = NoiseBasedChunkGeneratorMixin.TOTAL_STATE_TIME.get();
			final long write = NoiseBasedChunkGeneratorMixin.TOTAL_WRITE_TIME.get();
			
			System.out.println("Terrain Gen Summary (" + chunks + " chunks)");
			System.out.println("state total: " + TimeCost.formatNanos(stTime));
			System.out.println("write total: " + TimeCost.formatNanos(write));
		}
		
		cir.setReturnValue(centerChunk);
	}
	
	@Shadow
	protected abstract NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState randomState);
	
}