package com.server.render.entityculling.mixin;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("getWidth")
    float invokeGetWidth();

    @Invoker("getHeight")
    float invokeGetHeight();

    @Invoker("setWidth")
    void invokeSetWidth(float width);

    @Invoker("setHeight")
    void invokeSetHeight(float height);
}
