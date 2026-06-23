package com.server.render.entityculling.occlusion;

import com.hwpp.mod.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public class Provider implements DataProvider {
	@Override
	public boolean isOpaqueFullCube(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) return false;
		if (state.canOcclude()) {
			if (Config.get().solidLeaves && state.getBlock() instanceof LeavesBlock) {
				return true;
			}
			return state.isSolidRender();
		}
		return false;
	}
}
