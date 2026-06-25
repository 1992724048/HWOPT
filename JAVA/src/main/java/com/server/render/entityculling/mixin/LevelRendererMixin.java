package com.server.render.entityculling.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.server.render.entityculling.EntityCulling;
import com.server.render.entityculling.occlusion.HardwareOcclusionEngine;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	
	@Unique
	private net.minecraft.client.multiplayer.ClientLevel hwopt$getLevel() {
		return Minecraft.getInstance().level;
	}
	
	@Unique
	private net.minecraft.client.multiplayer.ClientLevel hwopt$lastLevel;
	
	@Inject(method = "render", at = @At("HEAD"))
	private void hwopt$onRenderHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
		HardwareOcclusionEngine hoc = HardwareOcclusionEngine.getInstance();
		if (!hoc.isEnabled() || !EntityCulling.enabled) return;
		net.minecraft.client.multiplayer.ClientLevel level = hwopt$getLevel();
		if (level == null) return;
		
		if (level != this.hwopt$lastLevel) {
			this.hwopt$lastLevel = level;
			hoc.onWorldChange();
		}
		
		hoc.advanceFrame();
	}
}
