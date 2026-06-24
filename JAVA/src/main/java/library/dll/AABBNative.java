package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 1)
public interface AABBNative {
	AABBNative INSTANCE = FFMFactory.load(AABBNative.class);
	
	@Static
	@Name("AABB::batch_collide_axis")
	double batchCollideAxis(int axis, double movingMinX, double movingMinY, double movingMinZ, double movingMaxX, double movingMaxY, double movingMaxZ, double[] stationaryBoxes, double initialDistance);
	
	@Static
	@Name("AABB::batch_find_collisions")
	int batchFindCollisions(double[] aabbs, int[] outputA, int[] outputB, int entityCount, int maxCollisions);
}

