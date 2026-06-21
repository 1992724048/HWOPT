package com.hwpp.util;

import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import library.dll.BlockIdRegistryNative;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class BlockIdRegistry {
	public static final Short2ObjectOpenHashMap<Block> idToBlock = new Short2ObjectOpenHashMap<>();
	public static final Object2ShortOpenHashMap<Block> blockToId = new Object2ShortOpenHashMap<>();
	public static BlockState[] blockStates;
	public static Block[] blocks_;
	public static short AIR_ID = 0;
	
	static {
		blockToId.defaultReturnValue((short) 0);
	}
	
	public static <ResourceLocation> void init() {
		List<Block> blocks = new ArrayList<>();
		
		for (Block block : BuiltInRegistries.BLOCK) {
			blocks.add(block);
		}
		
		blocks.sort(Comparator.comparing(b -> {
			ResourceLocation key = (ResourceLocation) BuiltInRegistries.BLOCK.getKey(b);
			return key.toString();
		}));
		
		blockStates = new BlockState[blocks.size()];
		blocks_ = new Block[blocks.size()];
		
		short id = 0;
		for (Block block : blocks) {
			blockToId.put(block, id);
			idToBlock.put(id, block);
			blockStates[id] = block.defaultBlockState();
			blocks_[id] = block;
			if (block == Blocks.AIR) {
				AIR_ID = id;
			}
			
			ResourceLocation key = (ResourceLocation) BuiltInRegistries.BLOCK.getKey(block);
			BlockIdRegistryNative.NATIVE.registry_block(key.toString(), id);
			
			id++;
			if (id == Short.MAX_VALUE) {
				throw new IllegalStateException("Too many blocks for short id!");
			}
		}
	}
	
	public static Block getBlock(short id) {
		return idToBlock.get(id);
	}
	
	public static short getId(Block block) {
		return blockToId.getShort(block);
	}
}
