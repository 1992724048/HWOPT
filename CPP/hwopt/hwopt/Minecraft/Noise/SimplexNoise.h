#pragma once
#include <array>
#include "../../JavaNative.h"

namespace minecraft {
    class SimplexNoise final : JavaNative<SimplexNoise> {
    public:
        double xo{};
        double yo{};
        double zo{};
        std::array<int, 256> p{};

        SimplexNoise();

        static auto add_methods() -> void;
        static auto _create(double xo, double yo, double zo, std::span<int> array) -> SimplexNoise*;
        auto _destroy() const -> void;

        [[nodiscard]] auto get_value2(double xin, double yin) const -> double;
        [[nodiscard]] auto get_value3(double xin, double yin, double zin) const -> double;
    private:
        [[nodiscard]] auto perm(int x) const -> int;

        static auto dot(const int* g, double x, double y, double z) -> double;
        static auto get_corner_noise3d(int index, double x, double y, double z, double base) -> double;

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

        static constexpr double F2 = 0.3660254037844386;
        static constexpr double G2 = 0.21132486540518713;
    };
} // namespace minecraft
