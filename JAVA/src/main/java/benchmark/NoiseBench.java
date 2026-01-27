package benchmark;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import library.dll.PerlinNoiseNative;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import static library.dll.PerlinNoiseNative.NATIVE;

public enum NoiseBench {
    ;
    static final int N = 50000000;

    static final double X = 100.3;
    static final double Y = 64.0;
    static final double Z = 200.7;

    static RandomSource random;
    static PerlinNoise JAVA_PERLIN;
    static PerlinNoiseNative hwopt$nativePtr;

    static {
        random = RandomSource.create(1234L);
        JAVA_PERLIN = net.minecraft.world.level.levelgen.synth.PerlinNoise.create(random, -2, 1.0, 1.0, 1.0, 1.0);

        DoubleArrayList amplitudeList = new DoubleArrayList();
        amplitudeList.add(1);
        amplitudeList.add(1);
        amplitudeList.add(1);
        amplitudeList.add(1);
        hwopt$nativePtr = NATIVE.create(-2, amplitudeList.toDoubleArray(), amplitudeList.size(), true);
        System.out.println("Native: " + hwopt$nativePtr);
    }

    static void main(String[] args) {
        // Java -> C++ (FFM)
        long t0 = System.nanoTime();
        double sumNative = 0.0;

        for (int i = 0; N > i; i++) {
            sumNative += hwopt$nativePtr.getValue(X + i * 0.001, Y, Z + i * 0.002);
        }

        long t1 = System.nanoTime();
        double nativeSec = (t1 - t0) / 1.0e9;
        System.out.println("Java -> C++ (Panama) time: " + nativeSec + " s");
        System.out.println("checksum(native): " + sumNative);

        // ️Java
        t0 = System.nanoTime();
        double sumJava = 0.0;

        for (int i = 0; N > i; i++) {
            sumJava += JAVA_PERLIN.getValue(X + i * 0.001, Y, Z + i * 0.002);
        }

        t1 = System.nanoTime();
        double javaSec   = (t1 - t0) / 1.0e9;
        System.out.println("Pure Java PerlinNoise time: " + javaSec + " s");
        System.out.println("checksum(java):   " + sumJava);
    }
}