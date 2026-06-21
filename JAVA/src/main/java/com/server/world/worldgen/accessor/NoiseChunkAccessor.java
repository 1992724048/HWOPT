package com.server.world.worldgen.accessor;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseChunk.class)
public interface NoiseChunkAccessor {
    
    @Invoker("cellWidth")
    int invokeCellWidth();
    
    @Invoker("cellHeight")
    int invokeCellHeight();

    @Invoker("getInterpolatedState")
    BlockState invokeGetInterpolatedState();

    @Invoker("getInterpolatedDensity")
    double invokeGetInterpolatedDensity();

    @Accessor("cellStartBlockX")
    int cellStartBlockX();

    @Accessor("cellStartBlockY")
    int cellStartBlockY();

    @Accessor("cellStartBlockZ")
    int cellStartBlockZ();

    @Accessor("inCellX")
    int inCellX();

    @Accessor("inCellY")
    int inCellY();

    @Accessor("inCellZ")
    int inCellZ();

    @Accessor("fillingCell")
    boolean fillingCell();

    @Accessor("interpolating")
    boolean interpolating();

    @Accessor("cellNoiseMinY")
    int cellNoiseMinY();
}
