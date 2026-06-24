package com.hwpp.mod;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
	public static final Config CONFIG;
	public static final ModConfigSpec SPEC;
	
	// Debug
	public final ModConfigSpec.BooleanValue logChunkGen;
	public final ModConfigSpec.IntValue logChunkGenInterval;
	
	// World
	public final ModConfigSpec.BooleanValue spawnAtVillage;
	
	// Rendering - Entity Culling
	public final ModConfigSpec.BooleanValue tickCulling;
	public final ModConfigSpec.BooleanValue solidLeaves;
	public final ModConfigSpec.IntValue tracingDistance;
	public final ModConfigSpec.IntValue sleepDelay;
	public final ModConfigSpec.IntValue hitboxLimit;
	public final ModConfigSpec.IntValue captureRate;
	public final ModConfigSpec.BooleanValue skipEntityCulling;
	public final ModConfigSpec.BooleanValue skipBlockEntityCulling;
	public final ModConfigSpec.BooleanValue forceDisplayCulling;
	public final ModConfigSpec.BooleanValue debugMode;
	public final ModConfigSpec.ConfigValue<java.util.List<? extends String>> blockEntityCullingWhitelist;
	public final ModConfigSpec.ConfigValue<java.util.List<? extends String>> entityCullingWhitelist;
	public final ModConfigSpec.ConfigValue<java.util.List<? extends String>> tickCullingWhitelist;
	
	// Entity - Collision
	public final ModConfigSpec.IntValue entityDensityThreshold;

	// Rendering - Particle
	public final ModConfigSpec.BooleanValue asyncParticleTick;
	public final ModConfigSpec.BooleanValue particleLightCache;
	public final ModConfigSpec.IntValue particleLimit;
	public final ModConfigSpec.BooleanValue removeIfMissedTick;
	public final ModConfigSpec.BooleanValue parallelQueueRemoval;
	public final ModConfigSpec.BooleanValue parallelQueueEviction;
	
	// Network - Aggregation
	public final ModConfigSpec.IntValue netFlushMs;
	public final ModConfigSpec.IntValue netMaxBytes;
	public final ModConfigSpec.IntValue netMaxCount;
	public final ModConfigSpec.IntValue netMaxPacketSize;
	
	// Network - Chunk Cache
	public final ModConfigSpec.BooleanValue dccEnabled;
	public final ModConfigSpec.IntValue dccCacheTimeout;
	public final ModConfigSpec.IntValue dccCacheDistance;
	public final ModConfigSpec.IntValue dccCacheSizeLimit;
	public final ModConfigSpec.IntValue dccBufferDistance;
	
	private Config(ModConfigSpec.Builder builder) {
		builder.push("debug").translation("hwopt.configuration.debug");
		logChunkGen = builder.comment("Log chunk generation timing stats to console").translation("hwopt.configuration.debug.logChunkGen").define("logChunkGen", false);
		logChunkGenInterval = builder.comment("Print stats every N chunks (0 = only at end of burst)").translation("hwopt.configuration.debug.logChunkGenInterval").defineInRange("logChunkGenInterval", 0, 0, Integer.MAX_VALUE);
		builder.pop();
		
		builder.push("world").translation("hwopt.configuration.world");
		spawnAtVillage = builder.comment("Move world spawn to the nearest village on world load").translation("hwopt.configuration.world.spawnAtVillage").define("spawnAtVillage", false);
		builder.pop();
		
		builder.push("entity").translation("hwopt.config.category.entity");
		entityDensityThreshold = builder.comment("Minimum collision partners to use cached entity push; below this threshold push is skipped for performance").translation("hwopt.config.entityDensityThreshold").defineInRange("entityDensityThreshold", 2, 0, 32);
		builder.pop();

		builder.push("rendering").translation("hwopt.config.category.rendering");
		builder.push("entity_culling").translation("hwopt.config.group.entityCulling");
		tickCulling = builder.comment("Cull entity ticking when behind walls").translation("hwopt.config.tickCulling").define("tickCulling", true);
		solidLeaves = builder.comment("Treat leaves as solid blocks for occlusion culling").translation("hwopt.config.solidLeaves").define("solidLeaves", false);
		tracingDistance = builder.comment("Maximum distance (blocks) for entity tracing/culling").translation("hwopt.config.tracingDistance").defineInRange("tracingDistance", 128, 16, 512);
		sleepDelay = builder.comment("Sleep delay (ms) between culling task iterations").translation("hwopt.config.sleepDelay").defineInRange("sleepDelay", 10, 0, 1000);
		hitboxLimit = builder.comment("Hitbox size limit for culling (skip oversized entities)").translation("hwopt.config.hitboxLimit").defineInRange("hitboxLimit", 50, 1, 500);
		captureRate = builder.comment("Entity capture rate (ticks between capture rounds)").translation("hwopt.config.captureRate").defineInRange("captureRate", 5, 1, 100);
		skipEntityCulling = builder.comment("Skip entity culling entirely").translation("hwopt.config.skipEntityCulling").define("skipEntityCulling", false);
		skipBlockEntityCulling = builder.comment("Skip block entity culling entirely").translation("hwopt.config.skipBlockEntityCulling").define("skipBlockEntityCulling", false);
		forceDisplayCulling = builder.comment("Force culling for Display entities with zero bounding box").translation("hwopt.config.forceDisplayCulling").define("forceDisplayCulling", false);
		debugMode = builder.comment("Use player eye position instead of camera for culling checks").translation("hwopt.config.debugMode").define("debugMode", false);
		blockEntityCullingWhitelist = builder.comment("Block entity IDs to exclude from culling").translation("hwopt.config.blockEntityCullingWhitelist").defineList("blockEntityCullingWhitelist", java.util.Arrays.asList("betterend:eternal_pedestal", "botania:falling_star", "botania:flame_ring", "botania:magic_missile", "create:hose_pulley", "create:rope_pulley", "minecraft:beacon"), s -> s instanceof String);
		entityCullingWhitelist = builder.comment("Entity IDs to exclude from culling").translation("hwopt.config.entityCullingWhitelist").defineList("entityCullingWhitelist", java.util.Arrays.asList("botania:mana_burst", "drg_flares:drg_flares", "quark:soul_bead"), s -> s instanceof String);
		tickCullingWhitelist = builder.comment("Entity IDs to exclude from tick culling").translation("hwopt.config.tickCullingWhitelist").defineList("tickCullingWhitelist", java.util.Arrays.asList("alexscaves:gum_worm", "alexscaves:gum_worm_segment", "avm_staff:campfire_flame", "minecraft:acacia_boat", "minecraft:acacia_chest_boat", "minecraft:bamboo_chest_raft", "minecraft:bamboo_raft", "minecraft:birch_boat", "minecraft:birch_chest_boat", "minecraft:block_display", "minecraft:boat", "minecraft:cherry_boat", "minecraft:cherry_chest_boat", "minecraft:dark_oak_boat", "minecraft:dark_oak_chest_boat", "minecraft:firework_rocket", "minecraft:item_display", "minecraft:jungle_boat", "minecraft:jungle_chest_boat", "minecraft:mangrove_boat", "minecraft:mangrove_chest_boat", "minecraft:oak_boat", "minecraft:oak_chest_boat", "minecraft:pale_oak_boat", "minecraft:pale_oak_chest_boat", "minecraft:spruce_boat", "minecraft:spruce_chest_boat", "minecraft:text_display"), s -> s instanceof String);
		builder.pop();
		
		builder.push("particle").translation("hwopt.config.group.particle");
		asyncParticleTick = builder.comment("Tick particles asynchronously on a separate thread pool").translation("hwopt.config.asyncParticleTick").define("asyncParticleTick", true);
		particleLightCache = builder.comment("Cache particle light coordinates to avoid repeated chunk lookups").translation("hwopt.config.particleLightCache").define("particleLightCache", true);
		particleLimit = builder.comment("Maximum particles per particle type group").translation("hwopt.config.particleLimit").defineInRange("particleLimit", 16384, 4096, 262144);
		removeIfMissedTick = builder.comment("Remove particles that missed their async tick").translation("hwopt.config.removeIfMissedTick").define("removeIfMissedTick", true);
		parallelQueueRemoval = builder.comment("Use parallel queue removal for dead particles").translation("hwopt.config.parallelQueueRemoval").define("parallelQueueRemoval", false);
		parallelQueueEviction = builder.comment("Use parallel eviction during queue removal").translation("hwopt.config.parallelQueueEviction").define("parallelQueueEviction", false);
		builder.pop();
		builder.pop();
		
		builder.push("network").translation("hwopt.config.category.network");
		builder.push("aggregation").translation("hwopt.config.group.aggregation");
		netFlushMs = builder.comment("Packet aggregation flush interval in milliseconds").translation("hwopt.config.netFlushMs").defineInRange("netFlushMs", 20, 5, 100);
		netMaxBytes = builder.comment("Maximum bytes in a batched packet before flushing").translation("hwopt.config.netMaxBytes").defineInRange("netMaxBytes", 262144, 16384, 1048576);
		netMaxCount = builder.comment("Maximum packet count in a batch before flushing").translation("hwopt.config.netMaxCount").defineInRange("netMaxCount", 50, 5, 200);
		netMaxPacketSize = builder.comment("Maximum size (bytes) for a single packet; larger packets are rejected").translation("hwopt.config.netMaxPacketSize").defineInRange("netMaxPacketSize", 4194304, 262144, 16777216);
		builder.pop();
		
		builder.push("chunk_cache").translation("hwopt.config.group.dcc");
		dccEnabled = builder.comment("Enable delayed chunk caching to reduce chunk loading churn").translation("hwopt.config.dccEnabled").define("dccEnabled", true);
		dccCacheTimeout = builder.comment("Chunk cache entry timeout in seconds").translation("hwopt.config.dccCacheTimeout").defineInRange("dccCacheTimeout", 60, 10, 300);
		dccCacheDistance = builder.comment("Distance (chunks) to keep cached chunks").translation("hwopt.config.dccCacheDistance").defineInRange("dccCacheDistance", 8, 2, 32);
		dccCacheSizeLimit = builder.comment("Maximum number of cached chunks before eviction").translation("hwopt.config.dccCacheSizeLimit").defineInRange("dccCacheSizeLimit", 1200, 100, 5000);
		dccBufferDistance = builder.comment("Extra buffer distance (chunks) beyond view distance for pre-loading").translation("hwopt.config.dccBufferDistance").defineInRange("dccBufferDistance", 8, 2, 32);
		builder.pop();
		builder.pop();
	}
	
	static {
		Pair<Config, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Config::new);
		CONFIG = pair.getLeft();
		SPEC = pair.getRight();
	}
}
