package com.worldgen.mixin;

import com.worldgen.util.HWMutableContext;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {

    @Shadow
    private DensityFunction preliminarySurfaceLevel;

    @Unique
    private static final ThreadLocal<HWMutableContext> HWOPT_CTX = ThreadLocal.withInitial(HWMutableContext::new);

    @Inject(method = "computePreliminarySurfaceLevel", at = @At("HEAD"), cancellable = true)
    private void hwopt$optComputePreliminarySurfaceLevel(final long key, final CallbackInfoReturnable<Integer> cir) {

        final int blockX = (int)(key >> 32);
        final int blockZ = (int)(key);

        final HWMutableContext ctx = NoiseChunkMixin.HWOPT_CTX.get();
        ctx.x = blockX;
        ctx.y = 0;
        ctx.z = blockZ;

        final double v = this.preliminarySurfaceLevel.compute(ctx);

        int y = (int)v;
        if (v < y) y--;

        cir.setReturnValue(y);
    }
}