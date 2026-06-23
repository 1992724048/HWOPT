package com.server.render.entityculling.mixin;

import com.server.render.entityculling.access.EntityRendererInter;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> implements EntityRendererInter<T> {

    @Shadow
    abstract boolean affectedByCulling(T entity);

    @Shadow
    abstract AABB getBoundingBoxForCulling(T entity);

    @Override
    public boolean entityCullingIgnoresCulling(T entity) {
        return !affectedByCulling(entity);
    }

    @Override
    public AABB entityCullingGetCullingBox(T entity) {
        return getBoundingBoxForCulling(entity);
    }
}
