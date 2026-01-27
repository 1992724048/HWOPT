#pragma once
#include <random>
#include <array>

#include "SimplexNoise.h"

class ImprovedNoise {
public:
    double xo;
    double yo;
    double zo;

    ImprovedNoise();

    [[nodiscard]] auto noise(const double _x, const double _y, const double _z) const -> double ;

    [[nodiscard]] auto noise(const double _x, const double _y, const double _z, const double yScale, const double yFudge) const -> double ;

    auto noise_with_derivative(const double _x, const double _y, const double _z, double* derivativeOut) const -> double ;

private:
    std::array<char, 256> p;

    static auto grad_dot(const int hash, const double x, const double y, const double z) -> double ;

    [[nodiscard]] auto perm(const int x) const -> int ;

    [[nodiscard]] auto sample_and_lerperm(const int x, const int y, const int z, const double xr, const double yr, const double zr, const double yrOriginal) const -> double ;

    auto sample_with_derivative(const int x, const int y, const int z, const double xr, const double yr, const double zr, double* derivativeOut) const -> double ;

    static auto smoothstep(const double x) -> double ;

    static auto smoothstep_derivative(const double x) -> double ;

    static auto lerp(const double alpha1, const double p0, const double p1) -> double ;

    static auto lerp2(const double alpha1, const double alpha2, const double x00, const double x10, const double x01, const double x11) -> double ;

    static auto lerp3(const double alpha1, const double alpha2, const double alpha3, const double x000, const double x100, const double x010, const double x110, const double x001, const double x101, const double x011, const double x111) -> double ;
};
