package library.dll;

import nativecode.dll.LibraryImport;
import nativecode.dll.Name;

@LibraryImport(dll = "F:\\CODE\\hwopt\\CPP\\hwopt\\x64-Release\\hwopt.dll")
public interface PerlinNoiseNative {
    @Name("PerlinNoise_create")
    long create(int firstOctave, double[] amplitudes, int size, boolean useNewInitialization);

    @Name("PerlinNoise_destroy")
    void destroy(long ptr);

    @Name("PerlinNoise_getValue_3")
    double getValue(long ptr, double x, double y, double z);

    @Name("PerlinNoise_getValue_6")
    double getValue(long ptr, double x, double y, double z, double yScale, double yFudge, boolean yFlatHack);

    @Name("PerlinNoise_edgeValue")
    double edgeValue(long ptr, double noiseValue);

    @Name("PerlinNoise_firstOctave")
    int firstOctave(long ptr);

    @Name("PerlinNoise_amplitudes")
    int amplitudes(long ptr, double[] amplitudes, int size);

    @Name("PerlinNoise_amplitudes_size")
    int amplitudesSize(long ptr);

    @Name("PerlinNoise_maxValue")
    double maxValue(long ptr);
}
