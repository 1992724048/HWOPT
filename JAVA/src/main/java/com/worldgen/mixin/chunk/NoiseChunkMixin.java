package com.worldgen.mixin.chunk;

import com.worldgen.accessor.NoiseInterpolatorAccessor;
import library.dll.MathNative;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {

	@Shadow
	@Final
	private int cellWidth;

	@Shadow
	@Final
	private int cellHeight;

	@Shadow
	private int arrayIndex;

	@Inject(method = "fillAllDirectly", at = @At("HEAD"), cancellable = true)
	private void hwopt$fillAllDirectly(double[] output, DensityFunction function, CallbackInfo ci) {
		if (function instanceof NoiseChunk.NoiseInterpolator interp) {
			NoiseInterpolatorAccessor acc = (NoiseInterpolatorAccessor) interp;
			MathNative.instance().batch_trilerp(
				acc.noise000(), acc.noise100(), acc.noise010(), acc.noise110(),
				acc.noise001(), acc.noise101(), acc.noise011(), acc.noise111(),
				this.cellWidth, this.cellHeight, output
			);
			this.arrayIndex = output.length;
			ci.cancel();
		}
	}
}
