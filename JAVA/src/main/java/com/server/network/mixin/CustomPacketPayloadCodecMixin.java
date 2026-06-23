package com.server.network.mixin;

import com.server.network.indextype.CustomPacketPrefixHelper;
import com.server.network.indextype.NamespaceIndexManager;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public abstract class CustomPacketPayloadCodecMixin {

    @Shadow @Final ConnectionProtocol val$protocol;

    @Redirect(method = "writeCap", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;writeIdentifier(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/network/FriendlyByteBuf;"))
    private FriendlyByteBuf hwopt$writeIndexed(FriendlyByteBuf buf, Identifier id) {
        if (val$protocol != ConnectionProtocol.PLAY || !NamespaceIndexManager.ready()) {
            return buf.writeIdentifier(id);
        }
        CustomPacketPrefixHelper.write(id, buf);
        return buf;
    }

    @Redirect(method = "decode", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readIdentifier()Lnet/minecraft/resources/Identifier;"))
    private Identifier hwopt$readIndexed(FriendlyByteBuf buf) {
        if (val$protocol != ConnectionProtocol.PLAY || !NamespaceIndexManager.ready()) {
            return buf.readIdentifier();
        }
        Identifier id = CustomPacketPrefixHelper.read(buf);
        return id != null ? id : buf.readIdentifier();
    }
}
