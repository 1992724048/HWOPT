package com.server.world.worldgen.mixin.chunk;

import com.server.world.worldgen.accessor.NoiseInterpolatorAccessor;
import library.dll.MathNative;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

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

	@Unique
	private static boolean hwopt$hasInputAndTransform(DensityFunction fn) {
		try {
			Method inputM = fn.getClass().getMethod("input");
			Method xformM = fn.getClass().getMethod("transform", double.class);
			return inputM.getReturnType() == DensityFunction.class
				&& xformM.getReturnType() == double.class;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

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
			return;
		}

		if (hwopt$hasInputAndTransform(function)) {
			try {
				DensityFunction inputFn = (DensityFunction) function.getClass().getMethod("input").invoke(function);
				inputFn.fillArray(output, (DensityFunction.ContextProvider) this);
				Method xformM = function.getClass().getMethod("transform", double.class);
				for (int i = 0; i < output.length; i++) {
					output[i] = (double) xformM.invoke(function, output[i]);
				}
				this.arrayIndex = output.length;
				ci.cancel();
			} catch (Exception ignored) {
			}
		}
	}
}
