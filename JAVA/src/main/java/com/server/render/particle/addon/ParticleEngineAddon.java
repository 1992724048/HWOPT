package com.server.render.particle.addon;

import net.minecraft.client.particle.ParticleRenderType;

public interface ParticleEngineAddon {
	void asyncparticle$addRenderType(ParticleRenderType type);

	void asyncparticle$tickSyncParticles();
}
