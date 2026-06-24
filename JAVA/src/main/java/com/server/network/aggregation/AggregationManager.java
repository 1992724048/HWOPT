package com.server.network.aggregation;

import com.server.network.util.PacketUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.DefaultChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class AggregationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AggrMgr");
    private static final WeakHashMap<Connection, ArrayList<AggregatedEncodePacket>> PACKET_BUFFER = new WeakHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("NEB-Flush").setDaemon(true).build());
    private static final ArrayList<ScheduledFuture<?>> TASKS = new ArrayList<>();
    private static volatile boolean initialized = false;
    private static final java.util.concurrent.atomic.AtomicLong aggregatedPackets = new java.util.concurrent.atomic.AtomicLong();

    public static long getAggregatedPacketCount() { return aggregatedPackets.get(); }

    public static synchronized void init() {
        if (initialized) return;
        initialized = false;
        PACKET_BUFFER.clear();
        TASKS.forEach(t -> t.cancel(false));
        TASKS.clear();
        TASKS.add(TIMER.scheduleAtFixedRate(AggregationManager::flush, 0, 20, TimeUnit.MILLISECONDS));
        initialized = true;
    }

    public static synchronized void takeOver(Packet<?> packet, Connection connection) {
        var type = PacketUtil.getTrueType(packet);
        PACKET_BUFFER.computeIfAbsent(connection, c -> new ArrayList<>()).add(new AggregatedEncodePacket(packet, type));
        aggregatedPackets.incrementAndGet();
    }

    public static synchronized void clearCache(Connection connection) { PACKET_BUFFER.remove(connection); }

    private static synchronized void flush() {
        PACKET_BUFFER.entrySet().removeIf(e -> !e.getKey().isConnected());
        PACKET_BUFFER.forEach(AggregationManager::flushInternal);
    }

    public static synchronized void flushConnection(Connection connection) {
        TIMER.execute(() -> {
            PACKET_BUFFER.entrySet().removeIf(e -> !e.getKey().isConnected());
            flushInternal(connection, PACKET_BUFFER.get(connection));
        });
    }

    private static void flushInternal(Connection connection, ArrayList<AggregatedEncodePacket> packets) {
        try {
            if (packets == null || packets.isEmpty()) return;
            var pipeline = (DefaultChannelPipeline) connection.channel().pipeline();
            var encoder = (net.minecraft.network.PacketEncoder) pipeline.get("encoder");
            if (encoder == null) { LOGGER.error("No encoder for {}", connection.getRemoteAddress()); return; }
            var sendPackets = new ArrayList<>(packets);
            var aggPacket = new PacketAggregationPacket(sendPackets, encoder.getProtocolInfo(), connection);
            connection.send(connection.getSending() == PacketFlow.CLIENTBOUND
                ? new ClientboundCustomPayloadPacket(aggPacket)
                : new ServerboundCustomPayloadPacket(aggPacket), null, true);
            packets.clear();
            connection.flushChannel();
        } catch (Exception e) { LOGGER.error("Flush fail", e); }
    }
}
