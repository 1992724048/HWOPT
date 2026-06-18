package com.hwpp.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkGenStats {
    public static final AtomicLong GEN_START = new AtomicLong(-1);
    public static final AtomicInteger GEN_CHUNKS = new AtomicInteger();
    public static final AtomicInteger ACTIVE_GEN = new AtomicInteger();

    public static void reset() {
        GEN_START.set(-1);
        GEN_CHUNKS.set(0);
        ACTIVE_GEN.set(0);
    }

    private ChunkGenStats() {}
}
