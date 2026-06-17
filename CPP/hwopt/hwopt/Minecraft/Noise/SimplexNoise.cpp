// 2026-06-17 03:26:03

#include "SimplexNoise.h"

#include "../../util.h"
using namespace minecraft;

SimplexNoise::SimplexNoise() {
    JavaNative::touch();
}

auto SimplexNoise::add_methods() -> void {
    "SimplexNoise::_create"_jf.reg<_create>();
    "SimplexNoise::_destroy"_jf.reg<&SimplexNoise::_destroy>();
    "SimplexNoise::get_value2"_jf.reg<&SimplexNoise::get_value2>();
    "SimplexNoise::get_value3"_jf.reg<&SimplexNoise::get_value3>();
}

auto SimplexNoise::_create(const double xo, const double yo, const double zo, int* array, const int array_size) -> SimplexNoise* {
    auto* noise = hwopt::util::mi_new<SimplexNoise>();
    noise->xo = xo;
    noise->yo = yo;
    noise->zo = zo;
    std::memcpy(noise->p.data(), array, array_size * sizeof(int));
    return noise;
}

auto SimplexNoise::_destroy() const -> void {
    hwopt::util::mi_delete(this);
}

auto SimplexNoise::get_value2(const double xin, const double yin) const -> double {
    const double s = (xin + yin) * F2;
    const int i = static_cast<int>(std::floor(xin + s));
    const int j = static_cast<int>(std::floor(yin + s));
    const double t = static_cast<double>(i + j) * G2;
    const double x0 = xin - (static_cast<double>(i) - t);
    const double y0 = yin - (static_cast<double>(j) - t);

    int i1;
    int j1;
    if (x0 > y0) {
        i1 = 1;
        j1 = 0;
    } else {
        i1 = 0;
        j1 = 1;
    }

    const double x1 = x0 - static_cast<double>(i1) + G2;
    const double y1 = y0 - static_cast<double>(j1) + G2;
    const double x2 = x0 - 1.0 + (2.0 * G2);
    const double y2 = y0 - 1.0 + (2.0 * G2);

    const int ii = i & 0xFF;
    const int jj = j & 0xFF;
    const int gi0 = perm(ii + perm(jj)) % 12;
    const int gi1 = perm(ii + i1 + perm(jj + j1)) % 12;
    const int gi2 = perm(ii + 1 + perm(jj + 1)) % 12;

    const double n0 = get_corner_noise3d(gi0, x0, y0, 0.0, 0.5);
    const double n1 = get_corner_noise3d(gi1, x1, y1, 0.0, 0.5);
    const double n2 = get_corner_noise3d(gi2, x2, y2, 0.0, 0.5);

    return 70.0 * (n0 + n1 + n2);
}

auto SimplexNoise::get_value3(const double xin, const double yin, const double zin) const -> double {
    constexpr double F3 = 0.3333333333333333;
    constexpr double G3 = 0.16666666666666666;

    const double s = (xin + yin + zin) * F3;
    const int i = static_cast<int>(std::floor(xin + s));
    const int j = static_cast<int>(std::floor(yin + s));
    const int k = static_cast<int>(std::floor(zin + s));
    const double t = static_cast<double>(i + j + k) * G3;
    const double x0 = xin - (static_cast<double>(i) - t);
    const double y0 = yin - (static_cast<double>(j) - t);
    const double z0 = zin - (static_cast<double>(k) - t);

    int i1;
    int j1;
    int k1;
    int i2;
    int j2;
    int k2;
    if (x0 >= y0) {
        if (y0 >= z0) {
            i1 = 1;
            j1 = 0;
            k1 = 0;
            i2 = 1;
            j2 = 1;
            k2 = 0;
        } else if (x0 >= z0) {
            i1 = 1;
            j1 = 0;
            k1 = 0;
            i2 = 1;
            j2 = 0;
            k2 = 1;
        } else {
            i1 = 0;
            j1 = 0;
            k1 = 1;
            i2 = 1;
            j2 = 0;
            k2 = 1;
        }
    } else if (y0 < z0) {
        i1 = 0;
        j1 = 0;
        k1 = 1;
        i2 = 0;
        j2 = 1;
        k2 = 1;
    } else if (x0 < z0) {
        i1 = 0;
        j1 = 1;
        k1 = 0;
        i2 = 0;
        j2 = 1;
        k2 = 1;
    } else {
        i1 = 0;
        j1 = 1;
        k1 = 0;
        i2 = 1;
        j2 = 1;
        k2 = 0;
    }

    const double x1 = x0 - static_cast<double>(i1) + G3;
    const double y1 = y0 - static_cast<double>(j1) + G3;
    const double z1 = z0 - static_cast<double>(k1) + G3;
    const double x2 = x0 - static_cast<double>(i2) + (2.0 * G3);
    const double y2 = y0 - static_cast<double>(j2) + (2.0 * G3);
    const double z2 = z0 - static_cast<double>(k2) + (2.0 * G3);
    const double x3 = x0 - 1.0 + 0.5;
    const double y3 = y0 - 1.0 + 0.5;
    const double z3 = z0 - 1.0 + 0.5;

    const int ii = i & 0xFF;
    const int jj = j & 0xFF;
    const int kk = k & 0xFF;
    const int gi0 = perm(ii + perm(jj + perm(kk))) % 12;
    const int gi1 = perm(ii + i1 + perm(jj + j1 + perm(kk + k1))) % 12;
    const int gi2 = perm(ii + i2 + perm(jj + j2 + perm(kk + k2))) % 12;
    const int gi3 = perm(ii + 1 + perm(jj + 1 + perm(kk + 1))) % 12;

    const double n0 = get_corner_noise3d(gi0, x0, y0, z0, 0.6);
    const double n1 = get_corner_noise3d(gi1, x1, y1, z1, 0.6);
    const double n2 = get_corner_noise3d(gi2, x2, y2, z2, 0.6);
    const double n3 = get_corner_noise3d(gi3, x3, y3, z3, 0.6);

    return 32.0 * (n0 + n1 + n2 + n3);
}

auto SimplexNoise::perm(const int x) const -> int {
    return this->p[x & 0xFF] & 0xFF;
}

auto SimplexNoise::dot(const int* g, const double x, const double y, const double z) -> double {
    return (g[0] * x) + (g[1] * y) + (g[2] * z);
}

auto SimplexNoise::get_corner_noise3d(const int index, const double x, const double y, const double z, const double base) -> double {
    double t = base - (x * x) - (y * y) - (z * z);
    if (t < 0.0) {
        return 0.0;
    }
    t *= t;
    return t * t * dot(&GRADIENT[index * 3], x, y, z);
}
