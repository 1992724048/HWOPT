package com.server.render.entityculling;

import com.server.render.entityculling.access.EntityRendererInter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

public class NMSCullingHelper {
    private static final Minecraft MC = Minecraft.getInstance();

    @SuppressWarnings("unchecked")
    public static boolean ignoresCulling(Entity entity) {
        EntityRenderer<? super Entity, ?> renderer = MC.getEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) return true;
        return ((EntityRendererInter<Entity>) renderer).entityCullingIgnoresCulling(entity);
    }

    @SuppressWarnings("unchecked")
    public static AABB getCullingBox(Entity entity) {
        if (entity instanceof ArmorStand armorStand && armorStand.isMarker()) {
            return entity.getType().getDimensions().makeBoundingBox(entity.position());
        }
        EntityRenderer<? super Entity, ?> renderer = MC.getEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) return null;
        return ((EntityRendererInter<Entity>) renderer).entityCullingGetCullingBox(entity);
    }
}
