package com.server.mixin;

import com.server.network.PacketIndexRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public abstract class CustomPacketPayloadCodecMixin {
	
	@Redirect(method = "writeCap", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;writeIdentifier(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/network/FriendlyByteBuf;"), expect = 1)
	private FriendlyByteBuf hwopt$writeIndexed(FriendlyByteBuf buf, Identifier id) {
		PacketIndexRegistry reg = PacketIndexRegistry.INSTANCE;
		int idx = reg.getIndex(id);
		if (idx < 0) {
			reg.captureVanillaPayloads(this);
			idx = reg.getIndex(id);
		}
		if (idx >= 0) {
			buf.writeByte(0);
			buf.writeShort(PacketIndexRegistry.getModIdx(idx));
			buf.writeShort(PacketIndexRegistry.getPktIdx(idx));
			return buf;
		}
		return buf.writeIdentifier(id);
	}
	
	@Redirect(method = "decode", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readIdentifier()Lnet/minecraft/resources/Identifier;"), expect = 1)
	private Identifier hwopt$readIndexed(FriendlyByteBuf buf) {
		int firstByte = buf.getByte(buf.readerIndex()) & 0xFF;
		if (firstByte == 0) {
			buf.readByte();
			int packed = PacketIndexRegistry.pack(buf.readUnsignedShort(), buf.readUnsignedShort());
			PacketIndexRegistry reg = PacketIndexRegistry.INSTANCE;
			Identifier id = reg.getIdentifier(packed);
			if (id == null) {
				reg.captureVanillaPayloads(this);
				id = reg.getIdentifier(packed);
			}
			return id;
		}
		return buf.readIdentifier();
	}
}
