package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 1)
public interface MathNative extends AutoCloseable {
	class Holder {
		static final MathNative INSTANCE = FFMFactory.load(MathNative.class);
	}
	
	static MathNative instance() {
		return MathNative.Holder.INSTANCE;
	}
	
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
}
