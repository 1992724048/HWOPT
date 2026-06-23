package com.server.render.particle.mixin;

import com.server.render.particle.addon.LightCachedParticleAddon;
import com.server.render.particle.addon.ParticleAddon;
import com.server.render.particle.util.ParticleThreadLocal;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Particle.class)
public class MixinParticle implements ParticleAddon, LightCachedParticleAddon {
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
	private volatile boolean hwopt$lightCacheEnabled;
	@Unique
	private byte hwopt$tickFlags;

	private static final byte FLAG_TICKED = 1;
	private static final byte FLAG_RENDER_SYNC = 2;
	private static final byte FLAG_TICK_SYNC = 4;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void hwopt$onInit(CallbackInfo ci) {
		this.hwopt$tickFlags = 0;
	}

	@Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
	private void hwopt$onGetLightCoords(float partialTick, CallbackInfoReturnable<Integer> cir) {
		if (hwopt$lightCacheEnabled) {
			cir.setReturnValue(LightCachedParticleAddon.decompress(hwopt$compressedLight));
		}
	}

	@Override
	public void asyncparticles$setTicked() {
		hwopt$tickFlags |= FLAG_TICKED;
	}

	@Override
	public void asyncparticles$resetTicked() {
		hwopt$tickFlags &= ~FLAG_TICKED;
	}

	@Override
	public boolean asyncparticles$isTicked() {
		return (hwopt$tickFlags & FLAG_TICKED) != 0;
	}

	@Override
	public void asyncparticles$setRenderSync() {
		hwopt$tickFlags |= FLAG_RENDER_SYNC;
	}

	@Override
	public boolean asyncparticles$isRenderSync() {
		return (hwopt$tickFlags & FLAG_RENDER_SYNC) != 0;
	}

	@Override
	public void asyncparticles$setTickSync() {
		hwopt$tickFlags |= FLAG_TICK_SYNC;
	}

	@Override
	public boolean asyncparticles$isTickSync() {
		return (hwopt$tickFlags & FLAG_TICK_SYNC) != 0;
	}

	@Override
	public boolean asyncparticles$isVisibleOnScreen() {
		return false;
	}

	@Override
	public Class<? extends Particle> asyncparticles$getRealClass() {
		return (Class<? extends Particle>) (Object) getClass();
	}

	@Override
	public byte getTickFlag() {
		return hwopt$tickFlags;
	}

	@Override
	public void asyncparticles$setLight(int light) {
		this.hwopt$compressedLight = LightCachedParticleAddon.compress(light);
	}

	@Override
	public byte asyncparticles$getCompressedLight() {
		return hwopt$compressedLight;
	}

	@Override
	public void asyncparticles$refresh() {
		ClientLevel level = this.level;
		if (level == null) {
			return;
		}
		BlockPos blockPos = hwopt$sharedPos().set(this.x, this.y, this.z);
		int light;
		try {
			light = level.hasChunkAt(blockPos) ? LightCoordsUtil.getLightCoords(level, blockPos) : 0;
		} catch (MissingPaletteEntryException ignore) {
			light = 0;
		}
		asyncparticles$setLight(light);
	}

	@Override
	public void asyncparticles$enableLightCache() {
		hwopt$lightCacheEnabled = true;
	}

	@Override
	public void asyncparticles$disableLightCache() {
		hwopt$lightCacheEnabled = false;
	}

	@Override
	public boolean asyncparticles$isEnabledLightCache() {
		return hwopt$lightCacheEnabled;
	}

	@Override
	public ClientLevel asyncparticles$level() {
		return this.level;
	}

	@Unique
	private static final ParticleThreadLocal<BlockPos.MutableBlockPos> hwopt$sharedPosTL =
		ParticleThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

	@Unique
	private static BlockPos.MutableBlockPos hwopt$sharedPos() {
		return hwopt$sharedPosTL.get();
	}
}
