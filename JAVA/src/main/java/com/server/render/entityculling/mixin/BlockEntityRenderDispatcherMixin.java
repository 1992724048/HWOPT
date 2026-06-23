package com.server.render.entityculling.mixin;

import com.server.render.entityculling.EntityCullingMod;
import com.server.render.entityculling.access.Cullable;
import com.hwpp.mod.Config;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Shadow
    public abstract <E extends BlockEntity> BlockEntityRenderer<E, ?> getRenderer(E blockEntity);

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void hwopt$tryExtractRenderState(BlockEntity blockEntity, float partialTick,
                                              ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                              boolean isGloballyRendered,
                                              CallbackInfoReturnable<Object> ci) {
        if (isGloballyRendered) return;
        EntityCullingMod mod = EntityCullingMod.getInstance();
        if (Config.CONFIG.skipBlockEntityCulling.get()) return;
        if (!EntityCullingMod.enabled || !Config.CONFIG.tickCulling.get()) return;
        BlockEntityRenderer<?, ?> renderer = getRenderer(blockEntity);
        if (renderer == null) return;
        if (renderer.shouldRenderOffScreen()) {
            mod.renderedBlockEntities++;
            return;
        }
        Cullable cullable = (Cullable) blockEntity;
        if (cullable.isCulled()) {
            mod.skippedBlockEntities++;
            ci.setReturnValue(null);
            return;
        }
        mod.renderedBlockEntities++;
    }
}
