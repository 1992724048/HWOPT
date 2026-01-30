#include "SimplexNoise.h"
using namespace minecraft;

SimplexNoise::SimplexNoise() {
    std::random_device rd;
    std::mt19937_64 mt(rd());
    std::uniform_real_distribution dist_double(0.0, 1.0);

    this->xo = dist_double(mt) * 256.0;
    this->yo = dist_double(mt) * 256.0;
    this->zo = dist_double(mt) * 256.0;
    int i = 0;

    while (i < 256) {
        this->p[i] = i++;
    }

    for (int ix = 0; ix < 256; ix++) {
        std::uniform_int_distribution d(0, 256 - i);
        const int offset = d(mt);
        const int tmp = this->p[ix];
        this->p[ix] = this->p[offset + ix];
        this->p[offset + ix] = tmp;
    }
}

auto SimplexNoise::get_value(const double xin, const double yin) const -> double {
    const double s = (xin + yin) * F2;
    const int i = std::floor(xin + s);
    const int j = std::floor(yin + s);
    const double t = (i + j) * G2;
    const double X0 = i - t;
    const double Y0 = j - t;
    const double x0 = xin - X0;
    const double y0 = yin - Y0;
    int i1;
    int j1;
    if (x0 > y0) {
        i1 = 1;
        j1 = 0;
    } else {
        i1 = 0;
        j1 = 1;
    }

    const double x1 = x0 - i1 + G2;
    const double y1 = y0 - j1 + G2;
    const double x2 = x0 - 1.0 + 2.0 * G2;
    const double y2 = y0 - 1.0 + 2.0 * G2;
    const int ii = i & 0xFF;
    const int jj = j & 0xFF;
    const int gi0 = this->perm(ii + this->perm(jj)) % 12;
    const int gi1 = this->perm(ii + i1 + this->perm(jj + j1)) % 12;
    const int gi2 = this->perm(ii + 1 + this->perm(jj + 1)) % 12;
    const double n0 = get_corner_noise_3d(gi0, x0, y0, 0.0, 0.5);
    const double n1 = get_corner_noise_3d(gi1, x1, y1, 0.0, 0.5);
    const double n2 = get_corner_noise_3d(gi2, x2, y2, 0.0, 0.5);
    return 70.0 * (n0 + n1 + n2);
}

auto SimplexNoise::get_value(const double xin, const double yin, const double zin) const -> double {
    constexpr double F3 = 0.3333333333333333;
    constexpr double G3 = 0.16666666666666666;
    const double s = (xin + yin + zin) * F3;
    const int i = std::floor(xin + s);
    const int j = std::floor(yin + s);
    const int k = std::floor(zin + s);
    const double t = (i + j + k) * G3;
    const double X0 = i - t;
    const double Y0 = j - t;
    const double Z0 = k - t;
    const double x0 = xin - X0;
    const double y0 = yin - Y0;
    const double z0 = zin - Z0;
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

    const double x1 = x0 - i1 + G3;
    const double y1 = y0 - j1 + G3;
    const double z1 = z0 - k1 + G3;
    const double x2 = x0 - i2 + F3;
    const double y2 = y0 - j2 + F3;
    const double z2 = z0 - k2 + F3;
    const double x3 = x0 - 1.0 + 0.5;
    const double y3 = y0 - 1.0 + 0.5;
    const double z3 = z0 - 1.0 + 0.5;
    const int ii = i & 0xFF;
    const int jj = j & 0xFF;
    const int kk = k & 0xFF;
    const int gi0 = this->perm(ii + this->perm(jj + this->perm(kk))) % 12;
    const int gi1 = this->perm(ii + i1 + this->perm(jj + j1 + this->perm(kk + k1))) % 12;
    const int gi2 = this->perm(ii + i2 + this->perm(jj + j2 + this->perm(kk + k2))) % 12;
    const int gi3 = this->perm(ii + 1 + this->perm(jj + 1 + this->perm(kk + 1))) % 12;
    const double n0 = get_corner_noise_3d(gi0, x0, y0, z0, 0.6);
    const double n1 = get_corner_noise_3d(gi1, x1, y1, z1, 0.6);
    const double n2 = get_corner_noise_3d(gi2, x2, y2, z2, 0.6);
    const double n3 = get_corner_noise_3d(gi3, x3, y3, z3, 0.6);
    return 32.0 * (n0 + n1 + n2 + n3);
}

auto SimplexNoise::dot(const int* g, const double x, const double y, const double z) -> double {
    return g[0] * x + g[1] * y + g[2] * z;
}

auto SimplexNoise::get_corner_noise_3d(const int index, const double x, const double y, const double z, const double base) -> double {
    double t0 = base - x * x - y * y - z * z;
    double n0;
    if (t0 < 0.0) {
        n0 = 0.0;
    } else {
        t0 *= t0;
        n0 = t0 * t0 * dot(GRADIENT[index], x, y, z);
    }

    return n0;
}

auto SimplexNoise::perm(const int x) const -> int {
    return this->p[x & 0xFF];
}
