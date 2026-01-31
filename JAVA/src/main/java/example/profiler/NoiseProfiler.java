package example.profiler;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import library.dll.PerlinNoiseNative;
import net.minecraft.util.RandomSource;

import static library.dll.PerlinNoiseNative.NATIVE;

public class NoiseProfiler {
    static final int N = 500000000;
    
    static PerlinNoiseNative hwopt$nativePtr;
    
    static {
        DoubleArrayList amplitudeList = new DoubleArrayList();
        amplitudeList.add(1);
        amplitudeList.add(1);
        amplitudeList.add(1);
        amplitudeList.add(1);
        hwopt$nativePtr = NATIVE.create(1234L, -2, amplitudeList.toDoubleArray(), amplitudeList.size(), true);
    }
    
    static void main(String[] args) {
        double sumNative = 0.0;
        for (int r = 0; r < N; r++) {
            for (int i = 0; i < N; i++) {
                sumNative += hwopt$nativePtr.getValue(
                        114 + i * 0.001,
                        514,
                        1919 + i * 0.002
                );
            }
        }
    }
}
