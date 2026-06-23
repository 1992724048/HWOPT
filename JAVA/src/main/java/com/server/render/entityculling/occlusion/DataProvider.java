package com.server.render.entityculling.occlusion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public interface DataProvider {
	boolean isOpaqueFullCube(BlockGetter level, BlockPos pos);
}
