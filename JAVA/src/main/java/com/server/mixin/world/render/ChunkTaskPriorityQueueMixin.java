package com.server.mixin.world.render;

import net.minecraft.server.level.ChunkTaskPriorityQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChunkTaskPriorityQueue.class)
public abstract class ChunkTaskPriorityQueueMixin {

	@ModifyArg(method = "submit", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"), index = 0, require = 1)
	private int hwopt$clampSubmitIndex(int index) {
		return Math.min(index, ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
	}

	@ModifyArg(method = "resortChunkTasks", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"), index = 0, require = 2)
	private int hwopt$clampResortIndex(int index) {
		return Math.min(index, ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
	}
}
