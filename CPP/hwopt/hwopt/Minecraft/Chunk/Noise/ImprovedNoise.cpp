// 2026-06-17 03:23:08

#include "ImprovedNoise.hpp"
#include <cmath>
#include <random>

#include "SimplexNoise.hpp"

#include "../../Math.hpp"
#include "../../../util.hpp"
using namespace minecraft::noise;
using namespace minecraft::math;

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
    std::span derivative_span(derivative_out, derivative_out_size);
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

auto ImprovedNoise::get_values(const double* xs, int xs_len, const double* ys, int ys_len, const double* zs, int zs_len, double* result, const int result_len) const -> void {
    for (int i = 0; i < result_len; i++) {
        result[i] = noise(xs[i], ys[i], zs[i]);
    }
}

auto ImprovedNoise::add_methods() -> void {
    "ImprovedNoise::_create"_jf.reg<_create>();
    "ImprovedNoise::_destroy"_jf.reg<&ImprovedNoise::_destroy>();
    "ImprovedNoise::noise3"_jf.reg<static_cast<double(ImprovedNoise::*)(double, double, double) const>(&ImprovedNoise::noise)>();
    "ImprovedNoise::noise5"_jf.reg<static_cast<double(ImprovedNoise::*)(double, double, double, double, double) const>(&ImprovedNoise::noise)>();
    "ImprovedNoise::grad_dot"_jf.reg<&ImprovedNoise::grad_dot>();
    "ImprovedNoise::sample_and_lerperm"_jf.reg<&ImprovedNoise::sample_and_lerperm>();
    "ImprovedNoise::sample_with_derivative"_jf.reg<&ImprovedNoise::sample_with_derivative>();
    "ImprovedNoise::perm"_jf.reg<&ImprovedNoise::perm>();
    "ImprovedNoise::noise_with_derivative"_jf.reg<&ImprovedNoise::noise_with_derivative>();
    "ImprovedNoise::get_values"_jf.reg<&ImprovedNoise::get_values>();
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

auto ImprovedNoise::grad_dot(const int hash, const double x, const double y, const double z) -> double {
    const int idx = (hash & 15) * 3;
    return (GRADIENT[idx] * x) + (GRADIENT[idx + 1] * y) + (GRADIENT[idx + 2] * z);
}

auto ImprovedNoise::perm(const int x) const -> int {
    return this->p[x & 0xFF] & 0xFF;
}

auto ImprovedNoise::sample_and_lerperm(const int x, const int y, const int z, const double xr, const double yr, const double zr, const double yr_original) const -> double {
    const int x0 = perm(x) + y;
    const int x1 = perm(x + 1) + y;
    const int xy00 = perm(x0) + z;
    const int xy01 = perm(x0 + 1) + z;
    const int xy10 = perm(x1) + z;
    const int xy11 = perm(x1 + 1) + z;
    const double xr_sub = xr - 1.0;
    const double yr_sub = yr - 1.0;
    const double zr_sub = zr - 1.0;
    const double d000 = grad_dot(perm(xy00), xr, yr, zr);
    const double d100 = grad_dot(perm(xy10), xr_sub, yr, zr);
    const double d010 = grad_dot(perm(xy01), xr, yr_sub, zr);
    const double d110 = grad_dot(perm(xy11), xr_sub, yr_sub, zr);
    const double d001 = grad_dot(perm(xy00 + 1), xr, yr, zr_sub);
    const double d101 = grad_dot(perm(xy10 + 1), xr_sub, yr, zr_sub);
    const double d011 = grad_dot(perm(xy01 + 1), xr, yr_sub, zr_sub);
    const double d111 = grad_dot(perm(xy11 + 1), xr_sub, yr_sub, zr_sub);
    return Math::lerp3(Math::smoothstep(xr), Math::smoothstep(yr_original), Math::smoothstep(zr), d000, d100, d010, d110, d001, d101, d011, d111);
}

auto ImprovedNoise::sample_with_derivative(const int x,
                                           const int y,
                                           const int z,
                                           const double xr,
                                           const double yr,
                                           const double zr,
                                           double* derivative_out,
                                           const int derivative_out_size) const -> double {
    const int x0 = perm(x) + y;
    const int x1 = perm(x + 1) + y;
    const int xy00 = perm(x0) + z;
    const int xy01 = perm(x0 + 1) + z;
    const int xy10 = perm(x1) + z;
    const int xy11 = perm(x1 + 1) + z;

    const int idx000 = (perm(xy00) & 15) * 3;
    const int idx100 = (perm(xy10) & 15) * 3;
    const int idx010 = (perm(xy01) & 15) * 3;
    const int idx110 = (perm(xy11) & 15) * 3;
    const int idx001 = (perm(xy00 + 1) & 15) * 3;
    const int idx101 = (perm(xy10 + 1) & 15) * 3;
    const int idx011 = (perm(xy01 + 1) & 15) * 3;
    const int idx111 = (perm(xy11 + 1) & 15) * 3;

    const double* g000 = &GRADIENT[idx000];
    const double* g100 = &GRADIENT[idx100];
    const double* g010 = &GRADIENT[idx010];
    const double* g110 = &GRADIENT[idx110];
    const double* g001 = &GRADIENT[idx001];
    const double* g101 = &GRADIENT[idx101];
    const double* g011 = &GRADIENT[idx011];
    const double* g111 = &GRADIENT[idx111];

    const double xr_sub = xr - 1.0;
    const double yr_sub = yr - 1.0;
    const double zr_sub = zr - 1.0;

    const double d000 = SimplexNoise::dot(g000, xr, yr, zr);
    const double d100 = SimplexNoise::dot(g100, xr_sub, yr, zr);
    const double d010 = SimplexNoise::dot(g010, xr, yr_sub, zr);
    const double d110 = SimplexNoise::dot(g110, xr_sub, yr_sub, zr);
    const double d001 = SimplexNoise::dot(g001, xr, yr, zr_sub);
    const double d101 = SimplexNoise::dot(g101, xr_sub, yr, zr_sub);
    const double d011 = SimplexNoise::dot(g011, xr, yr_sub, zr_sub);
    const double d111 = SimplexNoise::dot(g111, xr_sub, yr_sub, zr_sub);

    const double xAlpha = Math::smoothstep(xr);
    const double yAlpha = Math::smoothstep(yr);
    const double zAlpha = Math::smoothstep(zr);

    const double d1x = Math::lerp3(xAlpha, yAlpha, zAlpha, g000[0], g100[0], g010[0], g110[0], g001[0], g101[0], g011[0], g111[0]);
    const double d1y = Math::lerp3(xAlpha, yAlpha, zAlpha, g000[1], g100[1], g010[1], g110[1], g001[1], g101[1], g011[1], g111[1]);
    const double d1z = Math::lerp3(xAlpha, yAlpha, zAlpha, g000[2], g100[2], g010[2], g110[2], g001[2], g101[2], g011[2], g111[2]);

    const double d2x = Math::lerp2(yAlpha, zAlpha, d100 - d000, d110 - d010, d101 - d001, d111 - d011);
    const double d2y = Math::lerp2(zAlpha, xAlpha, d010 - d000, d011 - d001, d110 - d100, d111 - d101);
    const double d2z = Math::lerp2(xAlpha, yAlpha, d001 - d000, d101 - d100, d011 - d010, d111 - d110);

    derivative_out[0] += d1x + (Math::smoothstep_derivative(xr) * d2x);
    derivative_out[1] += d1y + (Math::smoothstep_derivative(yr) * d2y);
    derivative_out[2] += d1z + (Math::smoothstep_derivative(zr) * d2z);

    return Math::lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
}
