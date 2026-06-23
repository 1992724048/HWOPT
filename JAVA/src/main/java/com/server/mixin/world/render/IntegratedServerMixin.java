package com.server.mixin.world.render;

import com.hwpp.mod.Config;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", remap = false, ordinal = 0))
    private int hwopt$bufferedViewDistance(int a, int b) {
        return Math.max(a, b) + Config.CONFIG.dccBufferDistance.get();
    }
}
