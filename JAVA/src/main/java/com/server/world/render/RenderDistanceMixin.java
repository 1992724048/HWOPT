package com.server.world.render;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Options.class)
public abstract class RenderDistanceMixin {

	@Redirect(method = "<init>", at = @At(value = "NEW", target = "Lnet/minecraft/client/OptionInstance$IntRange;"), require = 2)
	private OptionInstance.IntRange hwopt$increaseRenderDistanceCap(int min, int max, boolean encodeAsBool) {
		return new OptionInstance.IntRange(min, 128, encodeAsBool);
	}
}
