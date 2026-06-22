package com.server.world.worldgen.mixin.chunk;

import com.server.world.misc.PrecomputedDensity;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {

	@Shadow @Final private int cellWidth;
	@Shadow @Final private int cellHeight;
	@Shadow private int arrayIndex;
	@Shadow private int cellStartBlockX;
	@Shadow private int cellStartBlockY;
	@Shadow private int cellStartBlockZ;
	@Shadow private int inCellX;
	@Shadow private int inCellY;
	@Shadow private int inCellZ;
	@Shadow private long interpolationCounter;

	@Unique
	private static final Map<Class<?>, Method[]> IO_METHOD_CACHE = new ConcurrentHashMap<>();

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

	@Inject(method = "getInterpolatedDensity", at = @At("HEAD"), cancellable = true)
	private void hwopt$getInterpolatedDensity(CallbackInfoReturnable<Double> cir) {
		if (!PrecomputedDensity.isActive()) return;
		NoiseChunkAccessor acc = (NoiseChunkAccessor) this;
		double d = PrecomputedDensity.get(
			acc.cellStartBlockX() + acc.inCellX(),
			acc.cellStartBlockY() + acc.inCellY(),
			acc.cellStartBlockZ() + acc.inCellZ());
		if (!Double.isNaN(d)) cir.setReturnValue(d);
	}

	@Inject(method = "updateForY", at = @At("HEAD"), cancellable = true)
	private void hwopt$skipUpdateForY(int y, double frac, CallbackInfo ci) {
		if (!PrecomputedDensity.isActive()) return;
		this.inCellY = y - this.cellStartBlockY;
		this.interpolationCounter++;
		ci.cancel();
	}

	@Inject(method = "updateForX", at = @At("HEAD"), cancellable = true)
	private void hwopt$skipUpdateForX(int x, double frac, CallbackInfo ci) {
		if (!PrecomputedDensity.isActive()) return;
		this.inCellX = x - this.cellStartBlockX;
		this.interpolationCounter++;
		ci.cancel();
	}

	@Inject(method = "updateForZ", at = @At("HEAD"), cancellable = true)
	private void hwopt$skipUpdateForZ(int z, double frac, CallbackInfo ci) {
		if (!PrecomputedDensity.isActive()) return;
		this.inCellZ = z - this.cellStartBlockZ;
		this.interpolationCounter++;
		ci.cancel();
	}
}
