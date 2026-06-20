package com.server.world.worldgen.mixin.chunk;

import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NoiseChunk.class)
public abstract class NoiseChunkForIndexMixin {

	@Shadow
	private int cellWidth;

	@Shadow
	private int cellHeight;

	@Shadow
	private int inCellX;

	@Shadow
	private int inCellY;

	@Shadow
	private int inCellZ;

	@Shadow
	private int arrayIndex;

	@Overwrite
	public NoiseChunk forIndex(int cellIndex) {
		int cellW = this.cellWidth;
		int zInCell = cellIndex & (cellW - 1);
		int xyIndex = cellIndex / cellW;
		int xInCell = xyIndex & (cellW - 1);
		int yInCell = this.cellHeight - 1 - (xyIndex / cellW);

		this.inCellX = xInCell;
		this.inCellY = yInCell;
		this.inCellZ = zInCell;
		this.arrayIndex = cellIndex;
		return (NoiseChunk) (Object) this;
	}
}
