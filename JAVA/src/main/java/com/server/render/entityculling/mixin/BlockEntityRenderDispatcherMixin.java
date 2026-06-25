package com.server.render.entityculling.mixin;

import com.hwpp.mod.Config;
import com.server.render.entityculling.EntityCulling;
import com.server.render.entityculling.occlusion.HardwareOcclusionEngine;
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

    @Inject(method = "tryExtractRenderState*", at = @At("HEAD"), cancellable = true)
    private void hwopt$tryExtractRenderState(BlockEntity blockEntity, float partialTicks,
                                              ModelFeatureRenderer.CrumblingOverlay breakProgress,
                                              boolean isGloballyRendered,
                                              CallbackInfoReturnable<Object> ci) {
        if (isGloballyRendered) return;
        if (Config.CONFIG.skipBlockEntityCulling.get()) return;
        if (!EntityCulling.enabled || !Config.CONFIG.tickCulling.get()) return;

        BlockEntityRenderer<?, ?> renderer = getRenderer(blockEntity);
        if (renderer == null) return;
        if (renderer.shouldRenderOffScreen()) {
            EntityCulling.getInstance().renderedBlockEntities++;
            return;
        }

        long beId = blockEntity.getBlockPos().asLong();
        if (HardwareOcclusionEngine.getInstance().isBlockEntityCulled(beId)) {
            EntityCulling.getInstance().skippedBlockEntities++;
            ci.setReturnValue(null);
            return;
        }

        HardwareOcclusionEngine.getInstance().trackRenderedBlockEntity(blockEntity);
        EntityCulling.getInstance().renderedBlockEntities++;
    }
}
