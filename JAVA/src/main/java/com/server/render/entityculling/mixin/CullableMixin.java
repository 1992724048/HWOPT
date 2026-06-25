package com.server.render.entityculling.mixin;

import com.server.render.entityculling.EntityCulling;
import com.server.render.entityculling.access.Cullable;
import com.server.render.entityculling.occlusion.HardwareOcclusionEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {Entity.class, BlockEntity.class})
public class CullableMixin implements Cullable {
    @Unique
    private long jAVA$lasttime;
    @Unique
    private boolean jAVA$culled;
    @Unique
    private boolean jAVA$outOfCamera;

    @Override
    public void setTimeout() {
        jAVA$lasttime = System.currentTimeMillis() + 1000;
    }

    @Override
    public boolean isForcedVisible() {
        return jAVA$lasttime > System.currentTimeMillis();
    }

    @Override
    public void setCulled(boolean value) {
        this.jAVA$culled = value;
        if (!value) {
            setTimeout();
        }
    }

    @Override
    public boolean isCulled() {
        if (!EntityCulling.enabled) return false;
        if (jAVA$culled) return true;
        Object self = this;
        if (self instanceof Entity entity) {
            return HardwareOcclusionEngine.getInstance().isEntityCulled(entity.getId());
        }
        if (self instanceof BlockEntity be) {
            return HardwareOcclusionEngine.getInstance().isBlockEntityCulled(be.getBlockPos().asLong());
        }
        return false;
    }

    @Override
    public void setOutOfCamera(boolean value) {
        this.jAVA$outOfCamera = value;
    }

    @Override
    public boolean isOutOfCamera() {
        if (!EntityCulling.enabled) return false;
        return jAVA$outOfCamera;
    }
}
