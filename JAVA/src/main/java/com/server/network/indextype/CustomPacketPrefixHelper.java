package com.server.network.indextype;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public class CustomPacketPrefixHelper {
    public static void write(Identifier type, FriendlyByteBuf buf) {
        if (NamespaceIndexManager.contains(type)) {
            int[] idx = NamespaceIndexManager.getCheckedIndex(type);
            buf.writeVarInt(idx[0]);
            buf.writeVarInt(idx[1]);
        } else {
            buf.writeByte(0);
            buf.writeIdentifier(type);
        }
    }

    public static Identifier read(FriendlyByteBuf buf) {
        byte firstByte = buf.getByte(buf.readerIndex());
        if (firstByte == 0) {
            buf.readByte();
            int peek = buf.readableBytes() > 1 ? buf.getByte(buf.readerIndex()) & 0xFF : 0;
            if (peek == 0) {
                buf.readByte();
                return buf.readIdentifier();
            }
            return buf.readIdentifier();
        }
        return NamespaceIndexManager.getIdentifier(buf.readVarInt(), buf.readVarInt());
    }
}
