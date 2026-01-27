package library.dll;

import nativecode.dll.*;

@LibraryImport(dll = "hwopt.dll", structSize = 96)
public interface PerlinNoiseNative {
    PerlinNoiseNative NATIVE = FFMFactory.load(PerlinNoiseNative.class);
    
    @Field(offset = 40)
    int first_octave();
    @Field(offset = 40)
    void first_octave(int v);
    @Field(offset = 88)
    double max_value();
    @Field(offset = 88)
    void max_value(double v);
    
    @Static
    @Name("PerlinNoise_create")
    PerlinNoiseNative create(int firstOctave, double[] amplitudes, int size, boolean useNewInitialization);

    @Name("PerlinNoise_destroy")
    void destroy();

    @Name("PerlinNoise_getValue_3")
    double getValue(double x, double y, double z);

    @Name("PerlinNoise_getValue_6")
    double getValue(double x, double y, double z, double yScale, double yFudge, boolean yFlatHack);

    @Name("PerlinNoise_edgeValue")
    double edgeValue(double noiseValue);

    @Name("PerlinNoise_firstOctave")
    int firstOctave();

    @Name("PerlinNoise_amplitudes")
    int amplitudes(double[] amplitudes, int size);

    @Name("PerlinNoise_amplitudes_size")
    int amplitudesSize();

    @Name("PerlinNoise_maxValue")
    double maxValue();
}
