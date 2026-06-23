package com.server.render.particle.addon;

import net.minecraft.client.particle.Particle;

import java.util.Set;

public interface ParticleGroupAddition {
	default void asyncparticles$removeDeadParticles() {
		throw new AssertionError("Must be implemented!");
	}

	default void asyncparticles$clear() {
		throw new AssertionError("Must be implemented!");
	}

	default void asyncparticles$tickSyncParticles() {
		throw new AssertionError("Must be implemented!");
	}
}
