package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 1)
public interface MathNative extends AutoCloseable {
	MathNative INSTANCE = FFMFactory.load(MathNative.class);
	
	@Static
	@Name("Math::lerp3")
	double lerp3(double alpha1, double alpha2, double alpha3, double x000, double x100, double x010, double x110, double x001, double x101, double x011, double x111);
	
	@Static
	@Name("Math::ray_intersects_aabb")
	boolean rayIntersectsAABB(double startX, double startY, double startZ, double dirX, double dirY, double dirZ, double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
	
	@Static
	@Name("Math::atan2")
	double atan2(double y, double x);
	
	@Static
	@Name("Math::clamped_lerp")
	double clamped_lerp(double factor, double min, double max);
	
	@Static
	@Name("Math::batch_trilerp")
	void batch_trilerp(double n000, double n100, double n010, double n110, double n001, double n101, double n011, double n111, int cellWidth, int cellHeight, double[] output);
}
