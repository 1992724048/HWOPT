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
	
	@Name("NormalNoise::max_value")
	double maxValue();
	
	@Name("NormalNoise::set_first")
	void setFirst(int firstOctave, double[] amplitudes, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value);
	
	@Name("NormalNoise::set_second")
	void setSecond(int firstOctave, double[] amplitudes, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value);
	
	@Name("NormalNoise::add_noise_to_first")
	void addNoiseToFirst(int index, double xo, double yo, double zo, byte[] array);
	
	@Name("NormalNoise::add_noise_to_second")
	void addNoiseToSecond(int index, double xo, double yo, double zo, byte[] array);
	
	@Static
	@Name("NormalNoise::expected_deviation")
	double expected_deviation(int octave_span);
}
