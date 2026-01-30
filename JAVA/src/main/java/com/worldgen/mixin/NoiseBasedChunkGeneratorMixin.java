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
    
    private static final Executor WORLDGEN_EXECUTOR = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
    
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    
    private static boolean layerAllAirXScan(short[] data, int base, int sizeX, int sizeZ, int strideXZ, short air) {
        
        int min = Math.min(sizeX, sizeZ);
        
        for (int i = 0; i < min; i++) {
            int idx = base + i * strideXZ + i;
            if (data[idx] != air) {
                return false;
            }
        }
        
        for (int i = 0; i < min; i++) {
            int z = sizeZ - 1 - i;
            int idx = base + z * strideXZ + i;
            if (data[idx] != air) {
                return false;
            }
        }
        
/*        for (int z = 0; z < sizeZ; z++) {
            int row = base + z * strideXZ;
            for (int x = 0; x < sizeX; x++) {
                if (data[row + x] != air) {
                    return false;
                }
            }
        }*/
        
        return true;
    }
    
    
    @Shadow
    protected abstract ChunkAccess doFill(Blender blender, StructureManager structureManager, RandomState randomState, ChunkAccess centerChunk, int cellMinY, int cellCountY);
    
    @Inject(method = "fillFromNoise", at = @At("HEAD"), cancellable = true)
    private void fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess centerChunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(centerChunk.getHeightAccessorForGeneration());
        int minY = noiseSettings.minY();
        int cellYMin = Mth.floorDiv(minY, noiseSettings.getCellHeight());
        int cellCountY = Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
        cir.setReturnValue(0 >= cellCountY ? CompletableFuture.completedFuture(centerChunk) : CompletableFuture.supplyAsync(() -> {
            int topSectionIndex = centerChunk.getSectionIndex(cellCountY * noiseSettings.getCellHeight() - 1 + minY);
            int bottomSectionIndex = centerChunk.getSectionIndex(minY);
            Set<LevelChunkSection> sections = Sets.newHashSet();
            
            for (int sectionIndex = topSectionIndex; sectionIndex >= bottomSectionIndex; sectionIndex--) {
                LevelChunkSection section = centerChunk.getSection(sectionIndex);
                section.acquire();
                sections.add(section);
            }
            
            ChunkAccess var20;
            try {
                var20 = this.doFill(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY);
            } finally {
                for (LevelChunkSection section : sections) {
                    section.release();
                }
            }
            
            return var20;
        }, WORLDGEN_EXECUTOR));
    }
    
    private static final AtomicLong TOTAL_STATE_TIME = new AtomicLong();
    private static final AtomicLong TOTAL_WRITE_TIME = new AtomicLong();
    private static final AtomicLong TOTAL_CHUNK_COUNT = new AtomicLong();
    
    @Inject(method = "doFill", at = @At("HEAD"), cancellable = true)
    private void doFill(Blender blender, StructureManager structureManager, RandomState randomState, ChunkAccess centerChunk, int cellMinY, int cellCountY, CallbackInfoReturnable<ChunkAccess> cir) {
        
        NoiseChunk noiseChunk = centerChunk.getOrCreateNoiseChunk(chunk -> this.createNoiseChunk(chunk, structureManager, blender, randomState));
        
        Heightmap oceanFloor = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        
        ChunkPos chunkPos = centerChunk.getPos();
        int chunkStartBlockX = chunkPos.getMinBlockX();
        int chunkStartBlockZ = chunkPos.getMinBlockZ();
        
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        
        int cellWidth = ((NoiseChunkAccessor) noiseChunk).invokeCellWidth();
        int cellHeight = ((NoiseChunkAccessor) noiseChunk).invokeCellHeight();
        int cellCountX = 16 / cellWidth;
        int cellCountZ = 16 / cellWidth;
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        int sizeX = cellCountX * cellWidth;
        int sizeY = cellCountY * cellHeight;
        int sizeZ = cellCountZ * cellWidth;
        
        int noiseArraySize = sizeX * sizeY * sizeZ;
        short[] noiseCache = new short[noiseArraySize];
        int strideXZ = sizeX;
        int strideY = sizeX * sizeZ;
        int baseY = cellMinY * cellHeight;
        
        long stateTime = 0;
        long writeTime = 0;
        
        long s0 = System.nanoTime();
        NoiseChunkGeneratorNative.NATIVE.getInterpolatedState(noiseCache, noiseArraySize, sizeX, sizeY, sizeZ);
        long s1 = System.nanoTime();
        stateTime += (s1 - s0);
        
        /*for (int cellX = 0; cellX < cellCountX; cellX++) {
            noiseChunk.advanceCellX(cellX);
            
            for (int cellZ = 0; cellZ < cellCountZ; cellZ++) {
                for (int cellY = cellCountY - 1; cellY >= 0; cellY--) {
                    noiseChunk.selectCellYZ(cellY, cellZ);
                    
                    for (int yInCell = cellHeight - 1; yInCell >= 0; yInCell--) {
                        int posY = (cellMinY + cellY) * cellHeight + yInCell;
                        double fy = (double) yInCell / cellHeight;
                        noiseChunk.updateForY(posY, fy);
                        
                        int arrayY = posY - baseY;
                        
                        for (int xInCell = 0; xInCell < cellWidth; xInCell++) {
                            int worldX = chunkStartBlockX + cellX * cellWidth + xInCell;
                            int arrayX = cellX * cellWidth + xInCell;
                            double fx = (double) xInCell / cellWidth;
                            noiseChunk.updateForX(worldX, fx);
                            
                            for (int zInCell = 0; zInCell < cellWidth; zInCell++) {
                                int worldZ = chunkStartBlockZ + cellZ * cellWidth + zInCell;
                                int arrayZ = cellZ * cellWidth + zInCell;
                                double fz = (double) zInCell / cellWidth;
                                noiseChunk.updateForZ(worldZ, fz);
                                
                                long t0 = System.nanoTime();
                                
                                long t1 = System.nanoTime();
                                loopOnlyTime += (t1 - t0);
                                
                                long s0 = System.nanoTime();
                                BlockState state = ((NoiseChunkAccessor) noiseChunk).invokeGetInterpolatedState();
                                long s1 = System.nanoTime();
                                
                                stateTime += (s1 - s0);
                                
                                if (state == null) {
                                    state = this.settings.value().defaultBlock();
                                }
                                
                                int idx = arrayX + strideXZ * (arrayZ + sizeZ * arrayY);
                                noiseCache[idx] = state;
                            }
                        }
                    }
                }
            }
            noiseChunk.swapSlices();
        }
        
        noiseChunk.stopInterpolation();*/
        
        long w0 = System.nanoTime();
        
        boolean scheduleFluid = aquifer.shouldScheduleFluidUpdate();
        
        int baseLocalX = chunkStartBlockX & 15;
        int baseLocalZ = chunkStartBlockZ & 15;
        int baseWorldX = chunkStartBlockX;
        int baseWorldZ = chunkStartBlockZ;
        
        int lastSectionIndex = -1;
        LevelChunkSection section = null;
        
        for (int y = sizeY - 1; y >= 0; y--) {
            int yOff = y * strideY;
            
            if (layerAllAirXScan(noiseCache, yOff, sizeX, sizeZ, strideXZ, AIR_ID)) {
                continue;
            }
            
            int posY = baseY + y;
            int sectionIndex = centerChunk.getSectionIndex(posY);
            if (sectionIndex != lastSectionIndex) {
                section = centerChunk.getSection(sectionIndex);
                lastSectionIndex = sectionIndex;
            }
            
            int localY = posY & 15;
            
            for (int z = 0; z < sizeZ; z++) {
                int localZ = (baseLocalZ + z) & 15;
                int worldZ = baseWorldZ + z;
                int zOff = z * strideXZ;
                int idx = yOff + zOff;
                
                for (int x = 0; x < sizeX; x++) {
                    short blockId = noiseCache[idx + x];
                    if (blockId == AIR_ID) {
                        continue;
                    }
                    
                    BlockState state = BlockIdRegistry.blockStates[blockId];
                    
                    int localX = (baseLocalX + x) & 15;
                    int worldX = baseWorldX + x;
                    
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
        
        long w1 = System.nanoTime();
        writeTime = w1 - w0;
        
        TOTAL_STATE_TIME.addAndGet(stateTime);
        TOTAL_WRITE_TIME.addAndGet(writeTime);
        
        long chunks = TOTAL_CHUNK_COUNT.incrementAndGet();
        
        if ((chunks & 64) == 0) {
            long stTime = TOTAL_STATE_TIME.get();
            long write = TOTAL_WRITE_TIME.get();
            
            System.out.println("Terrain Gen Summary (" + chunks + " chunks)");
            System.out.println("state total: " + TimeCost.formatNanos(stTime));
            System.out.println("write total: " + TimeCost.formatNanos(write));
        }
        
        cir.setReturnValue(centerChunk);
    }
    
    @Shadow
    protected abstract NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState randomState);
    
}