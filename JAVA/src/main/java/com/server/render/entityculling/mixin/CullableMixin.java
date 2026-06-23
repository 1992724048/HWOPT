package com.server.render.entityculling.mixin;

import com.server.render.entityculling.access.Cullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({Entity.class, BlockEntity.class})
public class CullableMixin implements Cullable {
	@Unique
	private boolean hwopt$culled;
	@Unique
	private boolean hwopt$outOfCamera;
	@Unique
	private long hwopt$timeout;
	@Unique
	private long hwopt$lastCulledChange;

	@Override
	public boolean isCulled() {
		long now = System.currentTimeMillis();
		if (now - hwopt$lastCulledChange < 1000) return false;
		return hwopt$culled;
	}

	@Override
	public void setCulled(boolean culled) {
		if (this.hwopt$culled != culled) {
			this.hwopt$culled = culled;
			this.hwopt$lastCulledChange = System.currentTimeMillis();
		}
	}

	@Override
	public boolean isForcedVisible() {
		long now = System.currentTimeMillis();
		return now - hwopt$lastCulledChange < 1000;
	}

	@Override
	public void setTimeout(long ticks) {
		this.hwopt$timeout = ticks;
		this.hwopt$lastCulledChange = System.currentTimeMillis();
	}

	@Override
	public boolean isOutOfCamera() {
		return hwopt$outOfCamera;
	}

	@Override
	public void setOutOfCamera(boolean outOfCamera) {
		this.hwopt$outOfCamera = outOfCamera;
	}
}
