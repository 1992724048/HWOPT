package com.server.world.worldgen.mixin.chunk;

import com.server.world.worldgen.accessor.NoiseChunkAccessor;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

	@Shadow
	@Final
	private List<NoiseChunk.NoiseInterpolator> interpolators;

	@Unique
	private static final Map<Class<?>, Method[]> IO_METHOD_CACHE = new ConcurrentHashMap<>();

	@Unique
	private static double[] hwopt$precomputedCache;

	@Unique
	public static void hwopt$setPrecomputedCache(double[] data) {
		hwopt$precomputedCache = data;
	}

	@Unique
	private static Method[] hwopt$getInputTransformMethods(DensityFunction fn) {
		Class<?> clazz = fn.getClass();
		return IO_METHOD_CACHE.computeIfAbsent(clazz, c -> {
			try {
				Method input = c.getMethod("input");
				Method transform = c.getMethod("transform", double.class);
				if (input.getReturnType() == DensityFunction.class
					&& transform.getReturnType() == double.class) {
					return new Method[]{input, transform};
				}
			} catch (NoSuchMethodException _) {
			}
			return null;
		});
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

		Method[] iom = hwopt$getInputTransformMethods(function);
		if (iom != null) {
			try {
				DensityFunction inputFn = (DensityFunction) iom[0].invoke(function);
				inputFn.fillArray(output, (DensityFunction.ContextProvider) this);
				for (int i = 0; i < output.length; i++) {
					output[i] = (double) iom[1].invoke(function, output[i]);
				}
				this.arrayIndex = output.length;
				ci.cancel();
			} catch (Exception ignored) {
			}
		}
	}

	@Inject(method = "fillSlice", at = @At("HEAD"), cancellable = true)
	private void hwopt$skipFillSlice(boolean useSlice0, int cellX, CallbackInfo ci) {
		if (hwopt$precomputedCache == null) return;
		NoiseChunkAccessor acc = (NoiseChunkAccessor) this;
		NoiseInterpolatorAccessor nia = (NoiseInterpolatorAccessor) this.interpolators.get(0);
		double[][] slice = useSlice0 ? nia.slice0() : nia.slice1();
		int numZ = acc.cellCountXZ() + 1;
		int numY = acc.cellCountY() + 1;
		int cx = cellX - acc.firstCellX();
		if (cx < 0) return;
		int base = cx * numZ * numY;
		for (int cz = 0; cz < numZ; cz++) {
			System.arraycopy(hwopt$precomputedCache, base + cz * numY, slice[cz], 0, numY);
		}
		ci.cancel();
	}
}
