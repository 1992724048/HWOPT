// 遂沫 ImprovedNoise.h
// 2026-02-13 14:04:10

#pragma once
#include <random>
#include <sycl/sycl.hpp>

namespace minecraft_sycl {
    class ImprovedNoise final {
    public:
        double xo;
        double yo;
        double zo;

        explicit ImprovedNoise(std::mt19937_64& mt);
        ImprovedNoise() = default;

        SYCL_EXTERNAL [[nodiscard]] auto noise(double x, double y, double z) const -> double;
        SYCL_EXTERNAL [[nodiscard]] auto noise(double _x, double _y, double _z, double yScale, double yFudge) const -> double;

    private:
        std::array<uint8_t, 256> p;

        SYCL_EXTERNAL inline static auto grad_dot(int hash, double x, double y, double z) -> double;

        SYCL_EXTERNAL [[nodiscard]] auto perm(int x) const -> int;

        SYCL_EXTERNAL [[nodiscard]] auto sample_and_lerperm(int x, int y, int z, double xr, double yr, double zr, double yr_original) const -> double;

        SYCL_EXTERNAL inline static auto smoothstep(double x) -> double;

        SYCL_EXTERNAL inline static auto lerp(double alpha1, double p0, double p1) -> double;
        SYCL_EXTERNAL inline static auto lerp2(double alpha1, double alpha2, double x00, double x10, double x01, double x11) -> double;
        SYCL_EXTERNAL inline static auto lerp3(double alpha1,
                                               double alpha2,
                                               double alpha3,
                                               double x000,
                                               double x100,
                                               double x010,
                                               double x110,
                                               double x001,
                                               double x101,
                                               double x011,
                                               double x111) -> double;

        static constexpr std::array<int, 48> GRADIENT{
            1,
            1,
            0,
            -1,
            1,
            0,
            1,
            -1,
            0,
            -1,
            -1,
            0,
            1,
            0,
            1,
            -1,
            0,
            1,
            1,
            0,
            -1,
            -1,
            0,
            -1,
            0,
            1,
            1,
            0,
            -1,
            1,
            0,
            1,
            -1,
            0,
            -1,
            -1,
            1,
            1,
            0,
            0,
            -1,
            1,
            -1,
            1,
            0,
            0,
            -1,
            -1
        };
    };
}
