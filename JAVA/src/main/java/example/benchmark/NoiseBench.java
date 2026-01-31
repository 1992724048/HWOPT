package example.benchmark;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import library.dll.PerlinNoiseNative;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import static library.dll.PerlinNoiseNative.NATIVE;

public enum NoiseBench {
    ;
    static final int N = 5000000;

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
        hwopt$nativePtr = NATIVE.create(1234L, -2, amplitudeList.toDoubleArray(), amplitudeList.size(), true);
    }
    
    static void main(String[] args) {
        final int RUNS = 10;
        
        double nativeTotal = 0.0;
        double javaTotal   = 0.0;
        
        double checksumNative = 0.0;
        double checksumJava   = 0.0;
        
        for (int r = 0; r < RUNS; r++) {
            // Java -> C++ (FFM / Panama)
            long t0 = System.nanoTime();
            double sumNative = 0.0;
            
            for (int i = 0; i < N; i++) {
                sumNative += hwopt$nativePtr.getValue(
                        X + i * 0.001,
                        Y,
                        Z + i * 0.002
                );
            }
            
            long t1 = System.nanoTime();
            double nativeSec = (t1 - t0) / 1.0e9;
            nativeTotal += nativeSec;
            checksumNative = sumNative;
            
            // Java
            t0 = System.nanoTime();
            double sumJava = 0.0;
            
            for (int i = 0; i < N; i++) {
                sumJava += JAVA_PERLIN.getValue(
                        X + i * 0.001,
                        Y,
                        Z + i * 0.002
                );
            }
            
            t1 = System.nanoTime();
            double javaSec = (t1 - t0) / 1.0e9;
            javaTotal += javaSec;
            checksumJava = sumJava;
        }
        
        double nativeAvg = nativeTotal / RUNS;
        double javaAvg   = javaTotal   / RUNS;
        
        double improvement = (javaAvg - nativeAvg) / javaAvg * 100.0;
        
        System.out.println("| Benchmark | Baseline | Optimized | Improvement |");
        System.out.println("|----------------------|-------------------|--------------------|------------------------|");
        
        System.out.printf(
                "| NoiseBench           | %.3f ms           | %.3f ms            | %+6.2f%%                |\n",
                javaAvg * 1000.0,
                nativeAvg * 1000.0,
                improvement
        );
        
        System.out.println();
        System.out.println("checksum(native): " + checksumNative);
        System.out.println("checksum(java):   " + checksumJava);
    }
}