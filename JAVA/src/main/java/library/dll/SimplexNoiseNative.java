package library.dll;

import nativecode.dll.*;

@LibraryImport(dll = "hwopt.dll", structSize = 280)
public interface SimplexNoiseNative extends AutoCloseable {
	SimplexNoiseNative NATIVE = FFMFactory.load(SimplexNoiseNative.class);

	@Static
	@Name("SimplexNoise::_create")
	SimplexNoiseNative create(double xo, double yo, double zo, int[] array, int len);

	@Name("SimplexNoise::_destroy")
	void destroy();

	@Name("SimplexNoise::get_value2")
	double getValue2(double x, double y);

	@Name("SimplexNoise::get_value3")
	double getValue(double x, double y, double z);
}
