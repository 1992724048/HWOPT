package com.server.network.chunk;

import com.hwpp.mod.Config;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CachedChunkTrackingView implements ChunkTrackingView {
    private static final long NO_CACHE = -1;
    private static final Logger LOGGER = LoggerFactory.getLogger("CachedChunkTrackingView");

    private ChunkTrackingView.Positioned major;
    private final Long2LongLinkedOpenHashMap cache = new Long2LongLinkedOpenHashMap();

    public CachedChunkTrackingView(ChunkTrackingView.Positioned major) {
        this.major = major;
        cache.defaultReturnValue(NO_CACHE);
    }

    @Override
    public boolean contains(int x, int z, boolean includeNeighbors) {
        return major.contains(x, z, includeNeighbors) || cache.containsKey(ChunkPos.pack(x, z));
    }

    @Override
    public void forEach(@NotNull Consumer<ChunkPos> consumer) {
        major.forEach(consumer);
        LongIterator it = cache.keySet().iterator();
        while (it.hasNext()) consumer.accept(ChunkPos.unpack(it.nextLong()));
    }

    public interface Context {
        void startChunkTracking(ChunkPos pos);
        void stopChunkTracking(ChunkPos pos);
        void putTicket(ChunkPos pos, int ticks);
    }

    public static void onUpdateChunkTracking(ServerPlayer player, int playerViewDistance, Context context) {
        ChunkTrackingView current = player.getChunkTrackingView();
        ChunkPos playerChunk = player.chunkPosition();
        ChunkTrackingView.Positioned nextPos = null;
        ChunkTrackingView.Positioned lastPos = switch (current) {
            case CachedChunkTrackingView cv -> cv.major;
            case ChunkTrackingView.Positioned p -> p;
            default -> null;
        };
        if (lastPos == null || !lastPos.center().equals(playerChunk) || lastPos.viewDistance() != playerViewDistance) {
            nextPos = new ChunkTrackingView.Positioned(playerChunk, playerViewDistance);
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(playerChunk.x(), playerChunk.z()));
        }
        if (current instanceof CachedChunkTrackingView cv) {
            cv.tick(player, Objects.requireNonNullElse(nextPos, cv.major), context);
        } else if (nextPos != null) {
            CachedChunkTrackingView cv = new CachedChunkTrackingView(nextPos);
            ChunkTrackingView.difference(current, cv, context::startChunkTracking, context::stopChunkTracking);
            player.setChunkTrackingView(cv);
        }
    }

    private void tick(ServerPlayer player, ChunkTrackingView.Positioned next, Context context) {
        long now = System.currentTimeMillis();
        int chunkCacheBufferSize = Config.CONFIG.dccCacheSizeLimit.get();
        int chunkCacheDistance = Config.CONFIG.dccCacheDistance.get();
        int chunkCacheTimeoutSec = Config.CONFIG.dccCacheTimeout.get();
        boolean dccEnabled = Config.CONFIG.dccEnabled.get();
        boolean isDebug = false;
        long chunkCacheTimeoutMs = TimeUnit.SECONDS.toMillis(chunkCacheTimeoutSec);

        if (!major.equals(next)) {
            ChunkTrackingView.difference(major, next, chunkPos -> {
                if (cache.remove(chunkPos.pack()) == NO_CACHE || !dccEnabled) {
                    context.startChunkTracking(chunkPos);
                    if (isDebug) LOGGER.debug("Cache miss at {} in {}'s cache.", chunkPos, player.getName().getString());
                } else {
                    if (isDebug) LOGGER.debug("Cache hit at {} in {}'s cache.", chunkPos, player.getName().getString());
                }
            }, chunkPos -> {
                if (dccEnabled) {
                    context.putTicket(player.chunkPosition(), chunkCacheTimeoutSec * 20);
                    cache.put(chunkPos.pack(), now);
                }
            });

            int cacheEvictDist = next.viewDistance() + chunkCacheDistance;
            enumerate((pos, time) -> {
                int dist = next.center().getChessboardDistance(ChunkPos.getX(pos), ChunkPos.getZ(pos));
                if (dist > cacheEvictDist) {
                    context.stopChunkTracking(ChunkPos.unpack(pos));
                    if (isDebug) LOGGER.debug("Remove {} from {}'s cache: too far.", ChunkPos.unpack(pos), player.getName().getString());
                    return CacheConsumer.REMOVE;
                }
                return CacheConsumer.CONTINUE;
            });
        }

        enumerate((pos, time) -> {
            boolean legacy = time <= now - chunkCacheTimeoutMs;
            if (legacy || cache.size() >= chunkCacheBufferSize) {
                context.stopChunkTracking(ChunkPos.unpack(pos));
                if (isDebug) LOGGER.debug("Remove {} from {}'s cache: {}", ChunkPos.unpack(pos), player.getName().getString(), legacy ? "timeout" : "full");
                return CacheConsumer.REMOVE;
            }
            return CacheConsumer.STOP;
        });

        major = next;
    }

    @FunctionalInterface
    private interface CacheConsumer {
        byte CONTINUE = 0, REMOVE = 1, STOP = 2;
        byte accept(long pos, long time);
    }

    private void enumerate(CacheConsumer consumer) {
        ObjectIterator<Long2LongMap.Entry> it = Long2LongMaps.fastIterator(cache);
        while (it.hasNext()) {
            Long2LongMap.Entry e = it.next();
            byte v = consumer.accept(e.getLongKey(), e.getLongValue());
            if ((v & CacheConsumer.REMOVE) != 0) it.remove();
            if ((v & CacheConsumer.STOP) != 0) return;
        }
    }
}
