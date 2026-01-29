package com.worldgen.accessor;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseChunk.class)
public interface NoiseChunkAccessor {
    
    @Invoker("cellWidth")
    int invokeCellWidth();
    
    @Invoker("cellHeight")
    int invokeCellHeight();
    
    @Invoker("getInterpolatedState")
    BlockState invokeGetInterpolatedState();
}
