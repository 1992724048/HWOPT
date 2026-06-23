package com.server.render.entityculling.mixin;

import com.server.render.entityculling.EntityCullingMod;
import com.server.render.entityculling.NMSCullingHelper;
import com.server.render.entityculling.access.Cullable;
import com.hwpp.mod.Config;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

    @Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
    private void hwopt$extractEntity(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> ci) {
        EntityCullingMod mod = EntityCullingMod.getInstance();
        if (Config.CONFIG.skipEntityCulling.get()) return;
        if (!EntityCullingMod.enabled || !Config.CONFIG.tickCulling.get()) return;
        Cullable cullable = (Cullable) entity;
        if (!cullable.isForcedVisible() && cullable.isCulled() && !NMSCullingHelper.ignoresCulling(entity)) {
            EntityRenderState state = new EntityRenderState();
            state.entityType = EntityTypes.INTERACTION;
            state.x = Mth.lerp(partialTick, entity.xOld, entity.getX());
            state.y = Mth.lerp(partialTick, entity.yOld, entity.getY());
            state.z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            state.isInvisible = true;
            mod.skippedEntities++;
            ci.setReturnValue(state);
            return;
        }
        mod.renderedEntities++;
        cullable.setOutOfCamera(false);
    }

    @Inject(method = "extractVisibleEntities", at = @At("HEAD"))
    private void hwopt$extractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker,
                                               LevelRenderState levelRenderState, CallbackInfo ci) {
        EntityCullingMod.getInstance().frustum = frustum;
    }
}
