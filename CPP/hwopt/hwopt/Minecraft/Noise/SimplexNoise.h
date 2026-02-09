// 遂沫 SimplexNoise.h
// 2026-02-08 23:22:13

#pragma once
#include <array>
#include <numbers>
#include <random>

namespace minecraft {
    class SimplexNoise {
    public :
        double xo;
        double yo;
        double zo;

        explicit SimplexNoise(std::mt19937_64 mt);

        [[nodiscard]] auto get_value(double xin, double yin) const -> double;

        [[nodiscard]] auto get_value(double xin, double yin, double zin) const -> double;

        static constexpr int GRADIENT[48] = {
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

        static auto dot(const int* g, double x, double y, double z) -> double;

    private:
        static constexpr double SQRT_3 = std::numbers::sqrt3;
        static constexpr double F2 = 0.5 * (SQRT_3 - 1.0);
        static constexpr double G2 = (3.0 - SQRT_3) / 6.0;
        std::array<int, 512> p;

        static auto get_corner_noise_3d(int index, double x, double y, double z, double base) -> double;

        [[nodiscard]] auto perm(int x) const -> int;
    };
}
