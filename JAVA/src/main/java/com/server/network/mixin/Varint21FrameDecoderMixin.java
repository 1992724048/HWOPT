package com.server.network.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import net.minecraft.network.Varint21FrameDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Varint21FrameDecoder.class)
public class Varint21FrameDecoderMixin {
    

    @Inject(method = "decode", at = @At("HEAD"))
    private void hwopt$checkFrameLength(ChannelHandlerContext context, ByteBuf input, List<Object> out, CallbackInfo ci) {
        int readable = input.readableBytes();
        if (readable <= 0) return;

        int maxSize = com.hwpp.mod.Config.get().netMaxPacketSize;
        int idx = input.readerIndex();
        int frameLen = 0;

        for (int i = 0; i < 3; i++) {
            if (i >= readable) return;
            int b = input.getByte(idx + i);
            frameLen |= (b & 0x7F) << (i * 7);
            if ((b & 0x80) == 0) {
                if (frameLen > maxSize) {
                    throw new CorruptedFrameException("Packet too large: " + frameLen + " > " + maxSize);
                }
                return;
            }
        }
    }
}
