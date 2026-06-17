// 2026-06-17 03:23:08

#include "ImprovedNoise.h"
#include <cmath>
#include <random>

#include "../../util.h"
using namespace minecraft;

static constexpr double SHIFT_UP_EPSILON = 1.0000000116860974E-7;

ImprovedNoise::ImprovedNoise(const double xo, const double yo, const double zo, const std::span<int8_t> perm) : xo(xo),
                                                                                                                yo(yo),
                                                                                                                zo(zo) {
    JavaNative::touch();
    std::memcpy(this->p.data(), perm.data(), perm.size());
}

auto ImprovedNoise::noise(const double x, const double y, const double z) const -> double {
    return noise(x, y, z, 0.0, 0.0);
}

auto ImprovedNoise::noise(const double x, const double y, const double z, const double y_scale, const double y_fudge) const -> double {
    const double x1 = x + this->xo;
    const double y1 = y + this->yo;
    const double z1 = z + this->zo;
    const double xf = std::floor(x1);
    const double yf = std::floor(y1);
    const double zf = std::floor(z1);
    const double xr = x1 - xf;
    const double yr = y1 - yf;
    const double zr = z1 - zf;

    double fudge_limit;
    if (y_fudge >= 0.0 && y_fudge < yr) {
        fudge_limit = y_fudge;
    } else {
        fudge_limit = yr;
    }

    const double yr_fudge = y_scale != 0.0 ? std::floor((fudge_limit / y_scale) + SHIFT_UP_EPSILON) * y_scale : 0.0;
    return sample_and_lerperm(xf, yf, zf, xr, yr - yr_fudge, zr, yr);
}

auto ImprovedNoise::noise_with_derivative(const double x, const double y, const double z, double* derivative_out, const int derivative_out_size) const -> double {
    std::span<double> derivative_span(derivative_out, derivative_out_size);
    const double x1 = x + this->xo;
    const double y1 = y + this->yo;
    const double z1 = z + this->zo;
    const double xf = std::floor(x1);
    const double yf = std::floor(y1);
    const double zf = std::floor(z1);
    const double xr = x1 - xf;
    const double yr = y1 - yf;
    const double zr = z1 - zf;
    return sample_with_derivative(xf, yf, zf, xr, yr, zr, derivative_span.data(), derivative_span.size());
}

auto ImprovedNoise::add_methods() -> void {
    "ImprovedNoise::_create"_jf.reg<_create>();
    "ImprovedNoise::_destroy"_jf.reg<&ImprovedNoise::_destroy>();
    "ImprovedNoise::noise"_jf.reg<static_cast<double(ImprovedNoise::*)(double, double, double, double, double) const>(&ImprovedNoise::noise)>();
    "ImprovedNoise::grad_dot"_jf.reg<&ImprovedNoise::grad_dot>();
    "ImprovedNoise::sample_and_lerperm"_jf.reg<&ImprovedNoise::sample_and_lerperm>();
    "ImprovedNoise::sample_with_derivative"_jf.reg<&ImprovedNoise::sample_with_derivative>();
    "ImprovedNoise::perm"_jf.reg<&ImprovedNoise::perm>();
    "ImprovedNoise::noise_with_derivative"_jf.reg<&ImprovedNoise::noise_with_derivative>();
}

auto ImprovedNoise::_create(const double xo, const double yo, const double zo, const int8_t* array, const int array_size) -> ImprovedNoise* {
    auto* noise = hwopt::util::mi_new<ImprovedNoise>();
    noise->xo = xo;
    noise->yo = yo;
    noise->zo = zo;
    std::memcpy(noise->p.data(), array, array_size);
    return noise;
}

auto ImprovedNoise::_destroy() const -> void {
    hwopt::util::mi_delete(this);
}

inline auto ImprovedNoise::grad_dot(const int hash, const double x, const double y, const double z) -> double {
    const int idx = (hash & 15) * 3;
    return (GRADIENT[idx] * x) + (GRADIENT[idx + 1] * y) + (GRADIENT[idx + 2] * z);
}

inline auto ImprovedNoise::perm(const int x) const -> int {
    return this->p[x & 0xFF] & 0xFF;
}

inline auto ImprovedNoise::sample_and_lerperm(const int x, const int y, const int z, const double xr, const double yr, const double zr, const double yr_original) const -> double {
    const int x0 = perm(x);
    const int x1 = perm(x + 1);
    const int xy00 = perm(x0 + y);
    const int xy01 = perm(x0 + y + 1);
    const int xy10 = perm(x1 + y);
    const int xy11 = perm(x1 + y + 1);
    const double d000 = grad_dot(perm(xy00 + z), xr, yr, zr);
    const double d100 = grad_dot(perm(xy10 + z), xr - 1.0, yr, zr);
    const double d010 = grad_dot(perm(xy01 + z), xr, yr - 1.0, zr);
    const double d110 = grad_dot(perm(xy11 + z), xr - 1.0, yr - 1.0, zr);
    const double d001 = grad_dot(perm(xy00 + z + 1), xr, yr, zr - 1.0);
    const double d101 = grad_dot(perm(xy10 + z + 1), xr - 1.0, yr, zr - 1.0);
    const double d011 = grad_dot(perm(xy01 + z + 1), xr, yr - 1.0, zr - 1.0);
    const double d111 = grad_dot(perm(xy11 + z + 1), xr - 1.0, yr - 1.0, zr - 1.0);
    return lerp3(smoothstep(xr), smoothstep(yr_original), smoothstep(zr), d000, d100, d010, d110, d001, d101, d011, d111);
}

inline auto ImprovedNoise::sample_with_derivative(const int x,
                                                  const int y,
                                                  const int z,
                                                  const double xr,
                                                  const double yr,
                                                  const double zr,
                                                  double* derivative_out,
                                                  const int derivative_out_size) const -> double {
    const int x0 = perm(x);
    const int x1 = perm(x + 1);
    const int xy00 = perm(x0 + y);
    const int xy01 = perm(x0 + y + 1);
    const int xy10 = perm(x1 + y);
    const int xy11 = perm(x1 + y + 1);

    const int idx000 = (perm(xy00 + z) & 15) * 3;
    const int idx100 = (perm(xy10 + z) & 15) * 3;
    const int idx010 = (perm(xy01 + z) & 15) * 3;
    const int idx110 = (perm(xy11 + z) & 15) * 3;
    const int idx001 = (perm(xy00 + z + 1) & 15) * 3;
    const int idx101 = (perm(xy10 + z + 1) & 15) * 3;
    const int idx011 = (perm(xy01 + z + 1) & 15) * 3;
    const int idx111 = (perm(xy11 + z + 1) & 15) * 3;

    const int* g000 = &GRADIENT[idx000];
    const int* g100 = &GRADIENT[idx100];
    const int* g010 = &GRADIENT[idx010];
    const int* g110 = &GRADIENT[idx110];
    const int* g001 = &GRADIENT[idx001];
    const int* g101 = &GRADIENT[idx101];
    const int* g011 = &GRADIENT[idx011];
    const int* g111 = &GRADIENT[idx111];

    const double d000 = dot(g000, xr, yr, zr);
    const double d100 = dot(g100, xr - 1.0, yr, zr);
    const double d010 = dot(g010, xr, yr - 1.0, zr);
    const double d110 = dot(g110, xr - 1.0, yr - 1.0, zr);
    const double d001 = dot(g001, xr, yr, zr - 1.0);
    const double d101 = dot(g101, xr - 1.0, yr, zr - 1.0);
    const double d011 = dot(g011, xr, yr - 1.0, zr - 1.0);
    const double d111 = dot(g111, xr - 1.0, yr - 1.0, zr - 1.0);

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

    derivative_out[0] += d1x + (xSD * d2x);
    derivative_out[1] += d1y + (ySD * d2y);
    derivative_out[2] += d1z + (zSD * d2z);

    return lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
}

inline auto ImprovedNoise::smoothstep(const double x) -> double {
    return ((((x * 6.0) - 15.0) * x) + 10.0) * x * x * x;
}

inline auto ImprovedNoise::smoothstep_derivative(const double x) -> double {
    const double t = x * (x - 1.0);
    return 30.0 * t * t;
}

inline auto ImprovedNoise::lerp(const double alpha1, const double p0, const double p1) -> double {
    return p0 + (alpha1 * (p1 - p0));
}

inline auto ImprovedNoise::lerp2(const double alpha1, const double alpha2, const double x00, const double x10, const double x01, const double x11) -> double {
    return lerp(alpha2, lerp(alpha1, x00, x10), lerp(alpha1, x01, x11));
}

inline auto ImprovedNoise::lerp3(const double alpha1,
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

inline auto ImprovedNoise::dot(const int* g, const double x, const double y, const double z) -> double {
    return (g[0] * x) + (g[1] * y) + (g[2] * z);
}
