package com.worldgen.mixin;

import com.google.common.annotations.VisibleForTesting;
import library.dll.PerlinNoiseNative;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.NoiseUtils;
import org.spongepowered.asm.mixin.*;

import java.lang.ref.Cleaner;

@Mixin(ImprovedNoise.class)
public abstract class ImprovedNoiseMixin {

    @Shadow
    @Final
    public double xo;
    @Shadow
    @Final
    public double yo;
    @Shadow
    @Final
    public double zo;
    @Shadow
    @Final
    private byte[] p;
    
    @Unique
    private PerlinNoiseNative hwopt$nativePtr;
    @Unique
    private Cleaner.Cleanable hwopt$cleanable;
    @Unique
    private static final Cleaner hwopt$CLEANER = Cleaner.create();
    @Unique
    private static final int[][] GRADIENT = new int[][]{
            {1, 1, 0},
            {-1, 1, 0},
            {1, -1, 0},
            {-1, -1, 0},
            {1, 0, 1},
            {-1, 0, 1},
            {1, 0, -1},
            {-1, 0, -1},
            {0, 1, 1},
            {0, -1, 1},
            {0, 1, -1},
            {0, -1, -1},
            {1, 1, 0},
            {0, -1, 1},
            {-1, 1, 0},
            {0, -1, -1}
    };

    @Overwrite
    public double noise(double _x, double _y, double _z) {
        return this.noise(_x, _y, _z, 0.0, 0.0);
    }
    
    @Overwrite
    @Deprecated
    public double noise(double _x, double _y, double _z, double yScale, double yFudge) {
        double x = _x + this.xo;
        double y = _y + this.yo;
        double z = _z + this.zo;
        int xf = Mth.floor(x);
        int yf = Mth.floor(y);
        int zf = Mth.floor(z);
        double xr = x - xf;
        double yr = y - yf;
        double zr = z - zf;
        double yrFudge;
        if (yScale != 0.0) {
            double fudgeLimit;
            if (yFudge >= 0.0 && yFudge < yr) {
                fudgeLimit = yFudge;
            } else {
                fudgeLimit = yr;
            }

            yrFudge = Mth.floor(fudgeLimit / yScale + 1.0E-7F) * yScale;
        } else {
            yrFudge = 0.0;
        }

        return this.sampleAndLerp(xf, yf, zf, xr, yr - yrFudge, zr, yr);
    }

    @Overwrite
    public double noiseWithDerivative(double _x, double _y, double _z, double[] derivativeOut) {
        double x = _x + this.xo;
        double y = _y + this.yo;
        double z = _z + this.zo;
        int xf = Mth.floor(x);
        int yf = Mth.floor(y);
        int zf = Mth.floor(z);
        double xr = x - xf;
        double yr = y - yf;
        double zr = z - zf;
        return this.sampleWithDerivative(xf, yf, zf, xr, yr, zr, derivativeOut);
    }

    @Overwrite
    private static double gradDot(int hash, double x, double y, double z) {
        return dot(GRADIENT[hash & 15], x, y, z);
    }

    @Overwrite
    private int p(int x) {
        return this.p[x & 0xFF] & 0xFF;
    }

    @Overwrite
    private double sampleAndLerp(int x, int y, int z, double xr, double yr, double zr, double yrOriginal) {
        int x0 = this.p(x);
        int x1 = this.p(x + 1);
        int xy00 = this.p(x0 + y);
        int xy01 = this.p(x0 + y + 1);
        int xy10 = this.p(x1 + y);
        int xy11 = this.p(x1 + y + 1);
        double d000 = gradDot(this.p(xy00 + z), xr, yr, zr);
        double d100 = gradDot(this.p(xy10 + z), xr - 1.0, yr, zr);
        double d010 = gradDot(this.p(xy01 + z), xr, yr - 1.0, zr);
        double d110 = gradDot(this.p(xy11 + z), xr - 1.0, yr - 1.0, zr);
        double d001 = gradDot(this.p(xy00 + z + 1), xr, yr, zr - 1.0);
        double d101 = gradDot(this.p(xy10 + z + 1), xr - 1.0, yr, zr - 1.0);
        double d011 = gradDot(this.p(xy01 + z + 1), xr, yr - 1.0, zr - 1.0);
        double d111 = gradDot(this.p(xy11 + z + 1), xr - 1.0, yr - 1.0, zr - 1.0);
        double xAlpha = Mth.smoothstep(xr);
        double yAlpha = Mth.smoothstep(yrOriginal);
        double zAlpha = Mth.smoothstep(zr);
        return Mth.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }

    @Overwrite
    private double sampleWithDerivative(int x, int y, int z, double xr, double yr, double zr, double[] derivativeOut) {
        int x0 = this.p(x);
        int x1 = this.p(x + 1);
        int xy00 = this.p(x0 + y);
        int xy01 = this.p(x0 + y + 1);
        int xy10 = this.p(x1 + y);
        int xy11 = this.p(x1 + y + 1);
        int p000 = this.p(xy00 + z);
        int p100 = this.p(xy10 + z);
        int p010 = this.p(xy01 + z);
        int p110 = this.p(xy11 + z);
        int p001 = this.p(xy00 + z + 1);
        int p101 = this.p(xy10 + z + 1);
        int p011 = this.p(xy01 + z + 1);
        int p111 = this.p(xy11 + z + 1);
        int[] g000 = GRADIENT[p000 & 15];
        int[] g100 = GRADIENT[p100 & 15];
        int[] g010 = GRADIENT[p010 & 15];
        int[] g110 = GRADIENT[p110 & 15];
        int[] g001 = GRADIENT[p001 & 15];
        int[] g101 = GRADIENT[p101 & 15];
        int[] g011 = GRADIENT[p011 & 15];
        int[] g111 = GRADIENT[p111 & 15];
        double d000 = dot(g000, xr, yr, zr);
        double d100 = dot(g100, xr - 1.0, yr, zr);
        double d010 = dot(g010, xr, yr - 1.0, zr);
        double d110 = dot(g110, xr - 1.0, yr - 1.0, zr);
        double d001 = dot(g001, xr, yr, zr - 1.0);
        double d101 = dot(g101, xr - 1.0, yr, zr - 1.0);
        double d011 = dot(g011, xr, yr - 1.0, zr - 1.0);
        double d111 = dot(g111, xr - 1.0, yr - 1.0, zr - 1.0);
        double xAlpha = Mth.smoothstep(xr);
        double yAlpha = Mth.smoothstep(yr);
        double zAlpha = Mth.smoothstep(zr);
        double d1x = Mth.lerp3(xAlpha, yAlpha, zAlpha, g000[0], g100[0], g010[0], g110[0], g001[0], g101[0], g011[0], g111[0]);
        double d1y = Mth.lerp3(xAlpha, yAlpha, zAlpha, g000[1], g100[1], g010[1], g110[1], g001[1], g101[1], g011[1], g111[1]);
        double d1z = Mth.lerp3(xAlpha, yAlpha, zAlpha, g000[2], g100[2], g010[2], g110[2], g001[2], g101[2], g011[2], g111[2]);
        double d2x = Mth.lerp2(yAlpha, zAlpha, d100 - d000, d110 - d010, d101 - d001, d111 - d011);
        double d2y = Mth.lerp2(zAlpha, xAlpha, d010 - d000, d011 - d001, d110 - d100, d111 - d101);
        double d2z = Mth.lerp2(xAlpha, yAlpha, d001 - d000, d101 - d100, d011 - d010, d111 - d110);
        double xSD = Mth.smoothstepDerivative(xr);
        double ySD = Mth.smoothstepDerivative(yr);
        double zSD = Mth.smoothstepDerivative(zr);
        double dX = d1x + xSD * d2x;
        double dY = d1y + ySD * d2y;
        double dZ = d1z + zSD * d2z;
        derivativeOut[0] += dX;
        derivativeOut[1] += dY;
        derivativeOut[2] += dZ;
        return Mth.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }

    @Overwrite
    @VisibleForTesting
    public void parityConfigString(StringBuilder sb) {
        NoiseUtils.parityNoiseOctaveConfigString(sb, this.xo, this.yo, this.zo, this.p);
    }
    
    @Unique
    private static double dot(int[] g, double x, double y, double z) {
        return g[0] * x + g[1] * y + g[2] * z;
    }
}
