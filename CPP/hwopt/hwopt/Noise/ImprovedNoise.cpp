#include "ImprovedNoise.h"

ImprovedNoise::ImprovedNoise() {
    std::random_device rd;
    std::mt19937_64 mt(rd());
    std::uniform_real_distribution dist_double(0.0, 1.0);

    this->xo = dist_double(mt) * 256.0;
    this->yo = dist_double(mt) * 256.0;
    this->zo = dist_double(mt) * 256.0;

    for (int i = 0; i < 256; i++) {
        this->p[i] = static_cast<char>(i);
    }

    for (int i = 0; i < 256; i++) {
        std::uniform_int_distribution d(0, 256 - i);
        const int offset = d(mt);
        const char tmp = this->p[i];
        this->p[i] = this->p[i + offset];
        this->p[i + offset] = tmp;
    }
}

auto ImprovedNoise::noise(const double _x, const double _y, const double _z) const -> double {
    return this->noise(_x, _y, _z, 0.0, 0.0);
}

auto ImprovedNoise::noise(const double _x, const double _y, const double _z, const double yScale, const double yFudge) const -> double {
    const double x = _x + this->xo;
    const double y = _y + this->yo;
    const double z = _z + this->zo;
    const int xf = std::floor(x);
    const int yf = std::floor(y);
    const int zf = std::floor(z);
    const double xr = x - xf;
    const double yr = y - yf;
    const double zr = z - zf;
    double yr_fudge;
    if (yScale != 0.0) {
        double fudge_limit;
        if (yFudge >= 0.0 && yFudge < yr) {
            fudge_limit = yFudge;
        } else {
            fudge_limit = yr;
        }

        yr_fudge = std::floor(fudge_limit / yScale + 1.0E-7F) * yScale;
    } else {
        yr_fudge = 0.0;
    }

    return this->sample_and_lerperm(xf, yf, zf, xr, yr - yr_fudge, zr, yr);
}

auto ImprovedNoise::noise_with_derivative(const double _x, const double _y, const double _z, double* derivativeOut) const -> double {
    const double x = _x + this->xo;
    const double y = _y + this->yo;
    const double z = _z + this->zo;
    const int xf = std::floor(x);
    const int yf = std::floor(y);
    const int zf = std::floor(z);
    const double xr = x - xf;
    const double yr = y - yf;
    const double zr = z - zf;
    return this->sample_with_derivative(xf, yf, zf, xr, yr, zr, derivativeOut);
}

auto ImprovedNoise::grad_dot(const int hash, const double x, const double y, const double z) -> double {
    return SimplexNoise::dot(SimplexNoise::GRADIENT[hash & 15], x, y, z);
}

auto ImprovedNoise::perm(const int x) const -> int {
    return this->p[x & 0xFF] & 0xFF;
}

auto ImprovedNoise::sample_and_lerperm(const int x, const int y, const int z, const double xr, const double yr, const double zr, const double yrOriginal) const -> double {
    const int x0 = this->perm(x);
    const int x1 = this->perm(x + 1);
    const int xy00 = this->perm(x0 + y);
    const int xy01 = this->perm(x0 + y + 1);
    const int xy10 = this->perm(x1 + y);
    const int xy11 = this->perm(x1 + y + 1);
    const double d000 = grad_dot(this->perm(xy00 + z), xr, yr, zr);
    const double d100 = grad_dot(this->perm(xy10 + z), xr - 1.0, yr, zr);
    const double d010 = grad_dot(this->perm(xy01 + z), xr, yr - 1.0, zr);
    const double d110 = grad_dot(this->perm(xy11 + z), xr - 1.0, yr - 1.0, zr);
    const double d001 = grad_dot(this->perm(xy00 + z + 1), xr, yr, zr - 1.0);
    const double d101 = grad_dot(this->perm(xy10 + z + 1), xr - 1.0, yr, zr - 1.0);
    const double d011 = grad_dot(this->perm(xy01 + z + 1), xr, yr - 1.0, zr - 1.0);
    const double d111 = grad_dot(this->perm(xy11 + z + 1), xr - 1.0, yr - 1.0, zr - 1.0);
    const double xAlpha = smoothstep(xr);
    const double yAlpha = smoothstep(yrOriginal);
    const double zAlpha = smoothstep(zr);
    return lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
}

auto ImprovedNoise::sample_with_derivative(const int x, const int y, const int z, const double xr, const double yr, const double zr, double* derivativeOut) const -> double {
    const int x0 = this->perm(x);
    const int x1 = this->perm(x + 1);
    const int xy00 = this->perm(x0 + y);
    const int xy01 = this->perm(x0 + y + 1);
    const int xy10 = this->perm(x1 + y);
    const int xy11 = this->perm(x1 + y + 1);
    const int p000 = this->perm(xy00 + z);
    const int p100 = this->perm(xy10 + z);
    const int p010 = this->perm(xy01 + z);
    const int p110 = this->perm(xy11 + z);
    const int p001 = this->perm(xy00 + z + 1);
    const int p101 = this->perm(xy10 + z + 1);
    const int p011 = this->perm(xy01 + z + 1);
    const int p111 = this->perm(xy11 + z + 1);
    const int* g000 = SimplexNoise::GRADIENT[p000 & 15];
    const int* g100 = SimplexNoise::GRADIENT[p100 & 15];
    const int* g010 = SimplexNoise::GRADIENT[p010 & 15];
    const int* g110 = SimplexNoise::GRADIENT[p110 & 15];
    const int* g001 = SimplexNoise::GRADIENT[p001 & 15];
    const int* g101 = SimplexNoise::GRADIENT[p101 & 15];
    const int* g011 = SimplexNoise::GRADIENT[p011 & 15];
    const int* g111 = SimplexNoise::GRADIENT[p111 & 15];
    const double d000 = SimplexNoise::dot(g000, xr, yr, zr);
    const double d100 = SimplexNoise::dot(g100, xr - 1.0, yr, zr);
    const double d010 = SimplexNoise::dot(g010, xr, yr - 1.0, zr);
    const double d110 = SimplexNoise::dot(g110, xr - 1.0, yr - 1.0, zr);
    const double d001 = SimplexNoise::dot(g001, xr, yr, zr - 1.0);
    const double d101 = SimplexNoise::dot(g101, xr - 1.0, yr, zr - 1.0);
    const double d011 = SimplexNoise::dot(g011, xr, yr - 1.0, zr - 1.0);
    const double d111 = SimplexNoise::dot(g111, xr - 1.0, yr - 1.0, zr - 1.0);
    const double xAlpha = smoothstep(xr);
    const double yAlpha = smoothstep(yr);
    const double zAlpha = smoothstep(zr);
    const double d1x = lerp3(xAlpha, yAlpha, zAlpha, g000[0], g100[0], g010[0], g110[0], g001[0], g101[0], g011[0], g111[0]);
    const double d1y = lerp3(xAlpha, yAlpha, zAlpha, g000[1], g100[1], g010[1], g110[1], g001[1], g101[1], g011[1], g111[1]);
    const double d1z = lerp3(xAlpha, yAlpha, zAlpha, g000[2], g100[2], g010[2], g110[2], g001[2], g101[2], g011[2], g111[2]);
    const double d2x = lerp2(yAlpha, zAlpha, d100 - d000, d110 - d010, d101 - d001, d111 - d011);
    const double d2y = lerp2(zAlpha, xAlpha, d010 - d000, d011 - d001, d110 - d100, d111 - d101);
    const double d2z = lerp2(xAlpha, yAlpha, d001 - d000, d101 - d100, d011 - d010, d111 - d110);
    const double xSD = smoothstep_derivative(xr);
    const double ySD = smoothstep_derivative(yr);
    const double zSD = smoothstep_derivative(zr);
    const double dX = d1x + xSD * d2x;
    const double dY = d1y + ySD * d2y;
    const double dZ = d1z + zSD * d2z;
    derivativeOut[0] += dX;
    derivativeOut[1] += dY;
    derivativeOut[2] += dZ;
    return lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
}

auto ImprovedNoise::smoothstep(const double x) -> double {
    return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
}

auto ImprovedNoise::smoothstep_derivative(const double x) -> double {
    return 30.0 * x * x * (x - 1.0) * (x - 1.0);
}

auto ImprovedNoise::lerp(const double alpha1, const double p0, const double p1) -> double {
    return p0 + alpha1 * (p1 - p0);
}

auto ImprovedNoise::lerp2(const double alpha1, const double alpha2, const double x00, const double x10, const double x01, const double x11) -> double {
    return lerp(alpha2, lerp(alpha1, x00, x10), lerp(alpha1, x01, x11));
}

auto ImprovedNoise::lerp3(const double alpha1,
        const double alpha2,
        const double alpha3,
        const double x000,
        const double x100,
        const double x010,
        const double x110,
        const double x001,
        const double x101,
        const double x011,
        const double x111) -> double {
    return lerp(alpha3, lerp2(alpha1, alpha2, x000, x100, x010, x110), lerp2(alpha1, alpha2, x001, x101, x011, x111));
}
