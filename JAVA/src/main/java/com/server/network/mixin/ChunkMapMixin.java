package com.server.network.mixin;

import com.server.network.chunk.CachedChunkTrackingView;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Inject(method = "applyChunkTrackingView", at = @At("HEAD"), cancellable = true)
    private void hwopt$onApplyChunkTrackingView(ServerPlayer player, ChunkTrackingView newView, CallbackInfo ci) {
        if (!(newView instanceof ChunkTrackingView.Positioned positioned)) return;

        ChunkTrackingView oldView = player.getChunkTrackingView();
        CachedChunkTrackingView cached;

        if (oldView instanceof CachedChunkTrackingView existing) {
            cached = existing;
            existing.tick(positioned);
        } else {
            cached = new CachedChunkTrackingView(positioned);
        }

        player.setChunkTrackingView(cached);
        ci.cancel();
    }
}
