package com.server.render.entityculling.mixin;

import com.server.render.entityculling.EntityCulling;
import com.server.render.entityculling.access.Cullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {Entity.class, BlockEntity.class})
public class CullableMixin implements Cullable {
    @Unique
    private long lasttime;
    @Unique
    private boolean culled;
    @Unique
    private boolean outOfCamera;

    @Override
    public void setTimeout() {
        lasttime = System.currentTimeMillis() + 1000;
    }

    @Override
    public boolean isForcedVisible() {
        return lasttime > System.currentTimeMillis();
    }

    @Override
    public void setCulled(boolean value) {
        this.culled = value;
        if (!value) {
            setTimeout();
        }
    }

    @Override
    public boolean isCulled() {
        if (!EntityCulling.enabled) return false;
        return culled;
    }

    @Override
    public void setOutOfCamera(boolean value) {
        this.outOfCamera = value;
    }

    @Override
    public boolean isOutOfCamera() {
        if (!EntityCulling.enabled) return false;
        return outOfCamera;
    }
}
