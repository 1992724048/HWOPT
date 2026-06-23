package com.server.render.particle.addon;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;

public interface LightCachedParticleAddon {
	byte hwopt$getCompressedLight();
	void hwopt$setCompressedLight(byte light);
	boolean hwopt$hasLightCache();
	void hwopt$enableLightCache();
	void hwopt$refreshLightCache();
	ClientLevel hwopt$level();

	static byte compress(int packedLight) {
		int block = (packedLight >> 4) & 0xF;
		int sky = (packedLight >> 20) & 0xF;
		return (byte) ((sky << 4) | block);
	}

	static int decompress(byte compressed) {
		int block = compressed & 0xF;
		int sky = (compressed >> 4) & 0xF;
		return (sky << 20) | (block << 4);
	}
}
