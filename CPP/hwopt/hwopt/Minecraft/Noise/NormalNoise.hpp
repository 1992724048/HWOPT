#pragma once
#include "PerlinNoise.hpp"

namespace minecraft::noise {
    class HWOPT_API NormalNoise final : JavaNative<NormalNoise> {
    public:
        PerlinNoise* first{nullptr};
        PerlinNoise* second{nullptr};
        double value_factor = 0.0;
        double max_value_field = 0.0;

        NormalNoise();
        NormalNoise(double value_factor, double max_value);

        static auto add_methods() -> void;
        static auto _create(double value_factor, double max_value) -> NormalNoise*;
        auto _destroy() const -> void;

        auto set_perlin_noise(PerlinNoise* first, PerlinNoise* second) -> void;

        [[nodiscard]] auto get_value(double x, double y, double z) const -> double;
        auto get_values(const double* xs, int xs_len, const double* ys, int ys_len, const double* zs, int zs_len, double* result, int result_len) const -> void;
        [[nodiscard]] auto max_value() const -> double;
        static auto expected_deviation(int octave_span) -> double;
    };
} // namespace minecraft
