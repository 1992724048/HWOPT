package com.server.network.mixin;

import com.server.network.chunk.CachedChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public class ServerPlayerChunkTrackingViewMixin {
    @Unique
    private CachedChunkTrackingView hwopt$chunkTrackingView;

    public CachedChunkTrackingView hwopt$getChunkTrackingView() {
        return hwopt$chunkTrackingView;
    }

    public void hwopt$setChunkTrackingView(CachedChunkTrackingView view) {
        this.hwopt$chunkTrackingView = view;
    }
}
