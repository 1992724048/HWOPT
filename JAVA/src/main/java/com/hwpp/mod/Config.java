package com.hwpp.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

public enum Config {
    ;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("debug");
        LOG_CHUNK_GEN = BUILDER
                .comment("Print chunk generation timing stats to chat")
                .translation("hwopt.configuration.debug.logChunkGen")
                .define("logChunkGen", false);
        LOG_CHUNK_GEN_INTERVAL = BUILDER
                .comment("Print interval in chunks (0 = only at end of burst)")
                .translation("hwopt.configuration.debug.logChunkGenInterval")
                .defineInRange("logChunkGenInterval", 0, 0, 10000);
        BUILDER.pop();

        BUILDER.push("world");
        SPAWN_AT_VILLAGE = BUILDER
                .comment("Move world spawn to the nearest village")
                .translation("hwopt.configuration.world.spawnAtVillage")
                .define("spawnAtVillage", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static final ModConfigSpec.BooleanValue LOG_CHUNK_GEN;
    public static final ModConfigSpec.IntValue LOG_CHUNK_GEN_INTERVAL;
    public static final ModConfigSpec.BooleanValue SPAWN_AT_VILLAGE;
    public static final ModConfigSpec SPEC;
}
