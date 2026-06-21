package com.server.world.worldgen.mixin.noise;

import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Ap2")
public abstract class Ap2Mixin {
	
	@Unique
	private static final Field ARG1_FIELD;
	@Unique
	private static final Field ARG2_FIELD;
	@Unique
	private static final Field TYPE_FIELD;

	@Unique
	private static final ThreadLocal<double[]> V2_CACHE = new ThreadLocal<>();

	static {
		Field a1 = null, a2 = null, t = null;
		try {
			Class<?> clazz = Class.forName("net.minecraft.world.level.levelgen.DensityFunctions$Ap2");
			a1 = clazz.getDeclaredField("argument1");
			a2 = clazz.getDeclaredField("argument2");
			t = clazz.getDeclaredField("type");
		} catch (Exception ignored) {
		}
		ARG1_FIELD = a1;
		ARG2_FIELD = a2;
		TYPE_FIELD = t;
	}
	
	@Inject(method = "fillArray([DLnet/minecraft/world/level/levelgen/DensityFunction$ContextProvider;)V", at = @At("HEAD"), cancellable = true)
	private void hwopt$fillArray(double[] output, DensityFunction.ContextProvider contextProvider, CallbackInfo ci) {
		if (ARG1_FIELD == null || ARG2_FIELD == null || TYPE_FIELD == null) return;
		try {
			DensityFunction arg1 = (DensityFunction) ARG1_FIELD.get(this);
			DensityFunction arg2 = (DensityFunction) ARG2_FIELD.get(this);
			Object type = TYPE_FIELD.get(this);
			int ordinal = ((Enum<?>) type).ordinal();
			
			arg1.fillArray(output, contextProvider);
			
			double[] v2 = V2_CACHE.get();
			if (v2 == null || v2.length != output.length || v2 == output) {
				v2 = new double[output.length];
				V2_CACHE.set(v2);
			}
			arg2.fillArray(v2, contextProvider);
			
			switch (ordinal) {
				case 0: // ADD
					for (int i = 0; i < output.length; i++) output[i] += v2[i];
					break;
				case 1: // MUL
					for (int i = 0; i < output.length; i++) {
						double v = output[i];
						output[i] = v == 0.0 ? 0.0 : v * v2[i];
					}
					break;
				case 2: // MIN
					double min = arg2.minValue();
					for (int i = 0; i < output.length; i++) {
						double v = output[i];
						output[i] = v < min ? v : Math.min(v, v2[i]);
					}
					break;
				case 3: // MAX
					double max = arg2.maxValue();
					for (int i = 0; i < output.length; i++) {
						double v = output[i];
						output[i] = v > max ? v : Math.max(v, v2[i]);
					}
					break;
			}
			ci.cancel();
		} catch (Exception ignored) {
		}
	}
}
