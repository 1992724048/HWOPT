package com.server.world.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
    private static void hwopt$spawnAtVillage(final ServerLevel level, final ServerLevelData levelData, final boolean spawnBonusChest, final boolean isDebug, final LevelLoadListener levelLoadListener, final CallbackInfo ci) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        final Optional<BlockPos> villagePos = Optional.ofNullable(level.findNearestMapStructure(StructureTags.VILLAGE, new BlockPos(0, level.getSeaLevel(), 0), 256, false));
        if (villagePos.isEmpty()) {
            return;
        }

        final BlockPos village = villagePos.get();
        final BlockPos spawn = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, village);

        levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), spawn, 0.0F, 0.0F));
        ci.cancel();
    }
}