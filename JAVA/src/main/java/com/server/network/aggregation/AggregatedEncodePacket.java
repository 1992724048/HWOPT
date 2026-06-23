package com.server.network.aggregation;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class AggregatedEncodePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger("AggrEnc");
    public final Identifier type;
    private final boolean isMinecraft;
    private final Packet<?> packet;
    private final CustomPacketPayload payload;
    private static final VarHandle VH_TYPE_GETTER, VH_TO_ID, VH_BY_ID, VH_SERIALIZER;

    static {
        try {
            var lookup = MethodHandles.lookup();
            var c = Class.forName("net.minecraft.network.codec.IdDispatchCodec");
            var priv = MethodHandles.privateLookupIn(c, lookup);
            VH_TYPE_GETTER = priv.findVarHandle(c, "typeGetter", java.util.function.Function.class);
            VH_TO_ID = priv.findVarHandle(c, "toId", it.unimi.dsi.fastutil.objects.Object2IntMap.class);
            VH_BY_ID = priv.findVarHandle(c, "byId", java.util.List.class);
            var entryCls = Class.forName("net.minecraft.network.codec.IdDispatchCodec$Entry");
            VH_SERIALIZER = MethodHandles.privateLookupIn(entryCls, lookup).findVarHandle(entryCls, "serializer", StreamCodec.class);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public AggregatedEncodePacket(Packet<?> p, Identifier type) {
        if (p instanceof ServerboundCustomPayloadPacket(CustomPacketPayload pld)) { isMinecraft=false; packet=null; payload=pld; }
        else if (p instanceof ClientboundCustomPayloadPacket(CustomPacketPayload pld)) { isMinecraft=false; packet=null; payload=pld; }
        else { isMinecraft=true; packet=p; payload=null; }
        this.type = type;
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    public void encode(ByteBuf buf, ProtocolInfo<?> pi, PacketFlow flow) {
        if (isMinecraft) {
            try {
                var codec = pi.codec();
                java.util.function.Function typeGetter = (java.util.function.Function) VH_TYPE_GETTER.get(codec);
                it.unimi.dsi.fastutil.objects.Object2IntMap toId = (it.unimi.dsi.fastutil.objects.Object2IntMap) VH_TO_ID.get(codec);
                java.util.List byId = (java.util.List) VH_BY_ID.get(codec);
                var t = typeGetter.apply(packet);
                int id = toId.getOrDefault(t, -1);
                if (id == -1) { LOGGER.error("Skip unknown packet {}", t); return; }
                var entry = byId.get(id);
                var codec2 = (StreamCodec) VH_SERIALIZER.get(entry);
                codec2.encode(buf, packet);
            } catch (Exception e) { LOGGER.error("Encode vanilla fail", e); }
        } else {
            var codec = (StreamCodec<ByteBuf,CustomPacketPayload>) NetworkRegistry.getCodec(payload.type().id(), ConnectionProtocol.PLAY, flow);
            codec.encode(buf, payload);
        }
    }
}
