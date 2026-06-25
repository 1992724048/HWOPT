package com.server.render.entityculling;

import com.hwpp.mod.Config;
import com.server.render.entityculling.occlusion.HardwareOcclusionEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CullTask implements Runnable {

    private final HardwareOcclusionEngine engine;
    private final Minecraft client = Minecraft.getInstance();

    private volatile boolean ingame = false;
    public volatile boolean requestCull = false;

    private volatile List<Entity> entities = Collections.emptyList();
    private volatile Map<BlockPos, BlockEntity> blockEntities = Collections.emptyMap();
    private volatile Vec3 cameraPos = Vec3.ZERO;
    private double lastX, lastY, lastZ;

    public CullTask(HardwareOcclusionEngine engine) {
        this.engine = engine;
    }

    public void setIngame(boolean ingame) { this.ingame = ingame; }

    public void setEntities(List<Entity> entities) { this.entities = entities; }

    public void setBlockEntities(Map<BlockPos, BlockEntity> blockEntities) {
        this.blockEntities = blockEntities;
    }

    public void setCameraPos(Vec3 pos) { this.cameraPos = pos; }

    @Override
    public void run() {
        while (client.isRunning()) {
            try {
                Thread.sleep(Config.CONFIG.sleepDelay.get());
                if (!engine.isEnabled() || !EntityCulling.enabled || !ingame || client.isPaused()) continue;

                Vec3 cam = cameraPos;
                if (requestCull || cam.x != lastX || cam.y != lastY || cam.z != lastZ) {
                    requestCull = false;
                    lastX = cam.x; lastY = cam.y; lastZ = cam.z;

                    for (Entity entity : entities) {
                        if (entity == null) break;
                        if (entity instanceof Player) continue;
                        if (entity.isCurrentlyGlowing()) continue;
                        if (entity.isPassenger()) continue;
                        engine.submitEntityQuery(entity, cam);
                    }

                    for (BlockEntity be : blockEntities.values()) {
                        if (be == null) break;
                        engine.submitBlockEntityQuery(be, cam);
                    }
                }
            } catch (Exception e) {
                EntityCulling.LOGGER.error("CullTask error", e);
            }
        }
    }
}
