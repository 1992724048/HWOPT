package com.server.world.worldgen.mixin.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.levelgen.Aquifer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Aquifer.NoiseBasedAquifer.class)
public abstract class AquiferStatusCacheMixin {

	@Unique
	private Long2ObjectOpenHashMap<Aquifer.FluidStatus> hwopt$statusCache;

	@Inject(method = "getAquiferStatus", at = @At("HEAD"), cancellable = true)
	private void hwopt$cachedGetAquiferStatus(int index, CallbackInfoReturnable<Aquifer.FluidStatus> cir) {
		if (hwopt$statusCache == null) {
			hwopt$statusCache = new Long2ObjectOpenHashMap<>();
			return;
		}
		if (hwopt$statusCache.containsKey(index)) {
			cir.setReturnValue(hwopt$statusCache.get(index));
		}
	}

	@Inject(method = "getAquiferStatus", at = @At("TAIL"))
	private void hwopt$afterGetAquiferStatus(int gridIndex, CallbackInfoReturnable<Aquifer.FluidStatus> cir) {
		hwopt$statusCache.put(gridIndex, cir.getReturnValue());
	}
}
