package com.server.network.chunk;

import com.hwpp.mod.Config;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

import java.util.function.Consumer;

public class CachedChunkTrackingView implements ChunkTrackingView {
    private final ChunkTrackingView.Positioned inner;
    private final Long2ObjectLinkedOpenHashMap<CacheEntry> cache = new Long2ObjectLinkedOpenHashMap<>();

    public CachedChunkTrackingView(ChunkTrackingView.Positioned inner) {
        this.inner = inner;
    }

    public ChunkTrackingView.Positioned inner() {
        return inner;
    }

    @Override
    public boolean contains(int chunkX, int chunkZ, boolean includeNeighbors) {
        return inner.contains(chunkX, chunkZ, includeNeighbors)
            || cache.containsKey(ChunkPos.pack(chunkX, chunkZ));
    }

    @Override
    public void forEach(Consumer<ChunkPos> consumer) {
        inner.forEach(consumer);
        var it = cache.keySet().iterator();
        while (it.hasNext()) {
            long packed = it.nextLong();
            consumer.accept(new ChunkPos(ChunkPos.getX(packed), ChunkPos.getZ(packed)));
        }
    }

    public void tick(ChunkTrackingView.Positioned next) {
        var cfg = Config.get();
        long timeout = cfg.dccCacheTimeout * 1000L;
        long now = System.currentTimeMillis();
        int cx = next.center().x(), cz = next.center().z();

        ChunkTrackingView.difference(inner, next, pos -> {
            long key = ChunkPos.pack(pos.x(), pos.z());
            cache.remove(key);
        }, pos -> {
            int dist = Math.max(Math.abs(pos.x() - cx), Math.abs(pos.z() - cz));
            if (dist <= cfg.dccCacheDistance) {
                cache.putAndMoveToLast(
                    ChunkPos.pack(pos.x(), pos.z()),
                    new CacheEntry(now + timeout)
                );
            }
        });

        var it = cache.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (now > entry.getValue().expireAt() || cache.size() > cfg.dccCacheSizeLimit) {
                it.remove();
            }
        }
    }

    private record CacheEntry(long expireAt) {}
}
