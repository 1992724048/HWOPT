package library.dll;

import nativecode.dll.FFMFactory;
import nativecode.dll.LibraryImport;
import nativecode.dll.Name;
import nativecode.dll.Static;

@LibraryImport(dll = "hwopt.dll", structSize = 32)
public interface NormalNoiseNative {
	NormalNoiseNative NATIVE = FFMFactory.load(NormalNoiseNative.class);
	
	@Static
	@Name("NormalNoise::_create")
	NormalNoiseNative create(long seed, int firstOctave, double[] amplitudes, int size, boolean useNewInitialization);
	
	@Name("NormalNoise::_destroy")
	void destroy();
	
	@Name("NormalNoise::get_value")
	double getValue(double x, double y, double z);
	
	@Name("NormalNoise::max_value")
	double maxValue();
	
	@Name("NormalNoise::expected_deviation")
	double expected_deviation(int octave_span);
}
