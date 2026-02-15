// 遂沫 ImprovedNoise.cpp
// 2026-02-15 22:26:33

#include "ImprovedNoise.h"
#include <array>
#include <random>
using namespace minecraft_sycl;

ImprovedNoise::ImprovedNoise(std::mt19937_64& mt) {
    std::uniform_real_distribution dist_double(0.0, 1.0);

    this->xo = dist_double(mt) * 256.0;
    this->yo = dist_double(mt) * 256.0;
    this->zo = dist_double(mt) * 256.0;

    for (int i = 0; i < 256; i++) {
        this->p[i] = static_cast<char>(i);
    }

    for (int i = 0; i < 256; i++) {
        std::uniform_int_distribution d(0, 255 - i);
        const int offset = d(mt);
        const uint8_t tmp = this->p[i];
        this->p[i] = this->p[static_cast<std::array<uint8_t, 256Ui64>::size_type>(i) + offset];
        this->p[static_cast<std::array<uint8_t, 256Ui64>::size_type>(i) + offset] = tmp;
    }
}

SYCL_EXTERNAL auto ImprovedNoise::noise(const double x, const double y, const double z) const -> double {
    return this->noise(x, y, z, 0.0, 0.0);
}

SYCL_EXTERNAL auto ImprovedNoise::noise(const double _x, const double _y, const double _z, const double yScale, const double yFudge) const -> double {
    const double x = _x + this->xo;
    const double y = _y + this->yo;
    const double z = _z + this->zo;
    const double xf = sycl::floor(x);
    const double yf = sycl::floor(y);
    const double zf = sycl::floor(z);
    const double xr = x - xf;
    const double yr = y - yf;
    const double zr = z - zf;
    const double fudge_limit = sycl::clamp(yFudge, .0, yr);
    const double yr_fudge = yScale != 0.0 ? sycl::floor(fudge_limit / yScale + 1e-7f) * yScale : 0.0;

    return this->sample_and_lerperm(xf, yf, zf, xr, yr - yr_fudge, zr, yr);
}

SYCL_EXTERNAL inline auto ImprovedNoise::grad_dot(const int hash, const double x, const double y, const double z) -> double {
    const int idx = (hash & 15) * 3;
    return GRADIENT[idx] * x + GRADIENT[idx + 1] * y + GRADIENT[idx + 2] * z;
}

SYCL_EXTERNAL auto ImprovedNoise::perm(const int x) const -> int {
    return this->p[x & 0xFF] & 0xFF;
}

SYCL_EXTERNAL auto ImprovedNoise::sample_and_lerperm(const int x, const int y, const int z, const double xr, const double yr, const double zr, const double yr_original) const -> double {
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
    const double yAlpha = smoothstep(yr_original);
    const double zAlpha = smoothstep(zr);
    return lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
}

SYCL_EXTERNAL inline auto ImprovedNoise::smoothstep(const double x) -> double {
    return ((x * 6.0 - 15.0) * x + 10.0) * x * x * x;
}

SYCL_EXTERNAL inline auto ImprovedNoise::lerp(const double alpha1, const double p0, const double p1) -> double {
    return sycl::fma(alpha1, p1 - p0, p0);
}

SYCL_EXTERNAL inline auto ImprovedNoise::lerp2(const double alpha1, const double alpha2, const double x00, const double x10, const double x01, const double x11) -> double {
    return lerp(alpha2, lerp(alpha1, x00, x10), lerp(alpha1, x01, x11));
}

SYCL_EXTERNAL inline auto ImprovedNoise::lerp3(const double alpha1,
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
