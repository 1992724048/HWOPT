package com.server.mixin.world.render;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChunkMap.class)
public abstract class ViewDistanceMixin {
	
	@ModifyConstant(method = "setServerViewDistance", constant = @Constant(intValue = 32), require = 1)
	private int hwopt$increaseViewDistanceCap(int original) {
		return 128;
	}
}
