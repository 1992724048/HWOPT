package com.hwpp.mod;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    public boolean logChunkGen;
    public int logChunkGenInterval;
    public boolean spawnAtVillage;

    public boolean tickCulling = true;
    public boolean blockEntityCulling = true;
    public boolean solidLeaves;
    public boolean nametagsThroughWalls;
    public int tracingDistance = 128;
    public int sleepDelay = 10;
    public int hitboxLimit = 50;
    public int captureRate = 5;
    public int maxEntities = 512;

    public boolean asyncParticleTick = true;

    public int netFlushMs = 20;
    public int netMaxBytes = 262144;
    public int netMaxCount = 50;
    public int netMaxPacketSize = 4194304;

    public int dccCacheTimeout = 60;
    public int dccCacheDistance = 8;
    public int dccCacheSizeLimit = 1200;
    public int dccBufferDistance = 8;

    private static Config INSTANCE = new Config();

    private static final String[] BOOL_KEYS = {
        "logChunkGen", "spawnAtVillage", "tickCulling", "blockEntityCulling",
        "solidLeaves", "nametagsThroughWalls", "asyncParticleTick"
    };
    private static final String[] INT_KEYS = {
        "logChunkGenInterval", "tracingDistance", "sleepDelay", "hitboxLimit",
        "captureRate", "maxEntities", "netFlushMs", "netMaxBytes", "netMaxCount",
        "netMaxPacketSize", "dccCacheTimeout", "dccCacheDistance", "dccCacheSizeLimit", "dccBufferDistance"
    };

    public static Config get() {
        return INSTANCE;
    }

    public static void load() {
        var path = getPath().toFile();
        if (!path.exists()) return;
        try {
            TomlParseResult result = Toml.parse(path.toPath());
            result.errors().forEach(e -> HWOPT.LOGGER.error("TOML error: {}", e));
            for (String key : BOOL_KEYS) {
                Boolean v = result.getBoolean(key);
                if (v != null) setField(key, v);
            }
            for (String key : INT_KEYS) {
                Long v = result.getLong(key);
                if (v != null) setField(key, v.intValue());
            }
        } catch (IOException e) {
            HWOPT.LOGGER.error("Failed to load config", e);
        }
    }

    private static void setField(String key, Object val) {
        try {
            Field f = Config.class.getField(key);
            if (val instanceof Boolean b) f.setBoolean(INSTANCE, b);
            else if (val instanceof Integer i) f.setInt(INSTANCE, i);
        } catch (Exception ignored) {}
    }

    public static void save() {
        var path = getPath();
        try (var w = new FileWriter(path.toFile())) {
            var cats = categorize();
            for (var cat : cats.entrySet()) {
                w.write("[" + cat.getKey() + "]\n");
                for (var entry : cat.getValue().entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof Boolean b) w.write(entry.getKey() + " = " + b + "\n");
                    else if (val instanceof Integer i) w.write(entry.getKey() + " = " + i + "\n");
                }
                w.write("\n");
            }
        } catch (Exception e) {
            HWOPT.LOGGER.error("Failed to save config", e);
        }
    }

    private static Map<String, Map<String, Object>> categorize() {
        Map<String, Map<String, Object>> cats = new LinkedHashMap<>();
        cat(cats, "debug", "logChunkGen", "logChunkGenInterval");
        cat(cats, "world", "spawnAtVillage");
        cat(cats, "rendering", "tickCulling", "blockEntityCulling", "solidLeaves", "nametagsThroughWalls",
            "tracingDistance", "sleepDelay", "hitboxLimit", "captureRate", "maxEntities", "asyncParticleTick");
        cat(cats, "network", "netFlushMs", "netMaxBytes", "netMaxCount", "netMaxPacketSize",
            "dccCacheTimeout", "dccCacheDistance", "dccCacheSizeLimit", "dccBufferDistance");
        return cats;
    }

    private static void cat(Map<String, Map<String, Object>> cats, String name, String... fields) {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            for (String f : fields) m.put(f, Config.class.getField(f).get(INSTANCE));
        } catch (Exception ignored) {}
        cats.put(name, m);
    }

    private static Path getPath() {
        return Path.of("config", HWOPT.MODID + ".toml");
    }
}
