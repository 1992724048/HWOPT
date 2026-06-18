#pragma once
#include <array>
#include "../../JavaNative.hpp"

namespace minecraft::noise {
    class HWOPT_API SimplexNoise final : JavaNative<SimplexNoise> {
    public:
        double xo{};
        double yo{};
        double zo{};
        std::array<int, 256> p{};

        SimplexNoise();

        static auto add_methods() -> void;
        static auto _create(double xo, double yo, double zo, const int* array, int array_size) -> SimplexNoise*;
        auto _destroy() const -> void;

        static auto dot(const double* g, double x, double y, double z) -> double;
        [[nodiscard]] auto get_value(double xin, double yin) const -> double;
        [[nodiscard]] auto get_value(double xin, double yin, double zin) const -> double;
    private:
        [[nodiscard]] auto perm(int x) const -> int;
        static auto get_corner_noise3d(int index, double x, double y, double z, double base) -> double;

        static constexpr std::array<double, 48> GRADIENT{
            1.0,
            1.0,
            0.0,
            -1.0,
            1.0,
            0.0,
            1.0,
            -1.0,
            0.0,
            -1.0,
            -1.0,
            0.0,
            1.0,
            0.0,
            1.0,
            -1.0,
            0.0,
            1.0,
            1.0,
            0.0,
            -1.0,
            -1.0,
            0.0,
            -1.0,
            0.0,
            1.0,
            1.0,
            0.0,
            -1.0,
            1.0,
            0.0,
            1.0,
            -1.0,
            0.0,
            -1.0,
            -1.0,
            1.0,
            1.0,
            0.0,
            -1.0,
            1.0,
            0.0,
            0.0,
            -1.0,
            1.0,
            0.0,
            -1.0,
            -1.0
        };

        static constexpr double F2 = 0.3660254037844386;
        static constexpr double G2 = 0.21132486540518713;
    };
} // namespace minecraft
