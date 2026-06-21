package com.server.world.worldgen.mixin.noise;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Spline")
public abstract class SplineMixin {

	@Shadow @Final
	private CubicSpline<DensityFunction.FunctionContext> spline;

	@Unique
	private static final ThreadLocal<Int2FloatOpenHashMap> SPLINE_CACHE = ThreadLocal.withInitial(() -> {
		Int2FloatOpenHashMap m = new Int2FloatOpenHashMap();
		m.defaultReturnValue(Float.NaN);
		return m;
	});

	@Overwrite
	public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
		int n = output.length;

		CubicSpline<?> raw = this.spline;
		if (raw instanceof CubicSpline.Multipoint<?> mp) {
			Object coordRaw = mp.coordinate();
			it.unimi.dsi.fastutil.floats.Float2FloatFunction coord = (it.unimi.dsi.fastutil.floats.Float2FloatFunction) coordRaw;
			Int2FloatOpenHashMap cache = SPLINE_CACHE.get();
			cache.clear();

			for (int i = 0; i < n; i++) {
				DensityFunction.FunctionContext ctx = contextProvider.forIndex(i);
				float input = coord.get(ctx);
				int key = Float.floatToIntBits(input);
				float cached = cache.get(key);
				if (Float.isNaN(cached)) {
					cached = CubicSpline.sample((CubicSpline) raw, ctx);
					cache.put(key, cached);
				}
				output[i] = cached;
			}
			return;
		}

		for (int i = 0; i < n; i++) {
			output[i] = CubicSpline.sample((CubicSpline) raw, contextProvider.forIndex(i));
		}
	}
}
