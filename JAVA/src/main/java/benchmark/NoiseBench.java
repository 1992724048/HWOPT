package benchmark;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import library.dll.PerlinNoiseNative;
import nativecode.dll.FFMFactory;
import net.minecraft.util.RandomSource;

public enum NoiseBench {
    ;
    static final int N = 50_000_000;

    static final double X = 100.3;
    static final double Y = 64.0;
    static final double Z = 200.7;

    static void main(String[] args) {
        RandomSource random = RandomSource.create(1234L);
        var JAVA_PERLIN = net.minecraft.world.level.levelgen.synth.PerlinNoise.create(random, -2, 1.0, 1.0, 1.0, 1.0);

        PerlinNoiseNative noise = FFMFactory.load(PerlinNoiseNative.class);
        DoubleArrayList amplitudeList = new DoubleArrayList();
        amplitudeList.add(1);
        amplitudeList.add(1);
        amplitudeList.add(1);
        amplitudeList.add(1);
        long hwopt$nativePtr = noise.create(-2, amplitudeList.toDoubleArray(), amplitudeList.size(), true);
        System.out.println("Native: " + hwopt$nativePtr);

        // =============================
        // 1️⃣ Java -> C++ (Panama)
        // =============================
        long t0 = System.nanoTime();
        double sumNative = 0.0;

        for (int i = 0; N > i; i++) {
            sumNative += noise.getValue(hwopt$nativePtr, X + i * 0.001, Y, Z + i * 0.002);
        }

        long t1 = System.nanoTime();

        // =============================
        // 2️⃣ 纯 Java PerlinNoise
        // =============================
        long t2 = System.nanoTime();
        double sumJava = 0.0;

        for (int i = 0; N > i; i++) {
            sumJava += JAVA_PERLIN.getValue(X + i * 0.001, Y, Z + i * 0.002);
        }

        long t3 = System.nanoTime();

        double nativeSec = (t1 - t0) / 1.0e9;
        double javaSec   = (t3 - t2) / 1.0e9;

        System.out.println("Java -> C++ (Panama) time: " + nativeSec + " s");
        System.out.println("Pure Java PerlinNoise time: " + javaSec + " s");
        System.out.println("checksum(native): " + sumNative);
        System.out.println("checksum(java):   " + sumJava);
    }
}