package com.server.render.particle.addon;

import net.minecraft.client.particle.Particle;

public interface ParticleAddon {
	void asyncparticles$setTicked();

	void asyncparticles$resetTicked();

	boolean asyncparticles$isTicked();

	void asyncparticles$setRenderSync();

	boolean asyncparticles$isRenderSync();

	void asyncparticles$setTickSync();

	boolean asyncparticles$isTickSync();

	boolean asyncparticles$isVisibleOnScreen();

	Class<? extends Particle> asyncparticles$getRealClass();

	byte getTickFlag();
}
