package com.server.mixin.world.render;

import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin {
	
	@ModifyConstant(method = "<init>", constant = @Constant(intValue = 32), require = 1)
	private int hwopt$increasePlayerTicketDistance(int original) {
		return 125;
	}
}
