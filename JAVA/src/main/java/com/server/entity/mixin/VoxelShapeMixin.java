package com.server.entity.mixin;

import com.server.entity.access.VoxelShapeAccess;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VoxelShape.class)
public abstract class VoxelShapeMixin implements VoxelShapeAccess {
	@Shadow
	@Final
	protected DiscreteVoxelShape shape;
	
	public boolean hwopt_isFullCube() {
		int x = this.shape.getSize(Direction.Axis.X);
		int y = this.shape.getSize(Direction.Axis.Y);
		int z = this.shape.getSize(Direction.Axis.Z);
		int total = x * y * z;
		if (total <= 0 || total > 8) return false;
		for (int i = 0; i < x; i++)
			for (int j = 0; j < y; j++)
				for (int k = 0; k < z; k++)
					if (!this.shape.isFull(i, j, k)) return false;
		return true;
	}
}
