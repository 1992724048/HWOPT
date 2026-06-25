package com.server.render.entityculling.mixin;

import com.server.render.entityculling.EntityCulling;
import com.server.render.entityculling.NMSCullingHelper;
import com.server.render.entityculling.access.Cullable;
import com.server.render.entityculling.occlusion.HardwareOcclusionEngine;
import com.hwpp.mod.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class EntityTickCullingMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void hwopt$tickEntity(Entity entity, CallbackInfo ci) {
        EntityCulling mod = EntityCulling.getInstance();
        if (!Config.CONFIG.tickCulling.get() || Config.CONFIG.skipEntityCulling.get()) return;
        if (mod.tickCullWhitelists.contains(entity.getType())) return;
        if (mod.entityWhitelist.contains(entity.getType())) return;
        if (entity instanceof AbstractMinecart) return;
        if (NMSCullingHelper.ignoresCulling(entity)) return;
        if (Config.CONFIG.forceDisplayCulling.get() && entity instanceof Display display) {
            com.server.render.entityculling.mixin.DisplayAccessor da = (com.server.render.entityculling.mixin.DisplayAccessor) display;
            if (da.invokeGetWidth() <= 0 || da.invokeGetHeight() <= 0) {
                da.invokeSetWidth(3);
                da.invokeSetHeight(3);
                display.setPos(display.getX(), display.getY(), display.getZ());
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player || entity == mc.getCameraEntity()) return;
        if (entity.isPassenger() || entity.isVehicle()) return;
        Cullable cullable = (Cullable) entity;
        if (cullable.isCulled() || cullable.isOutOfCamera()) {
            mod.skippedEntityTicks++;
            hwopt$basicTick(entity);
            ci.cancel();
        } else {
            mod.tickedEntities++;
        }
    }

    @Unique
    private void hwopt$basicTick(Entity entity) {
        entity.setOldPosAndRot();
        entity.tickCount++;
        if (entity instanceof LivingEntity living) {
            living.aiStep();
            if (living.hurtTime > 0) living.hurtTime--;
        }
    }
}
