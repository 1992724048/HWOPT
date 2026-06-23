package com.server.network.aggregation;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class AggregatedDecodePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger("AggrDec");
    private final Identifier type;
    private final ByteBuf data;
    private static final Object2IntArrayMap<Identifier> VANILLA_TO_ID = new Object2IntArrayMap<>();
    private static final VarHandle VH_TO_ID, VH_BY_ID, VH_SERIALIZER;
    static { VANILLA_TO_ID.defaultReturnValue(-1); }

    static {
        try {
            var lookup = MethodHandles.lookup();
            var c = Class.forName("net.minecraft.network.codec.IdDispatchCodec");
            var priv = MethodHandles.privateLookupIn(c, lookup);
            VH_TO_ID = priv.findVarHandle(c, "toId", it.unimi.dsi.fastutil.objects.Object2IntMap.class);
            VH_BY_ID = priv.findVarHandle(c, "byId", java.util.List.class);
            var entryCls = Class.forName("net.minecraft.network.codec.IdDispatchCodec$Entry");
            VH_SERIALIZER = MethodHandles.privateLookupIn(entryCls, lookup).findVarHandle(entryCls, "serializer", StreamCodec.class);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public AggregatedDecodePacket(Identifier type, ByteBuf data) { this.type = type; this.data = data; }

    @SuppressWarnings({"rawtypes","unchecked"})
    public void handle(ProtocolInfo<?> pi, IPayloadContext ctx) {
        try {
            var codec = pi.codec();
            var toId = (it.unimi.dsi.fastutil.objects.Object2IntMap) VH_TO_ID.get(codec);
            var byId = (java.util.List) VH_BY_ID.get(codec);
            if (toId.size() != VANILLA_TO_ID.size()) {
                VANILLA_TO_ID.clear();
                var entries = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<java.lang.Object>[]) (Object) toId.object2IntEntrySet().toArray();
                for (var e : entries) VANILLA_TO_ID.put(((PacketType) e.getKey()).id(), e.getIntValue());
            }
            int id = VANILLA_TO_ID.getInt(type);
            if (id != -1) {
                var entry = byId.get(id);
                var streamCodec = (StreamCodec) VH_SERIALIZER.get(entry);
                var pkt = (Packet) streamCodec.decode(data);
                ctx.enqueueWork(() -> pkt.handle(ctx.listener()));
                return;
            }
        } catch (Exception e) { LOGGER.error("Vanilla decode fail", e); }
        var codec = (StreamCodec<ByteBuf,CustomPacketPayload>) NetworkRegistry.getCodec(type, ConnectionProtocol.PLAY, ctx.flow());
        if (codec == null) { LOGGER.error("No codec for {}", type); return; }
        try {
            var pkt = codec.decode(data);
            if (ctx.listener() instanceof ServerCommonPacketListener l) l.handleCustomPayload(new ServerboundCustomPayloadPacket(pkt));
            else if (ctx.listener() instanceof ClientCommonPacketListener l) l.handleCustomPayload(new ClientboundCustomPayloadPacket(pkt));
        } catch (Exception e) { LOGGER.error("Handle fail {}", type, e); }
    }

    public ByteBuf getData() { return data; }
}
