package com.server.network.util;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class PacketUtil {
    public static Identifier getTrueType(Packet<?> packet) {
        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload p)) return p.type().id();
        if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload p)) return p.type().id();
        return packet.type().id();
    }

    public static Object getTruePacket(Packet<?> packet) {
        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload p)) return p;
        if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload p)) return p;
        return packet;
    }
}
