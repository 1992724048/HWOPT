package com.worldgen.mixin;

import com.google.common.collect.Sets;
import com.worldgen.accessor.NoiseChunkAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.google.common.util.concurrent.Futures.submit;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;
    
    private static final Executor WORLDGEN_EXECUTOR =
            Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
    
    @Shadow
    protected abstract ChunkAccess doFill(Blender blender, StructureManager structureManager, RandomState randomState,
                                          ChunkAccess centerChunk, int cellMinY, int cellCountY);
    
    @Inject(method = "fillFromNoise", at = @At("HEAD"), cancellable = true)
    private void fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager,
                               ChunkAccess centerChunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(
                centerChunk.getHeightAccessorForGeneration()); int minY = noiseSettings.minY();
        int cellYMin = Mth.floorDiv(minY, noiseSettings.getCellHeight());
        int cellCountY = Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight()); cir.setReturnValue(
                cellCountY <= 0 ? CompletableFuture.completedFuture(centerChunk) : CompletableFuture.supplyAsync(() -> {
                    int topSectionIndex = centerChunk.getSectionIndex(
                            cellCountY * noiseSettings.getCellHeight() - 1 + minY);
                    int bottomSectionIndex = centerChunk.getSectionIndex(minY);
                    Set<LevelChunkSection> sections = Sets.newHashSet();
                    
                    for (int sectionIndex = topSectionIndex; sectionIndex >= bottomSectionIndex; sectionIndex--) {
                        LevelChunkSection section = centerChunk.getSection(sectionIndex); section.acquire();
                        sections.add(section);
                    }
                    
                    ChunkAccess var20; try {
                        var20 = this.doFill(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY);
                    } finally {
                        for (LevelChunkSection section : sections) {
                            section.release();
                        }
                    }
                    
                    return var20;
                }, WORLDGEN_EXECUTOR));
    }
    
    @Inject(method = "doFill", at = @At("HEAD"), cancellable = true)
    private void doFill(Blender blender, StructureManager structureManager, RandomState randomState,
                        ChunkAccess centerChunk, int cellMinY, int cellCountY,
                        CallbackInfoReturnable<ChunkAccess> cir) {
        
        NoiseChunk noiseChunk = centerChunk.getOrCreateNoiseChunk(
                chunk -> this.createNoiseChunk(chunk, structureManager, blender, randomState));
        
        Heightmap oceanFloor = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        
        ChunkPos chunkPos = centerChunk.getPos(); int chunkStartBlockX = chunkPos.getMinBlockX();
        int chunkStartBlockZ = chunkPos.getMinBlockZ();
        
        Aquifer aquifer = noiseChunk.aquifer(); noiseChunk.initializeForFirstCellX();
        
        int cellWidth = ((NoiseChunkAccessor) noiseChunk).invokeCellWidth();
        int cellHeight = ((NoiseChunkAccessor) noiseChunk).invokeCellHeight(); int cellCountX = 16 / cellWidth;
        int cellCountZ = 16 / cellWidth;
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        for (int cellX = 0; cellX < cellCountX; cellX++) {
            noiseChunk.advanceCellX(cellX);
            
            for (int cellZ = 0; cellZ < cellCountZ; cellZ++) {
                int lastSectionIndex = centerChunk.getSectionsCount() - 1;
                LevelChunkSection section = centerChunk.getSection(lastSectionIndex);
                
                for (int cellY = cellCountY - 1; cellY >= 0; cellY--) {
                    noiseChunk.selectCellYZ(cellY, cellZ);
                    
                    for (int yInCell = cellHeight - 1; yInCell >= 0; yInCell--) {
                        int posY = (cellMinY + cellY) * cellHeight + yInCell; int yInSection = posY & 15;
                        int sectionIndex = centerChunk.getSectionIndex(posY); if (sectionIndex != lastSectionIndex) {
                            lastSectionIndex = sectionIndex; section = centerChunk.getSection(sectionIndex);
                        }
                        
                        double fy = (double) yInCell / cellHeight; noiseChunk.updateForY(posY, fy);
                        
                        for (int xInCell = 0; xInCell < cellWidth; xInCell++) {
                            int posX = chunkStartBlockX + cellX * cellWidth + xInCell; int xInSection = posX & 15;
                            double fx = (double) xInCell / cellWidth; noiseChunk.updateForX(posX, fx);
                            
                            for (int zInCell = 0; zInCell < cellWidth; zInCell++) {
                                int posZ = chunkStartBlockZ + cellZ * cellWidth + zInCell; int zInSection = posZ & 15;
                                double fz = (double) zInCell / cellWidth; noiseChunk.updateForZ(posZ, fz);
                                
                                BlockState state = ((NoiseChunkAccessor) noiseChunk).invokeGetInterpolatedState();
                                
                                if (state == null) {
                                    state = this.settings.value().defaultBlock();
                                }
                                
                                if (state != Blocks.AIR.defaultBlockState()) {
                                    section.setBlockState(xInSection, yInSection, zInSection, state, false);
                                    oceanFloor.update(xInSection, posY, zInSection, state);
                                    worldSurface.update(xInSection, posY, zInSection, state);
                                    
                                    if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
                                        pos.set(posX, posY, posZ); centerChunk.markPosForPostprocessing(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            noiseChunk.swapSlices();
        }
        
        noiseChunk.stopInterpolation(); cir.setReturnValue(centerChunk);
    }
    
    @Shadow
    protected abstract NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager,
                                                   Blender blender, RandomState randomState);
    
}