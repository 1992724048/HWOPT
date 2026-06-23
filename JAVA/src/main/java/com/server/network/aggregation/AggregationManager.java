package com.server.network.aggregation;

import com.hwpp.mod.Config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AggregationManager {
    
    
    

    private static final WeakHashMap<Connection, Batch> BUFFERS = new WeakHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("hwopt-aggr").setDaemon(true).build());
    private static boolean initialized;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        TIMER.scheduleWithFixedDelay(AggregationManager::tick, Config.CONFIG.netFlushMs.get(), Config.CONFIG.netFlushMs.get(), TimeUnit.MILLISECONDS);
    }

    public static void enqueue(Connection conn, Packet<?> packet) {
        synchronized (BUFFERS) {
            var batch = BUFFERS.computeIfAbsent(conn, k -> new Batch());
            batch.packets.add(packet);
            batch.estimatedBytes += 256;
            if (batch.packets.size() >= Config.CONFIG.netMaxCount.get() || batch.estimatedBytes >= Config.CONFIG.netMaxBytes.get()) {
                var toSend = batch.packets;
                BUFFERS.remove(conn);
                flushBatch(conn, toSend);
            }
        }
    }

    public static void flush(Connection conn) {
        List<Packet<?>> packets;
        synchronized (BUFFERS) {
            var batch = BUFFERS.remove(conn);
            packets = batch != null ? batch.packets : null;
        }
        if (packets != null && !packets.isEmpty()) {
            flushBatch(conn, packets);
        }
    }

    public static void release(Connection conn) {
        synchronized (BUFFERS) {
            BUFFERS.remove(conn);
        }
    }

    private static void tick() {
        List<Connection> conns;
        synchronized (BUFFERS) {
            conns = new ArrayList<>(BUFFERS.keySet());
        }
        for (var conn : conns) flush(conn);
    }

    private static void flushBatch(Connection conn, List<Packet<?>> packets) {
        Channel channel = conn.channel();
        if (channel == null) return;
        try {
            for (var pkt : packets) {
                channel.write(pkt);
            }
            channel.flush();
        } catch (Exception ignored) {
        }
    }

    private static class Batch {
        final List<Packet<?>> packets = new ArrayList<>();
        int estimatedBytes;
    }
}
