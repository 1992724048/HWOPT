package com.server.network.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Varint21LengthFieldPrepender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Varint21LengthFieldPrepender.class)
public class Varint21LengthFieldPrependerMixin {
    

    @Inject(method = "encode", at = @At("HEAD"))
    private void hwopt$checkPacketSize(ChannelHandlerContext context, ByteBuf input, ByteBuf output, CallbackInfo ci) {
        int size = input.readableBytes();
        if (size > com.hwpp.mod.Config.CONFIG.netMaxPacketSize.get()) {
            throw new IllegalArgumentException("Packet too large: " + size);
        }
    }
}
