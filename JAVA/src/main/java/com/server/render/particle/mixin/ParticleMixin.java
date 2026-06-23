package com.server.render.particle.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.server.render.particle.util.ParticleThreadLocal;
import com.server.render.particle.addon.LightCachedParticleAddon;

@Mixin(Particle.class)
public class ParticleMixin implements LightCachedParticleAddon {
	@Shadow
	protected double x;
	@Shadow
	protected double y;
	@Shadow
	protected double z;
	@Shadow
	@Final
	protected ClientLevel level;

	@Unique
	private byte hwopt$compressedLight;
	@Unique
	private boolean hwopt$ticked;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void hwopt$onInit(CallbackInfo ci) {
		this.hwopt$ticked = false;
	}

	@Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
	private void hwopt$onGetLightCoords(float partialTick, CallbackInfoReturnable<Integer> cir) {
		if (partialTick == 1.0f || this.hwopt$ticked) {
			cir.setReturnValue(LightCachedParticleAddon.decompress(hwopt$compressedLight));
		}
	}

	@Override
	public byte hwopt$getCompressedLight() {
		return hwopt$compressedLight;
	}

	@Override
	public void hwopt$setCompressedLight(byte light) {
		this.hwopt$compressedLight = light;
	}

	@Override
	public boolean hwopt$hasLightCache() {
		return true;
	}

	@Override
	public void hwopt$enableLightCache() {
	}

	@Override
	public void hwopt$refreshLightCache() {
		BlockPos pos = hwopt$sharedPos().set(this.x, this.y, this.z);
		this.hwopt$compressedLight = LightCachedParticleAddon.compress(
			LightCoordsUtil.getLightCoords(this.level, pos)
		);
	}

	@Unique
	private static final ParticleThreadLocal<BlockPos.MutableBlockPos> hwopt$sharedPosTL =
		new ParticleThreadLocal<>(BlockPos.MutableBlockPos::new);

	@Unique
	private static BlockPos.MutableBlockPos hwopt$sharedPos() {
		return hwopt$sharedPosTL.get();
	}

	@Override
	public ClientLevel hwopt$level() {
		return this.level;
	}
}
