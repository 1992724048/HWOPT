package com.server.render.entityculling.occlusion;

import com.server.render.entityculling.occlusion.DataProvider;
import com.hwpp.mod.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;

public class Provider implements DataProvider {
    private final Minecraft client = Minecraft.getInstance();
    private ClientLevel world;

    @Override
    public boolean prepareChunk(int chunkX, int chunkZ) {
        world = client.level;
        return world != null;
    }

    @Override
    public boolean isOpaqueFullCube(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        var state = world.getBlockState(pos);
        if (Config.CONFIG.solidLeaves.get() && state.getBlock() instanceof LeavesBlock) {
            return true;
        }
        return state.isSolidRender();
    }

    @Override
    public void cleanup() {
        world = null;
    }
}
