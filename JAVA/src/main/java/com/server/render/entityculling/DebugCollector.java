package com.server.render.entityculling;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DebugCollector {
    private boolean requestStart = false;
    private boolean running = false;
    private final DataHolder dataHolder = new DataHolder();

    public void requestStart() {
        this.requestStart = true;
    }

    public boolean isRunning() {
        return running;
    }

    public DataHolder getDataHolder() {
        return dataHolder;
    }

    public void tick() {
        if (requestStart) {
            requestStart = false;
            running = true;
            dataHolder.reset();
        }
        if (running) {
            dataHolder.tickCount.incrementAndGet();
            if (dataHolder.tickCount.get() > 100) {
                running = false;
            }
        }
    }

    public void addEntity(net.minecraft.world.entity.Entity entity, boolean visible, boolean ignored) {
        if (!running) return;
        // data collection
    }

    public static class DataHolder {
        public int consideredEntities = 0;
        public int consideredBlockEntities = 0;
        public final AtomicInteger tickCount = new AtomicInteger(0);

        public void reset() {
            consideredEntities = 0;
            consideredBlockEntities = 0;
            tickCount.set(0);
        }
    }
}
