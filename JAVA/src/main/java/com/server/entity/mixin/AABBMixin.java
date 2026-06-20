package com.server.entity.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Optional;

@Mixin(AABB.class)
public abstract class AABBMixin {
	@Shadow
	@Final
	public double maxY;
	@Shadow
	@Final
	private static double EPSILON;
	@Shadow
	@Final
	public static AABB INFINITE;
	@Shadow
	@Final
	public double minX;
	@Shadow
	@Final
	public double minY;
	@Shadow
	@Final
	public double minZ;
	@Shadow
	@Final
	public double maxX;
	@Shadow
	@Final
	public double maxZ;
	
	@Overwrite
	public static @Nullable BlockHitResult clip(Iterable<AABB> aabBs, Vec3 from, Vec3 to, BlockPos pos) {
		double[] scaleReference = new double[]{(double) 1.0F};
		Direction direction = null;
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double dz = to.z - from.z;
		
		for (AABB aabb : aabBs) {
			direction = getDirection(aabb.move(pos), from, scaleReference, direction, dx, dy, dz);
		}
		
		if (direction == null) {
			return null;
		} else {
			double scale = scaleReference[0];
			return new BlockHitResult(from.add(scale * dx, scale * dy, scale * dz), direction, pos, false);
		}
	}
	
	@Overwrite
	public static AABB of(BoundingBox box) {
		return new AABB((double) box.minX(), (double) box.minY(), (double) box.minZ(), (double) (box.maxX() + 1), (double) (box.maxY() + 1), (double) (box.maxZ() + 1));
	}
	
	@Overwrite
	public static AABB unitCubeFromLowerCorner(Vec3 pos) {
		return new AABB(pos.x, pos.y, pos.z, pos.x + (double) 1.0F, pos.y + (double) 1.0F, pos.z + (double) 1.0F);
	}
	
	@Overwrite
	public static AABB encapsulatingFullBlocks(BlockPos pos0, BlockPos pos1) {
		return new AABB((double) Math.min(pos0.getX(), pos1.getX()), (double) Math.min(pos0.getY(), pos1.getY()), (double) Math.min(pos0.getZ(), pos1.getZ()), (double) (Math.max(pos0.getX(), pos1.getX()) + 1), (double) (Math.max(pos0.getY(), pos1.getY()) + 1), (double) (Math.max(pos0.getZ(), pos1.getZ()) + 1));
	}
	
	@Overwrite
	public AABB setMinX(double minX) {
		return new AABB(minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
	}
	
	@Overwrite
	public AABB setMinY(double minY) {
		return new AABB(this.minX, minY, this.minZ, this.maxX, this.maxY, this.maxZ);
	}
	
	@Overwrite
	public AABB setMinZ(double minZ) {
		return new AABB(this.minX, this.minY, minZ, this.maxX, this.maxY, this.maxZ);
	}
	
	@Overwrite
	public AABB setMaxX(double maxX) {
		return new AABB(this.minX, this.minY, this.minZ, maxX, this.maxY, this.maxZ);
	}
	
	@Overwrite
	public AABB setMaxY(double maxY) {
		return new AABB(this.minX, this.minY, this.minZ, this.maxX, maxY, this.maxZ);
	}
	
	@Overwrite
	public AABB setMaxZ(double maxZ) {
		return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, maxZ);
	}
	
	@Overwrite
	public double min(Direction.Axis axis) {
		return axis.choose(this.minX, this.minY, this.minZ);
	}
	
	@Overwrite
	public double max(Direction.Axis axis) {
		return axis.choose(this.maxX, this.maxY, this.maxZ);
	}
	
	@Overwrite
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o instanceof AABB aabb) {
			if (Double.compare(aabb.minX, this.minX) != 0) {
				return false;
			} else if (Double.compare(aabb.minY, this.minY) != 0) {
				return false;
			} else if (Double.compare(aabb.minZ, this.minZ) != 0) {
				return false;
			} else if (Double.compare(aabb.maxX, this.maxX) != 0) {
				return false;
			} else {
				return Double.compare(aabb.maxY, this.maxY) == 0 && Double.compare(aabb.maxZ, this.maxZ) == 0;
			}
		} else {
			return false;
		}
	}
	
	@Overwrite
	public int hashCode() {
		int result = Double.hashCode(this.minX);
		result = 31 * result + Double.hashCode(this.minY);
		result = 31 * result + Double.hashCode(this.minZ);
		result = 31 * result + Double.hashCode(this.maxX);
		result = 31 * result + Double.hashCode(this.maxY);
		return 31 * result + Double.hashCode(this.maxZ);
	}
	
	@Overwrite
	public AABB contract(double xa, double ya, double za) {
		double minX = this.minX;
		double minY = this.minY;
		double minZ = this.minZ;
		double maxX = this.maxX;
		double maxY = this.maxY;
		double maxZ = this.maxZ;
		if (xa < (double) 0.0F) {
			minX -= xa;
		} else if (xa > (double) 0.0F) {
			maxX -= xa;
		}
		
		if (ya < (double) 0.0F) {
			minY -= ya;
		} else if (ya > (double) 0.0F) {
			maxY -= ya;
		}
		
		if (za < (double) 0.0F) {
			minZ -= za;
		} else if (za > (double) 0.0F) {
			maxZ -= za;
		}
		
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	@Overwrite
	public AABB expandTowards(Vec3 delta) {
		return this.expandTowards(delta.x, delta.y, delta.z);
	}
	
	@Overwrite
	public AABB expandTowards(double xa, double ya, double za) {
		double minX = this.minX;
		double minY = this.minY;
		double minZ = this.minZ;
		double maxX = this.maxX;
		double maxY = this.maxY;
		double maxZ = this.maxZ;
		if (xa < (double) 0.0F) {
			minX += xa;
		} else if (xa > (double) 0.0F) {
			maxX += xa;
		}
		
		if (ya < (double) 0.0F) {
			minY += ya;
		} else if (ya > (double) 0.0F) {
			maxY += ya;
		}
		
		if (za < (double) 0.0F) {
			minZ += za;
		} else if (za > (double) 0.0F) {
			maxZ += za;
		}
		
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	@Overwrite
	public AABB inflate(double xAdd, double yAdd, double zAdd) {
		double minX = this.minX - xAdd;
		double minY = this.minY - yAdd;
		double minZ = this.minZ - zAdd;
		double maxX = this.maxX + xAdd;
		double maxY = this.maxY + yAdd;
		double maxZ = this.maxZ + zAdd;
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	@Overwrite
	public AABB inflate(double amountToAddInAllDirections) {
		return this.inflate(amountToAddInAllDirections, amountToAddInAllDirections, amountToAddInAllDirections);
	}
	
	@Overwrite
	public AABB intersect(AABB other) {
		double minX = Math.max(this.minX, other.minX);
		double minY = Math.max(this.minY, other.minY);
		double minZ = Math.max(this.minZ, other.minZ);
		double maxX = Math.min(this.maxX, other.maxX);
		double maxY = Math.min(this.maxY, other.maxY);
		double maxZ = Math.min(this.maxZ, other.maxZ);
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	@Overwrite
	public AABB minmax(AABB other) {
		double minX = Math.min(this.minX, other.minX);
		double minY = Math.min(this.minY, other.minY);
		double minZ = Math.min(this.minZ, other.minZ);
		double maxX = Math.max(this.maxX, other.maxX);
		double maxY = Math.max(this.maxY, other.maxY);
		double maxZ = Math.max(this.maxZ, other.maxZ);
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	@Overwrite
	public AABB move(double xa, double ya, double za) {
		return new AABB(this.minX + xa, this.minY + ya, this.minZ + za, this.maxX + xa, this.maxY + ya, this.maxZ + za);
	}
	
	@Overwrite
	public AABB move(BlockPos pos) {
		return new AABB(this.minX + (double) pos.getX(), this.minY + (double) pos.getY(), this.minZ + (double) pos.getZ(), this.maxX + (double) pos.getX(), this.maxY + (double) pos.getY(), this.maxZ + (double) pos.getZ());
	}
	
	@Overwrite
	public AABB move(Vec3 pos) {
		return this.move(pos.x, pos.y, pos.z);
	}
	
	@Overwrite
	public AABB move(Vector3f pos) {
		return this.move((double) pos.x, (double) pos.y, (double) pos.z);
	}
	
	@Overwrite
	public boolean intersects(AABB aabb) {
		return this.intersects(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
	}
	
	@Overwrite
	public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return this.minX < maxX && this.maxX > minX && this.minY < maxY && this.maxY > minY && this.minZ < maxZ && this.maxZ > minZ;
	}
	
	@Overwrite
	public boolean intersects(Vec3 min, Vec3 max) {
		return this.intersects(Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z), Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z));
	}
	
	@Overwrite
	public boolean intersects(BlockPos pos) {
		return this.intersects((double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), (double) (pos.getX() + 1), (double) (pos.getY() + 1), (double) (pos.getZ() + 1));
	}
	
	@Overwrite
	public boolean contains(Vec3 vec) {
		return this.contains(vec.x, vec.y, vec.z);
	}
	
	@Overwrite
	public boolean contains(double x, double y, double z) {
		return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
	}
	
	@Overwrite
	public double getSize() {
		double xs = this.getXsize();
		double ys = this.getYsize();
		double zs = this.getZsize();
		return (xs + ys + zs) / (double) 3.0F;
	}
	
	@Overwrite
	public double getXsize() {
		return this.maxX - this.minX;
	}
	
	@Overwrite
	public double getYsize() {
		return this.maxY - this.minY;
	}
	
	@Overwrite
	public double getZsize() {
		return this.maxZ - this.minZ;
	}
	
	@Overwrite
	public AABB deflate(double xSubstract, double ySubtract, double zSubtract) {
		return this.inflate(-xSubstract, -ySubtract, -zSubtract);
	}
	
	@Overwrite
	public AABB deflate(double amount) {
		return this.inflate(-amount);
	}
	
	@Overwrite
	public Optional<Vec3> clip(Vec3 from, Vec3 to) {
		return clip(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ, from, to);
	}
	
	@Overwrite
	public static Optional<Vec3> clip(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3 from, Vec3 to) {
		double[] scaleReference = new double[]{(double) 1.0F};
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double dz = to.z - from.z;
		Direction direction = getDirection(minX, minY, minZ, maxX, maxY, maxZ, from, scaleReference, (Direction) null, dx, dy, dz);
		if (direction == null) {
			return Optional.empty();
		} else {
			double scale = scaleReference[0];
			return Optional.of(from.add(scale * dx, scale * dy, scale * dz));
		}
	}
	
	@Overwrite
	private static @Nullable Direction getDirection(AABB aabb, Vec3 from, double[] scaleReference, @Nullable Direction direction, double dx, double dy, double dz) {
		return getDirection(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, from, scaleReference, direction, dx, dy, dz);
	}
	
	@Overwrite
	private static @Nullable Direction getDirection(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3 from, double[] scaleReference, @Nullable Direction direction, double dx, double dy, double dz) {
		if (dx > 1.0E-7) {
			direction = clipPoint(scaleReference, direction, dx, dy, dz, minX, minY, maxY, minZ, maxZ, Direction.WEST, from.x, from.y, from.z);
		} else if (dx < -1.0E-7) {
			direction = clipPoint(scaleReference, direction, dx, dy, dz, maxX, minY, maxY, minZ, maxZ, Direction.EAST, from.x, from.y, from.z);
		}
		
		if (dy > 1.0E-7) {
			direction = clipPoint(scaleReference, direction, dy, dz, dx, minY, minZ, maxZ, minX, maxX, Direction.DOWN, from.y, from.z, from.x);
		} else if (dy < -1.0E-7) {
			direction = clipPoint(scaleReference, direction, dy, dz, dx, maxY, minZ, maxZ, minX, maxX, Direction.UP, from.y, from.z, from.x);
		}
		
		if (dz > 1.0E-7) {
			direction = clipPoint(scaleReference, direction, dz, dx, dy, minZ, minX, maxX, minY, maxY, Direction.NORTH, from.z, from.x, from.y);
		} else if (dz < -1.0E-7) {
			direction = clipPoint(scaleReference, direction, dz, dx, dy, maxZ, minX, maxX, minY, maxY, Direction.SOUTH, from.z, from.x, from.y);
		}
		
		return direction;
	}
	
	@Overwrite
	private static @Nullable Direction clipPoint(double[] scaleReference, @Nullable Direction direction, double da, double db, double dc, double point, double minB, double maxB, double minC, double maxC, Direction newDirection, double fromA, double fromB, double fromC) {
		double s = (point - fromA) / da;
		double pb = fromB + s * db;
		double pc = fromC + s * dc;
		if ((double) 0.0F < s && s < scaleReference[0] && minB - 1.0E-7 < pb && pb < maxB + 1.0E-7 && minC - 1.0E-7 < pc && pc < maxC + 1.0E-7) {
			scaleReference[0] = s;
			return newDirection;
		} else {
			return direction;
		}
	}
	
	@Overwrite
	public boolean collidedAlongVector(Vec3 vector, List<AABB> aabbs) {
		Vec3 from = this.getCenter();
		Vec3 to = from.add(vector);
		
		for (AABB shapePart : aabbs) {
			AABB inflated = shapePart.inflate(this.getXsize() * (double) 0.5F - 1.0E-7, this.getYsize() * (double) 0.5F - 1.0E-7, this.getZsize() * (double) 0.5F - 1.0E-7);
			if (inflated.contains(to) || inflated.contains(from)) {
				return true;
			}
			
			if (inflated.clip(from, to).isPresent()) {
				return true;
			}
		}
		
		return false;
	}
	
	@Overwrite
	public double distanceToSqr(Vec3 point) {
		double dx = Math.max(Math.max(this.minX - point.x, point.x - this.maxX), (double) 0.0F);
		double dy = Math.max(Math.max(this.minY - point.y, point.y - this.maxY), (double) 0.0F);
		double dz = Math.max(Math.max(this.minZ - point.z, point.z - this.maxZ), (double) 0.0F);
		return Mth.lengthSquared(dx, dy, dz);
	}
	
	@Overwrite
	public double distanceToSqr(AABB boundingBox) {
		double dx = Math.max(Math.max(this.minX - boundingBox.maxX, boundingBox.minX - this.maxX), (double) 0.0F);
		double dy = Math.max(Math.max(this.minY - boundingBox.maxY, boundingBox.minY - this.maxY), (double) 0.0F);
		double dz = Math.max(Math.max(this.minZ - boundingBox.maxZ, boundingBox.minZ - this.maxZ), (double) 0.0F);
		return Mth.lengthSquared(dx, dy, dz);
	}
	
	@Overwrite
	public String toString() {
		return "AABB[" + this.minX + ", " + this.minY + ", " + this.minZ + "] -> [" + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
	}
	
	@Overwrite
	public boolean hasNaN() {
		return Double.isNaN(this.minX) || Double.isNaN(this.minY) || Double.isNaN(this.minZ) || Double.isNaN(this.maxX) || Double.isNaN(this.maxY) || Double.isNaN(this.maxZ);
	}
	
	@Overwrite
	public Vec3 getCenter() {
		return new Vec3(Mth.lerp((double) 0.5F, this.minX, this.maxX), Mth.lerp((double) 0.5F, this.minY, this.maxY), Mth.lerp((double) 0.5F, this.minZ, this.maxZ));
	}
	
	@Overwrite
	public Vec3 getBottomCenter() {
		return new Vec3(Mth.lerp((double) 0.5F, this.minX, this.maxX), this.minY, Mth.lerp((double) 0.5F, this.minZ, this.maxZ));
	}
	
	@Overwrite
	public Vec3 getMinPosition() {
		return new Vec3(this.minX, this.minY, this.minZ);
	}
	
	@Overwrite
	public Vec3 getMaxPosition() {
		return new Vec3(this.maxX, this.maxY, this.maxZ);
	}
	
	@Overwrite
	public static AABB ofSize(Vec3 center, double sizeX, double sizeY, double sizeZ) {
		return new AABB(center.x - sizeX / (double) 2.0F, center.y - sizeY / (double) 2.0F, center.z - sizeZ / (double) 2.0F, center.x + sizeX / (double) 2.0F, center.y + sizeY / (double) 2.0F, center.z + sizeZ / (double) 2.0F);
	}
}
