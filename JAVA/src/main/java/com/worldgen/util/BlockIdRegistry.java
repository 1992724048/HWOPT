package com.worldgen.util;

import library.dll.BlockIdRegistryNative;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class BlockIdRegistry {
    public static final Map<Short, Block> idToBlock = new HashMap<>();
    public static final Map<Block, Short> blockToId = new HashMap<>();
    public static BlockState[] blockStates;
    public static Block[] blocks_;
    public static short AIR_ID = 0;

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
            System.out.println("[BlockIdRegistry] " + id + " -> " + key);
            BlockIdRegistryNative.NATIVE.registry_block(key.toString(), id);
            
            id++;
            if (id == Short.MAX_VALUE) {
                throw new IllegalStateException("Too many blocks for short id!");
            }
        }
        
        System.out.println("[BlockIdRegistry] Total blocks: " + blocks.size());
        System.out.println("[BlockIdRegistry] Finished. Count=" + idToBlock.size());
    }
    
    public static Block getBlock(short id) {
        return idToBlock.get(id);
    }
    
    public static short getId(Block block) {
        return blockToId.get(block);
    }
}
