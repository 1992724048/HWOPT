package com.worldgen.accessor;

import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NoiseChunk.NoiseInterpolator.class)
public interface NoiseInterpolatorAccessor {

	@Accessor("noise000")
	double noise000();

	@Accessor("noise100")
	double noise100();

	@Accessor("noise010")
	double noise010();

	@Accessor("noise110")
	double noise110();

	@Accessor("noise001")
	double noise001();

	@Accessor("noise101")
	double noise101();

	@Accessor("noise011")
	double noise011();

	@Accessor("noise111")
	double noise111();
}
