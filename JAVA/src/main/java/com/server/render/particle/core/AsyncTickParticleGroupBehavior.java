package com.server.render.particle.core;

import net.minecraft.client.particle.ParticleGroup;

import java.util.List;

public class AsyncTickParticleGroupBehavior {
	private static final List<Class<?>> ASYNC_TICKABLE_CLASSES = List.of(ParticleGroup.class);

	public static boolean canTickAsync(ParticleGroup<?> particleGroup) {
		Class<?> clazz = particleGroup.getClass();
		if (clazz == ParticleGroup.class) return true;
		return ASYNC_TICKABLE_CLASSES.contains(clazz);
	}
}
