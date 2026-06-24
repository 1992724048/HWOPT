package com.server.network.aggregation;

import com.server.network.indextype.CustomPacketPrefixHelper;
import com.server.network.zstd.ZstdHelper;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

public class PacketAggregationPacket implements CustomPacketPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger("PacketAggr");
    public static final CustomPacketPayload.Type<PacketAggregationPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("hwopt", "packet_aggregation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAggregationPacket> CODEC =
        new StreamCodec<>() {
            @Override public void encode(RegistryFriendlyByteBuf buf, PacketAggregationPacket p) { p.encode(buf); }
            @Override public PacketAggregationPacket decode(RegistryFriendlyByteBuf buf) { return new PacketAggregationPacket(buf); }
        };
    @Override public CustomPacketPayload.Type<PacketAggregationPacket> type() { return TYPE; }

    private int bakedSize;
    private final ArrayList<AggregatedEncodePacket> packetsToEncode;
    private final ProtocolInfo<?> protocolInfo;
    private Connection connection;
    private RegistryFriendlyByteBuf data;

    public PacketAggregationPacket(ArrayList<AggregatedEncodePacket> p, ProtocolInfo<?> pi, Connection c) { packetsToEncode=p; protocolInfo=pi; connection=c; data=null; }
    public PacketAggregationPacket(RegistryFriendlyByteBuf buf) { packetsToEncode=null; protocolInfo=null; connection=null; data=new RegistryFriendlyByteBuf(buf.retainedDuplicate(), buf.registryAccess(), buf.getConnectionType()); buf.readerIndex(buf.writerIndex()); }

    public void encode(RegistryFriendlyByteBuf buf) {
        var raw = new RegistryFriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer(), buf.registryAccess(), buf.getConnectionType());
        for (var p : packetsToEncode) {
            var d = new RegistryFriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer(), raw.registryAccess(), raw.getConnectionType());
            try { p.encode(d, protocolInfo, connection.getSending()); } catch (Exception e) { d.release(); continue; }
            CustomPacketPrefixHelper.write(p.type, raw);
            raw.writeVarInt(d.readableBytes()); raw.writeBytes(d); d.release();
        }
        int rawSize = raw.readableBytes();
        boolean compress = rawSize >= 32;
        buf.writeBoolean(compress);
        if (compress) {
            buf.writeVarInt(rawSize);
            var c = ZstdHelper.compress(connection, raw);
            if (c != null) { buf.writeBytes(c); c.release(); bakedSize = c.readableBytes(); }
            else { buf.writeBytes(raw); bakedSize = rawSize; }
        } else { buf.writeBytes(raw); bakedSize = rawSize; }
        raw.release();
    }

    public void handle(IPayloadContext ctx) {
        connection = ctx.connection();
        boolean compressed = data.readBoolean();
        RegistryFriendlyByteBuf raw;
        if (compressed) {
            int size = data.readVarInt();
            var dec = ZstdHelper.decompress(connection, data.retainedDuplicate(), size);
            if (dec == null) { LOGGER.error("Decompress fail"); data.release(); return; }
            raw = new RegistryFriendlyByteBuf(dec, data.registryAccess(), data.getConnectionType());
        } else { raw = new RegistryFriendlyByteBuf(data.retain(), data.registryAccess(), data.getConnectionType()); }
        var inProto = ctx.connection().getInboundProtocol();
        while (raw.readableBytes() > 0) {
            var t = CustomPacketPrefixHelper.read(raw);
            var s = raw.readVarInt();
            var pd = new RegistryFriendlyByteBuf(raw.readRetainedSlice(s), data.registryAccess(), data.getConnectionType());
            new AggregatedDecodePacket(t, pd).handle(inProto, ctx);
            pd.release();
        }
        data.release(); raw.release();
    }

    public int getBakedSize() { return bakedSize; }
    public void setBakedSize(int s) { bakedSize = s; }
}
