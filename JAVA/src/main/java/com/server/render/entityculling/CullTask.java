package com.server.render.entityculling;

import com.server.render.entityculling.occlusion.OcclusionCullingInstance;
import com.server.render.entityculling.occlusion.Vec3d;
import com.server.render.entityculling.access.Cullable;
import com.hwpp.mod.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class CullTask implements Runnable {
    public boolean requestCull = false;
    public boolean disableEntityCulling = false;
    public boolean disableBlockEntityCulling = false;

    private final OcclusionCullingInstance culling;
    private final Minecraft client = Minecraft.getInstance();
    private final int sleepDelay;
    private final int hitboxLimit;
    private final Set<BlockEntityType<?>> blockEntityWhitelist;
    private final Set<EntityType<?>> entityWhitelist;
    public double lastTime = 0;

    private Vec3d lastPos = new Vec3d(0, 0, 0);
    private Vec3d aabbMin = new Vec3d(0, 0, 0);
    private Vec3d aabbMax = new Vec3d(0, 0, 0);

    private boolean ingame = false;
    private List<Entity> entitiesForRendering = new ArrayList<>();
    private Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    private Vec3 cameraMC = new Vec3(0, 0, 0);

    public CullTask(OcclusionCullingInstance culling, Set<BlockEntityType<?>> blockEntityWhitelist,
                    Set<EntityType<?>> entityWhitelist) {
        this.culling = culling;
        this.blockEntityWhitelist = blockEntityWhitelist;
        this.entityWhitelist = entityWhitelist;
        this.sleepDelay = Config.CONFIG.sleepDelay.get();
        this.hitboxLimit = Config.CONFIG.hitboxLimit.get();
    }

    public void setIngame(boolean ingame) { this.ingame = ingame; }
    public void setEntitiesForRendering(List<Entity> entities) { this.entitiesForRendering = entities; }
    public void setBlockEntities(Map<BlockPos, BlockEntity> blockEntities) { this.blockEntities = blockEntities; }
    public void setCameraMC(Vec3 pos) { this.cameraMC = pos; }

    @Override
    public void run() {
        while (client.isRunning()) {
            try {
                Thread.sleep(sleepDelay);
                if (EntityCullingMod.enabled && ingame) {
                    if (requestCull || !(cameraMC.x == lastPos.x && cameraMC.y == lastPos.y && cameraMC.z == lastPos.z)) {
                        long start = System.nanoTime();
                        requestCull = false;
                        lastPos.set(cameraMC.x, cameraMC.y, cameraMC.z);
                        Vec3d camera = lastPos;
                        culling.resetCache();
                        cullBlockEntities(cameraMC, camera);
                        cullEntities(cameraMC, camera);
                        lastTime = (System.nanoTime() - start) / 1_000_000.0;
                    }
                } else {
                    lastTime = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void cullEntities(Vec3 cameraMC, Vec3d camera) {
        if (disableEntityCulling) return;
        EntityCullingMod mod = EntityCullingMod.getInstance();
        int tracingDist = Config.CONFIG.tracingDistance.get();
        Iterator<Entity> iterable = entitiesForRendering.iterator();
        while (iterable.hasNext()) {
            Entity entity = iterable.next();
            if (entity == null) break;
            if (!(entity instanceof Cullable)) continue;
            if (entityWhitelist.contains(entity.getType())) continue;
            if (mod.isDynamicWhitelisted(entity)) continue;
            Cullable cullable = (Cullable) entity;
            if (!cullable.isForcedVisible()) {
                if (client.shouldEntityAppearGlowing(entity)) {
                    cullable.setCulled(false);
                    continue;
                }
                if (!entity.position().closerThan(cameraMC, tracingDist)) {
                    cullable.setCulled(false);
                    continue;
                }
                AABB boundingBox = NMSCullingHelper.getCullingBox(entity);
                if (boundingBox == null || boundingBox.getXsize() > hitboxLimit
                    || boundingBox.getYsize() > hitboxLimit || boundingBox.getZsize() > hitboxLimit) {
                    cullable.setCulled(false);
                    continue;
                }
                aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
                boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
                cullable.setCulled(!visible);
            }
        }
    }

    private void cullBlockEntities(Vec3 cameraMC, Vec3d camera) {
        if (disableBlockEntityCulling) return;
        EntityCullingMod mod = EntityCullingMod.getInstance();
        Iterator<Map.Entry<BlockPos, BlockEntity>> iterator = blockEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BlockEntity> entry;
            try {
                entry = iterator.next();
            } catch (NullPointerException | ConcurrentModificationException ex) {
                break;
            }
            if (entry == null) break;
            if (blockEntityWhitelist.contains(entry.getValue().getType())) continue;
            if (client.getBlockEntityRenderDispatcher().getRenderer(entry.getValue()) == null) continue;
            if (mod.isDynamicWhitelisted(entry.getValue())) continue;
            Cullable cullable = (Cullable) entry.getValue();
            if (!cullable.isForcedVisible()) {
                BlockPos pos = entry.getKey();
                if (closerThan(pos, cameraMC, 64)) {
                    AABB boundingBox = mod.setupAABB(entry.getValue(), pos);
                    if (boundingBox.getXsize() > hitboxLimit
                        || boundingBox.getYsize() > hitboxLimit
                        || boundingBox.getZsize() > hitboxLimit) {
                        cullable.setCulled(false);
                        continue;
                    }
                    aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                    aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
                    boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
                    cullable.setCulled(!visible);
                }
            }
        }
    }

    private static boolean closerThan(BlockPos blockPos, Position position, double d) {
        return distSqr(blockPos, position.x(), position.y(), position.z(), true) < d * d;
    }

    private static double distSqr(BlockPos blockPos, double d, double e, double f, boolean bl) {
        double g = bl ? 0.5D : 0.0D;
        double h = (double) blockPos.getX() + g - d;
        double i = (double) blockPos.getY() + g - e;
        double j = (double) blockPos.getZ() + g - f;
        return h * h + i * i + j * j;
    }
}
