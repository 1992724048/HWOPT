package com.server.render.entityculling;

import com.server.render.entityculling.occlusion.OcclusionCullingInstance;
import com.server.render.entityculling.occlusion.Provider;
import com.hwpp.mod.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class EntityCulling {
    public static final Logger LOGGER = LoggerFactory.getLogger("EntityCulling");
    private static volatile EntityCulling instance;
    public static boolean enabled = true;

    public final DebugCollector debugCollector = new DebugCollector();
    public OcclusionCullingInstance culling;
    public boolean debugHitboxes = false;
    public int renderedBlockEntities = 0;
    public int skippedBlockEntities = 0;
    public int renderedEntities = 0;
    public int skippedEntities = 0;
    public int tickedEntities = 0;
    public int skippedEntityTicks = 0;
    public Set<BlockEntityType<?>> blockEntityWhitelist = new HashSet<>();
    public Set<EntityType<?>> entityWhitelist = new HashSet<>();
    public Set<EntityType<?>> tickCullWhitelists = new HashSet<>();
    public CullTask cullTask;
    public double lastTickTime = 0;
    public Frustum frustum = null;

    private final Thread cullThread;
    private int tickCounter = 0;
    private boolean lateInit = false;
    private final Set<Function<BlockEntity, Boolean>> dynamicBlockEntityWhitelist = new HashSet<>();
    private final Set<Function<Entity, Boolean>> dynamicEntityWhitelist = new HashSet<>();

    public EntityCulling() {
        instance = this;
        culling = new OcclusionCullingInstance(64, new Provider());
        cullTask = new CullTask(culling, blockEntityWhitelist, entityWhitelist);
        cullThread = new Thread(cullTask, "CullThread");
        cullThread.setUncaughtExceptionHandler((thread, ex) -> LOGGER.error("CullingThread crashed!", ex));
        cullThread.setDaemon(true);
    }

    public void loadConfig() {
        blockEntityWhitelist.clear();
        tickCullWhitelists.clear();
        entityWhitelist.clear();
        initWhitelists();
    }

    public static EntityCulling getInstance() {
        if (instance == null) {
            synchronized (EntityCulling.class) {
                if (instance == null) {
                    new EntityCulling();
                }
            }
        }
        return instance;
    }

    public void clientTick() {
        long start = System.nanoTime();
        debugCollector.tick();
        if (!lateInit) {
            lateInit = true;
            cullThread.start();
            initWhitelists();
        }
        Minecraft client = Minecraft.getInstance();
        boolean ingame = client.level != null && client.player != null && client.player.tickCount > 10;
        if (ingame && enabled) {
            boolean changed = false;
            if (tickCounter++ % Config.CONFIG.captureRate.get() == 0) {
                if (!Config.CONFIG.skipEntityCulling.get()) {
                    List<Entity> entities = new ArrayList<>();
                    for (Entity entity : client.level.entitiesForRendering()) {
                        entities.add(entity);
                    }
                    cullTask.setEntitiesForRendering(entities);
                    debugCollector.getDataHolder().consideredEntities = entities.size();
                }
                if (!Config.CONFIG.skipBlockEntityCulling.get()) {
                    Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
                    for (int x = -8; x <= 8; x++) {
                        for (int z = -8; z <= 8; z++) {
                            LevelChunk chunk = client.level.getChunk(client.player.chunkPosition().x() + x, client.player.chunkPosition().z() + z);
                            blockEntities.putAll(chunk.getBlockEntities());
                        }
                    }
                    cullTask.setBlockEntities(blockEntities);
                    debugCollector.getDataHolder().consideredBlockEntities = blockEntities.size();
                }
                changed = true;
            }
            cullTask.setIngame(true);
            cullTask.setCameraMC(Config.CONFIG.debugMode.get() ? client.player.getEyePosition(0) : client.gameRenderer.mainCamera().position());
            cullTask.requestCull = true;
            if (changed) {
                lastTickTime = (System.nanoTime() - start) / 1_000_000.0;
            }
        } else {
            cullTask.setIngame(false);
            cullTask.setEntitiesForRendering(Collections.emptyList());
            cullTask.setBlockEntities(Collections.emptyMap());
            lastTickTime = (System.nanoTime() - start) / 1_000_000.0;
        }
    }

    public void worldTick() {
        cullTask.requestCull = true;
    }

    private void initWhitelists() {
        for (String id : Config.CONFIG.blockEntityCullingWhitelist.get().split(",")) {
            id = id.trim();
            if (id.isEmpty()) continue;
            Identifier loc = Identifier.tryParse(id);
            if (loc != null) BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(loc).ifPresent(blockEntityWhitelist::add);
        }
        for (String id : Config.CONFIG.tickCullingWhitelist.get().split(",")) {
            id = id.trim();
            if (id.isEmpty()) continue;
            Identifier loc = Identifier.tryParse(id);
            if (loc != null) BuiltInRegistries.ENTITY_TYPE.getOptional(loc).ifPresent(tickCullWhitelists::add);
        }
        for (String id : Config.CONFIG.entityCullingWhitelist.get().split(",")) {
            id = id.trim();
            if (id.isEmpty()) continue;
            Identifier loc = Identifier.tryParse(id);
            if (loc != null) BuiltInRegistries.ENTITY_TYPE.getOptional(loc).ifPresent(entityWhitelist::add);
        }
    }

    public AABB setupAABB(BlockEntity entity, BlockPos pos) {
        if (entity instanceof BannerBlockEntity) {
            return new AABB(pos).inflate(0, 1, 0);
        }
        return new AABB(pos);
    }

    public boolean isDynamicWhitelisted(BlockEntity entity) {
        for (Function<BlockEntity, Boolean> fun : dynamicBlockEntityWhitelist) {
            if (fun.apply(entity)) return true;
        }
        return false;
    }

    public boolean isDynamicWhitelisted(Entity entity) {
        for (Function<Entity, Boolean> fun : dynamicEntityWhitelist) {
            if (fun.apply(entity)) return true;
        }
        return false;
    }

    public void addDynamicBlockEntityWhitelist(Function<BlockEntity, Boolean> function) {
        this.dynamicBlockEntityWhitelist.add(function);
    }

    public void addDynamicEntityWhitelist(Function<Entity, Boolean> function) {
        this.dynamicEntityWhitelist.add(function);
    }

    public boolean isEnabled() {
        return enabled && Config.CONFIG.tickCulling.get();
    }
}
