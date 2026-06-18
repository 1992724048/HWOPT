package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 32)
public interface NormalNoiseNative extends AutoCloseable {
	class Holder {
		static final NormalNoiseNative INSTANCE = FFMFactory.load(NormalNoiseNative.class);
	}
	
	static NormalNoiseNative instance() {
		return NormalNoiseNative.Holder.INSTANCE;
	}
	
	@Static
	@Name("NormalNoise::_create")
	NormalNoiseNative create(double value_factor, double max_value);

	@Name("NormalNoise::_destroy")
	void destroy();
	
	@Name("NormalNoise::get_value")
	double getValue(double x, double y, double z);
	
	@Name("NormalNoise::get_values")
	void getValues(double[] xs, double[] ys, double[] zs, double[] result);

	@Name("NormalNoise::max_value")
	double maxValue();
	
	@Name("NormalNoise::set_perlin_noise")
	void setPerlinNoise(PerlinNoiseNative first, PerlinNoiseNative second);

	@Static
	@Name("NormalNoise::expected_deviation")
	double expected_deviation(int octave_span);
}
