package library.dll;

import nativecode.dll.*;

@LibraryImport(dll = "hwopt.dll", structSize = 96)
public interface PerlinNoiseNative extends AutoCloseable {
	class Holder {
		static final PerlinNoiseNative INSTANCE = FFMFactory.load(PerlinNoiseNative.class);
	}
	
	static PerlinNoiseNative instance() {
		return PerlinNoiseNative.Holder.INSTANCE;
	}
	
	@Field(offset = 8)
	int first_octave();
	
	@Field(offset = 8)
	void first_octave(int v);
	
	@Field(offset = 16)
	double max_value();
	
	@Field(offset = 16)
	void max_value(double v);
	
	@Static
	@Name("PerlinNoise::_create")
	PerlinNoiseNative create(int firstOctave, double[] amplitudes, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value);
	
	@Name("PerlinNoise::_destroy")
	void destroy();

	@Name("PerlinNoise::add_noise")
	void addNoise(int index, ImprovedNoiseNative noise);

	@Name("PerlinNoise::get_value3")
	double getValue(double x, double y, double z);
	
	@Name("PerlinNoise::get_value5")
	double getValue(double x, double y, double z, double yScale, double yFudge);
	
	@Name("PerlinNoise::edge_value")
	double edgeValue(double noiseValue);
	
	@Name("PerlinNoise::_amplitudes")
	double[] amplitudes();
	
	@Name("PerlinNoise::max_broken_value")
	double max_broken_value(double y_scale);
	
	@Name("PerlinNoise::get_values")
	void get_values(double[] xs, double[] ys, double[] zs, double[] result);
}
