package com.server.entity.aabb.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FastLadderCache {
	private static boolean[] CLIMBABLE_BLOCKS = null;

	public static boolean isClimbable(BlockState state) {
		if (CLIMBABLE_BLOCKS == null) rebuildCache();
		int id = BuiltInRegistries.BLOCK.getId(state.getBlock());
		return id >= 0 && id < CLIMBABLE_BLOCKS.length && CLIMBABLE_BLOCKS[id];
	}

	private static synchronized void rebuildCache() {
		if (CLIMBABLE_BLOCKS != null) return;
		int maxId = BuiltInRegistries.BLOCK.size() + 256;
		boolean[] newCache = new boolean[maxId];
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block.builtInRegistryHolder().is(BlockTags.CLIMBABLE)) {
				int id = BuiltInRegistries.BLOCK.getId(block);
				if (id >= 0 && id < newCache.length) {
					newCache[id] = true;
				}
			}
		}
		CLIMBABLE_BLOCKS = newCache;
	}
}
