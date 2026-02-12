package library.dll;

import nativecode.dll.*;

import java.lang.foreign.MemorySegment;

@LibraryImport(dll = "hwopt.dll", structSize = 96)
public interface PerlinNoiseNative {
    PerlinNoiseNative NATIVE = FFMFactory.load(PerlinNoiseNative.class);
    
    @Field(offset = 8)
    int first_octave();
    @Field(offset = 8)
    void first_octave(int v);
    @Field(offset = 16)
    double max_value();
    @Field(offset = 16)
    void max_value(double v);
    
    @Static
    @Name("NoiseContext::_create")
    PerlinNoiseNative create(long seed, int firstOctave, double[] amplitudes, int size, boolean useNewInitialization);

    @Name("NoiseContext::_destroy")
    void destroy();

    @Name("NoiseContext::get_value3")
    double getValue(double x, double y, double z);

    @Name("NoiseContext::get_value6")
    double getValue(double x, double y, double z, double yScale, double yFudge, boolean yFlatHack);

    @Name("NoiseContext::edge_value")
    double edgeValue(double noiseValue);

    @Name("NoiseContext::_amplitudes")
    int amplitudes(MemorySegment amplitudes, int size);

    @Name("NoiseContext::_amplitudes_size")
    int amplitudesSize();
}
