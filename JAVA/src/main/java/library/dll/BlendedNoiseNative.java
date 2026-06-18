package library.dll;

import nativecode.dll.*;

@LibraryImport(dll = "hwopt.dll", structSize = 88)
public interface BlendedNoiseNative extends AutoCloseable {
	class Holder {
		static final BlendedNoiseNative INSTANCE = FFMFactory.load(BlendedNoiseNative.class);
	}
	
	static BlendedNoiseNative instance() {
		return BlendedNoiseNative.Holder.INSTANCE;
	}
	
	@Field(offset = 72)
	double max_value();
	
	@Field(offset = 72)
	void max_value(double v);
	
	@Static
	@Name("BlendedNoise::_create")
	BlendedNoiseNative create(PerlinNoiseNative minLimitNoise, PerlinNoiseNative maxLimitNoise, PerlinNoiseNative mainNoise, double xzScale, double yScale, double xzFactor, double yFactor, double smearScaleMultiplier);

	@Name("BlendedNoise::_destroy")
	void destroy();
	
	@Name("BlendedNoise::compute")
	double compute(double x, double y, double z);
	
	@Name("BlendedNoise::get_values")
	void getValues(double[] xs, double[] ys, double[] zs, double[] result);
}
