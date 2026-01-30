#pragma once
#include <random>
#include <array>

#include "SimplexNoise.h"
#include "../../JavaNative.h"

namespace minecraft {
    class ImprovedNoise : JavaNative<ImprovedNoise> {
    public:
        double xo;
        double yo;
        double zo;

        ImprovedNoise(std::mt19937_64& mt);
        ImprovedNoise();

        [[nodiscard]] auto noise(double _x, double _y, double _z) const -> double;

        [[nodiscard]] auto noise(double _x, double _y, double _z, double yScale, double yFudge) const -> double;

        auto noise_with_derivative(double _x, double _y, double _z, double* derivativeOut) const -> double;

        static auto add_methods() -> void;

    private:
        std::array<uint8_t, 256> p;

        static auto grad_dot(int hash, double x, double y, double z) -> double;

        [[nodiscard]] auto perm(int x) const -> int;

        [[nodiscard]] auto sample_and_lerperm(int x, int y, int z, double xr, double yr, double zr, double yrOriginal) const -> double;

        auto sample_with_derivative(int x, int y, int z, double xr, double yr, double zr, double* derivativeOut) const -> double;

        static auto smoothstep(double x) -> double;

        static auto smoothstep_derivative(double x) -> double;

        static auto lerp(double alpha1, double p0, double p1) -> double;

        static auto lerp2(double alpha1, double alpha2, double x00, double x10, double x01, double x11) -> double;

        static auto lerp3(double alpha1, double alpha2, double alpha3, double x000, double x100, double x010, double x110, double x001, double x101, double x011, double x111) -> double;

        static auto _create(double _x, double _y, double _z, const uint8_t* bytes) -> ImprovedNoise*;

        auto _destroy() const -> void;
    };
}

namespace fortran {
    using namespace minecraft;
#define DLL_API __declspec(dllimport)
    extern "C" {
        DLL_API auto ImprovedNoise_noise_3(const ImprovedNoise* state, double x, double y, double z) -> double;
        DLL_API auto ImprovedNoise_noise_5(const ImprovedNoise* state, double x, double y, double z, double yScale, double yFudge) -> double;
        DLL_API auto ImprovedNoise_noise_with_derivative(const ImprovedNoise* state, double x, double y, double z, double* derivativeOut) -> double;
    }
}