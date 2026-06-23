package com.server.render.entityculling.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public interface EntityRendererInter<T extends Entity> {
    boolean entityCullingIgnoresCulling(T entity);
    AABB entityCullingGetCullingBox(T entity);
}
