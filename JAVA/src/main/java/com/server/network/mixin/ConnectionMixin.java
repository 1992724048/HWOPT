package com.server.network.mixin;

import com.server.network.aggregation.AggregationManager;
import io.netty.channel.local.LocalAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@Mixin(Connection.class)
public class ConnectionMixin {
    static {
        AggregationManager.init();
    }
    @Unique
    private static final Set<Class<?>> hwopt$bypassPackets = new HashSet<>();

    static {
        hwopt$bypassPackets.add(ClientboundKeepAlivePacket.class);
        hwopt$bypassPackets.add(ClientboundPlayerPositionPacket.class);
        hwopt$bypassPackets.add(ClientboundLoginPacket.class);
        hwopt$bypassPackets.add(ClientboundSystemChatPacket.class);
        hwopt$bypassPackets.add(ServerboundChatPacket.class);
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            at = @At("HEAD"), cancellable = true)
    private void hwopt$aggregatePacket(Packet<?> packet, @Nullable PacketSendListener listener, CallbackInfo ci) {
        Connection self = (Connection) (Object) this;
        if (self.getRemoteAddress() instanceof LocalAddress) return;
        if (packet.isTerminal() || hwopt$bypassPackets.contains(packet.getClass())) {
            AggregationManager.flush(self);
            return;
        }
        AggregationManager.enqueue(self, packet);
        ci.cancel();
    }

    @Inject(method = "handleDisconnection", at = @At("HEAD"))
    private void hwopt$onDisconnect(CallbackInfo ci) {
        AggregationManager.release((Connection) (Object) this);
    }
}
