package com.server.network.mixin;

import com.server.network.chunk.CachedChunkTrackingView;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow @Final private net.minecraft.server.level.ServerLevel level;
    @Shadow @Final private TicketStorage ticketStorage;
    @Shadow protected abstract int getPlayerViewDistance(ServerPlayer player);
    @Shadow protected abstract void markChunkPendingToSend(ServerPlayer player, ChunkPos pos);
    @Shadow
    private static void dropChunk(ServerPlayer player, ChunkPos pos) {
    }

    @Unique
    private static TicketType CACHE_TICKET;
    @Unique private static final int CACHE_TICKET_LEVEL = 33 + 5;

    @Inject(method = "updateChunkTracking", at = @At("HEAD"), cancellable = true)
    private void hwopt$updateChunkTracking(ServerPlayer player, CallbackInfo ci) {
        if (player.level() != this.level) return;

        CachedChunkTrackingView.onUpdateChunkTracking(player, getPlayerViewDistance(player),
            new CachedChunkTrackingView.Context() {
                @Override public void startChunkTracking(ChunkPos pos) {
                    markChunkPendingToSend(player, pos);
                }
                @Override public void stopChunkTracking(ChunkPos pos) {
                    ChunkMapMixin.dropChunk(player, pos);
                }
                @Override public void putTicket(ChunkPos pos, int ticks) {
                    if (CACHE_TICKET == null || CACHE_TICKET.timeout() != TimeUnit.SECONDS.toMillis(ticks / 20)) {
                        CACHE_TICKET = new TicketType(TimeUnit.SECONDS.toMillis(ticks / 20), 0);
                    }
                    ticketStorage.addTicketWithRadius(CACHE_TICKET, pos, 0);
                }
            });
        ci.cancel();
    }

}
