package com.worldgen.mixin;

import com.worldgen.accessor.NoiseChunkAccessor;
import com.worldgen.util.BlockIdRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.*;
import util.TimeCost;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.worldgen.util.BlockIdRegistry.AIR_ID;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {

    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    private static final Executor WORLDGEN_EXECUTOR = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() * 2));

    private static final boolean PROFILE = false;

    @Unique
    private static final AtomicLong TOTAL_STATE_TIME = new AtomicLong();
    @Unique
    private static final AtomicLong TOTAL_WRITE_TIME = new AtomicLong();
    @Unique
    private static final AtomicLong TOTAL_CHUNK_COUNT = new AtomicLong();

    @Overwrite
    public CompletableFuture<ChunkAccess> fillFromNoise(final Blender blender, final RandomState randomState, final StructureManager structureManager, final ChunkAccess centerChunk) {
        final NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(centerChunk.getHeightAccessorForGeneration());
        final int minY = noiseSettings.minY();
        final int cellYMin = Mth.floorDiv(minY, noiseSettings.getCellHeight());
        final int cellCountY = Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
        if (cellCountY <= 0) {
            return CompletableFuture.completedFuture(centerChunk);
        }

        return CompletableFuture.supplyAsync(() -> {
            final int topIndex = centerChunk.getSectionIndex(cellCountY * noiseSettings.getCellHeight() - 1 + minY);
            final int bottomIndex = centerChunk.getSectionIndex(minY);

            final Set<LevelChunkSection> sections = new HashSet<>();
            for (int i = topIndex; i >= bottomIndex; i--) {
                LevelChunkSection s = centerChunk.getSection(i);
                s.acquire();
                sections.add(s);
            }

            ChunkAccess result;
            try {
                result = doFill(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY);
            } finally {
                for (LevelChunkSection s : sections) {
                    s.release();
                }
            }
            return result;
        }, WORLDGEN_EXECUTOR);
    }

    @Overwrite
    public ChunkAccess doFill(final Blender blender, final StructureManager structureManager, final RandomState randomState, final ChunkAccess centerChunk, final int cellMinY, final int cellCountY) {
        final NoiseChunk noiseChunk = centerChunk.getOrCreateNoiseChunk(chunk -> this.createNoiseChunk(chunk, structureManager, blender, randomState));
        final NoiseChunkAccessor noiseAccessor = (NoiseChunkAccessor) noiseChunk;
        final NoiseGeneratorSettings genSettings = this.settings.value();
        final BlockState defaultBlock = genSettings.defaultBlock();

        final Heightmap oceanFloor = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        final Heightmap worldSurface = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        final ChunkPos chunkPos = centerChunk.getPos();
        final int chunkStartBlockX = chunkPos.getMinBlockX();
        final int chunkStartBlockZ = chunkPos.getMinBlockZ();
        final int baseLocalX = chunkStartBlockX & 15;
        final int baseLocalZ = chunkStartBlockZ & 15;

        final Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();

        final int cellWidth = noiseAccessor.invokeCellWidth();
        final int cellHeight = noiseAccessor.invokeCellHeight();
        final int cellCountX = 16 / cellWidth;
        final int cellCountZ = 16 / cellWidth;

        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        final int sizeX = cellCountX * cellWidth;
        final int sizeY = cellCountY * cellHeight;
        final int sizeZ = cellCountZ * cellWidth;
        final int strideY = sizeX * sizeZ;
        final int baseY = cellMinY * cellHeight;

        final short[] noiseCache = new short[sizeX * sizeY * sizeZ];

        // === 噪声计算 ===
        final long stateStart = PROFILE ? System.nanoTime() : 0;

        for (int cellX = 0; cellX < cellCountX; cellX++) {
            noiseChunk.advanceCellX(cellX);

            for (int cellZ = 0; cellZ < cellCountZ; cellZ++) {
                for (int cellY = cellCountY - 1; cellY >= 0; cellY--) {
                    noiseChunk.selectCellYZ(cellY, cellZ);

                    for (int yInCell = cellHeight - 1; yInCell >= 0; yInCell--) {
                        final int posY = (cellMinY + cellY) * cellHeight + yInCell;
                        noiseChunk.updateForY(posY, (double) yInCell / cellHeight);

                        final int arrayY = posY - baseY;
                        final int zSliceBase = arrayY * strideY;

                        for (int xInCell = 0; xInCell < cellWidth; xInCell++) {
                            final int worldX = chunkStartBlockX + cellX * cellWidth + xInCell;
                            noiseChunk.updateForX(worldX, (double) xInCell / cellWidth);

                            final int arrayX = cellX * cellWidth + xInCell;

                            for (int zInCell = 0; zInCell < cellWidth; zInCell++) {
                                final int worldZ = chunkStartBlockZ + cellZ * cellWidth + zInCell;
                                noiseChunk.updateForZ(worldZ, (double) zInCell / cellWidth);

                                BlockState state = noiseAccessor.invokeGetInterpolatedState();
                                if (state == null) {
                                    state = defaultBlock;
                                }

                                final int arrayZ = cellZ * cellWidth + zInCell;
                                noiseCache[arrayX + sizeX * arrayZ + zSliceBase] = BlockIdRegistry.getId(state.getBlock());
                            }
                        }
                    }
                }
            }
            noiseChunk.swapSlices();
        }
        noiseChunk.stopInterpolation();

        if (PROFILE) {
            TOTAL_STATE_TIME.addAndGet(System.nanoTime() - stateStart);
        }

        // === 写入区块 ===
        final long writeStart = PROFILE ? System.nanoTime() : 0;

        final boolean scheduleFluid = aquifer.shouldScheduleFluidUpdate();

        int lastSectionIndex = -1;
        LevelChunkSection section = null;

        for (int y = sizeY - 1; y >= 0; y--) {
            final int posY = baseY + y;
            final int sectionIndex = centerChunk.getSectionIndex(posY);
            if (sectionIndex != lastSectionIndex) {
                section = centerChunk.getSection(sectionIndex);
                lastSectionIndex = sectionIndex;
            }
            if (section == null) continue;

            final int localY = posY & 15;
            final int yOff = y * strideY;

            for (int z = 0; z < sizeZ; z++) {
                final int localZ = (baseLocalZ + z) & 15;
                final int worldZ = chunkStartBlockZ + z;
                final int lineBase = yOff + z * sizeX;

                for (int x = 0; x < sizeX; x++) {
                    final short blockId = noiseCache[lineBase + x];
                    if (blockId == AIR_ID) continue;

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

        if (PROFILE) {
            TOTAL_WRITE_TIME.addAndGet(System.nanoTime() - writeStart);
            final long chunks = TOTAL_CHUNK_COUNT.incrementAndGet();
            if ((chunks & 63) == 0) {
                System.out.println("Terrain Gen Summary (" + chunks + " chunks)");
                System.out.println("state total: " + TimeCost.formatNanos(TOTAL_STATE_TIME.get()));
                System.out.println("write total: " + TimeCost.formatNanos(TOTAL_WRITE_TIME.get()));
            }
        }

        return centerChunk;
    }

    @Shadow
    protected abstract NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState randomState);
}
