package com.server.render.entityculling.occlusion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OcclusionCullingInstance {
	private final DataProvider dataProvider;
	private boolean useCache;

	public OcclusionCullingInstance(DataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	public void resetCache() {
	}

	public boolean isAABBVisible(AABB aabb, Vec3 cameraPos, BlockGetter level) {
		double dx = (aabb.minX + aabb.maxX) / 2 - cameraPos.x;
		double dy = (aabb.minY + aabb.maxY) / 2 - cameraPos.y;
		double dz = (aabb.minZ + aabb.maxZ) / 2 - cameraPos.z;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (dist < 2.0) return true;

		double sx = dx / dist;
		double sy = dy / dist;
		double sz = dz / dist;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (double d = 1.0; d < dist - 1.0; d += 1.0) {
			pos.set(cameraPos.x + sx * d, cameraPos.y + sy * d, cameraPos.z + sz * d);
			if (dataProvider.isOpaqueFullCube(level, pos)) return false;
		}
		return true;
	}
}
