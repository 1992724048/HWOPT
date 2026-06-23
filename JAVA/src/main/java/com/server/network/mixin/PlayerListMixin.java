package com.server.network.mixin;

import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    

    @ModifyVariable(method = "setViewDistance", at = @At("HEAD"), argsOnly = true)
    private int hwopt$extendViewDistance(int viewDistance) {
        return viewDistance + com.hwpp.mod.Config.get().dccBufferDistance;
    }
}
