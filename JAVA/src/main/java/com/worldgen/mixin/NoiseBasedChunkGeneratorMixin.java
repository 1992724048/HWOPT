package com.worldgen.mixin;

import com.hwpp.mod.ChunkGenStats;
import com.hwpp.mod.Config;
import com.worldgen.accessor.NoiseChunkAccessor;
import com.worldgen.util.BlockIdRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.worldgen.util.BlockIdRegistry.AIR_ID;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
	
	@Shadow
	@Final
	private Holder<NoiseGeneratorSettings> settings;
	
	@Unique
	private static final Executor WORLDGEN_EXECUTOR = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()), runnable -> new Thread(runnable, "hwopt-worldgen-thread"));
	
	@Inject(method = "fillFromNoise", at = @At("HEAD"), cancellable = true)
	public void hwopt$fillFromNoise(final Blender blender, final RandomState randomState, final StructureManager structureManager, final ChunkAccess centerChunk, final CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
		final NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(centerChunk.getHeightAccessorForGeneration());
		final int minY = noiseSettings.minY();
		final int cellYMin = Mth.floorDiv(minY, noiseSettings.getCellHeight());
		final int cellCountY = Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
		
		if (cellCountY <= 0) {
			cir.setReturnValue(CompletableFuture.completedFuture(centerChunk));
			return;
		}
		
		ChunkGenStats.ACTIVE_GEN.incrementAndGet();
		CompletableFuture<ChunkAccess> future = CompletableFuture.supplyAsync(() -> {
			ChunkGenStats.GEN_START.compareAndSet(-1, System.nanoTime());
			
			final int topIndex = centerChunk.getSectionIndex(cellCountY * noiseSettings.getCellHeight() - 1 + minY);
			final int bottomIndex = centerChunk.getSectionIndex(minY);
			
			final Set<LevelChunkSection> sections = new HashSet<>();
			for (int i = topIndex; i >= bottomIndex; i--) {
				LevelChunkSection s = centerChunk.getSection(i);
				s.acquire();
				sections.add(s);
			}
			
			try {
				return hwopt$doFill(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY);
			} finally {
				for (LevelChunkSection s : sections) {
					s.release();
				}
			}
		}, WORLDGEN_EXECUTOR);
		
		hwopt$attachStatsHook(future);
		
		cir.setReturnValue(future);
	}
	
	@Shadow
	protected abstract NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState randomState);
	
	@Unique
	private void hwopt$attachStatsHook(CompletableFuture<ChunkAccess> future) {
		boolean logGen = Config.LOG_CHUNK_GEN.getAsBoolean();
		int interval = Config.LOG_CHUNK_GEN_INTERVAL.getAsInt();
		if (!logGen && interval <= 0) return;
		
		future.whenComplete((res, ex) -> hwopt$printStatsOnComplete(logGen, interval));
	}
	
	@Unique
	private void hwopt$printStatsOnComplete(boolean logGen, int printInterval) {
		int chunks = ChunkGenStats.GEN_CHUNKS.incrementAndGet();
		boolean burstEnd = ChunkGenStats.ACTIVE_GEN.decrementAndGet() == 0;
		
		boolean shouldPrint;
		if (printInterval > 0) {
			shouldPrint = chunks % printInterval == 0;
		} else {
			shouldPrint = burstEnd && chunks > 0 && logGen;
		}
		if (!shouldPrint) return;
		
		long startNs = ChunkGenStats.GEN_START.get();
		if (startNs <= 0) return;
		
		long now = System.nanoTime();
		long elapsedMs = (now - startNs) / 1_000_000L;
		String elapsed = hwopt$formatTime(elapsedMs);
		String avg = hwopt$formatTime(elapsedMs / Math.max(chunks, 1));
		long chkPerSec = chunks * 1000L / Math.max(elapsedMs, 1);
		
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(Component.translatable("hwopt.chunk_gen.stats", chunks, elapsed, avg, chkPerSec))));
		}
	}
	
	@Unique
	private static String hwopt$formatTime(long ms) {
		if (ms < 1000) {
			return ms + " ms";
		}
		return String.format("%.1f s", ms / 1000.0);
	}
	
	@Unique
	private ChunkAccess hwopt$doFill(final Blender blender, final StructureManager structureManager, final RandomState randomState, final ChunkAccess centerChunk, final int cellMinY, final int cellCountY) {
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
		
		final int sizeX = cellCountX * cellWidth;
		final int sizeY = cellCountY * cellHeight;
		final int sizeZ = cellCountZ * cellWidth;
		final int baseY = cellMinY * cellHeight;
		
		final short[] noiseCache = hwopt$computeNoiseCache(noiseChunk, noiseAccessor, defaultBlock, chunkStartBlockX, chunkStartBlockZ, cellMinY, cellCountY, cellWidth, cellHeight, sizeX, sizeY, sizeZ);
		
		hwopt$applyNoiseToChunk(centerChunk, noiseCache, oceanFloor, worldSurface, aquifer, sizeX, sizeY, sizeZ, baseY, chunkStartBlockX, chunkStartBlockZ, baseLocalX, baseLocalZ);
		
		return centerChunk;
	}
	
	@Unique
	private short[] hwopt$computeNoiseCache(final NoiseChunk noiseChunk, final NoiseChunkAccessor noiseAccessor, final BlockState defaultBlock, final int chunkStartBlockX, final int chunkStartBlockZ, final int cellMinY, final int cellCountY, final int cellWidth, final int cellHeight, final int sizeX, final int sizeY, final int sizeZ) {
		final int cellCountX = 16 / cellWidth;
		final int cellCountZ = 16 / cellWidth;
		final short[] noiseCache = new short[sizeX * sizeY * sizeZ];
		
		for (int cellX = 0; cellX < cellCountX; cellX++) {
			noiseChunk.advanceCellX(cellX);
			
			for (int cellZ = 0; cellZ < cellCountZ; cellZ++) {
				for (int cellY = cellCountY - 1; cellY >= 0; cellY--) {
					noiseChunk.selectCellYZ(cellY, cellZ);
					
					for (int yInCell = cellHeight - 1; yInCell >= 0; yInCell--) {
						final int posY = (cellMinY + cellY) * cellHeight + yInCell;
						noiseChunk.updateForY(posY, (double) yInCell / cellHeight);
						final int yIdx = cellY * cellHeight + yInCell;
						
						for (int xInCell = 0; xInCell < cellWidth; xInCell++) {
							noiseChunk.updateForX(chunkStartBlockX + cellX * cellWidth + xInCell, (double) xInCell / cellWidth);
							final int xIdx = cellX * cellWidth + xInCell;
							
							for (int zInCell = 0; zInCell < cellWidth; zInCell++) {
								noiseChunk.updateForZ(chunkStartBlockZ + cellZ * cellWidth + zInCell, (double) zInCell / cellWidth);
								final int zIdx = cellZ * cellWidth + zInCell;
								
								BlockState state = noiseAccessor.invokeGetInterpolatedState();
								if (state == null) {
									state = defaultBlock;
								}
								noiseCache[(yIdx * sizeZ + zIdx) * sizeX + xIdx] = BlockIdRegistry.getId(state.getBlock());
							}
						}
					}
				}
			}
			noiseChunk.swapSlices();
		}
		noiseChunk.stopInterpolation();
		
		return noiseCache;
	}
	
	@Unique
	private void hwopt$applyNoiseToChunk(final ChunkAccess centerChunk, final short[] noiseCache, final Heightmap oceanFloor, final Heightmap worldSurface, final Aquifer aquifer, final int sizeX, final int sizeY, final int sizeZ, final int baseY, final int chunkStartBlockX, final int chunkStartBlockZ, final int baseLocalX, final int baseLocalZ) {
		final boolean scheduleFluid = aquifer.shouldScheduleFluidUpdate();
		final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		
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
			
			for (int z = 0; z < sizeZ; z++) {
				final int localZ = (baseLocalZ + z) & 15;
				final int worldZ = chunkStartBlockZ + z;
				final int zBase = (y * sizeZ + z) * sizeX;
				
				for (int x = 0; x < sizeX; x++) {
					final short blockId = noiseCache[zBase + x];
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
	}
}
