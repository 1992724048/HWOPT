#pragma once
#include "PerlinNoise.hpp"

#include "../../../JavaNative.hpp"

namespace minecraft::noise {
    class HWOPT_API BlendedNoise final : JavaNative<BlendedNoise> {
    public:
        PerlinNoise* min_limit_noise{};
        PerlinNoise* max_limit_noise{};
        PerlinNoise* main_noise{};
        double xz_multiplier{};
        double y_multiplier{};
        double xz_factor{};
        double y_factor{};
        double smear_scale_multiplier{};
        double max_value{};
        double xz_scale{};
        double y_scale{};

        static auto add_methods() -> void;
        static auto _create(PerlinNoise* min_limit_noise,
                            PerlinNoise* max_limit_noise,
                            PerlinNoise* main_noise,
                            double xz_scale,
                            double y_scale,
                            double xz_factor,
                            double y_factor,
                            double smear_scale_multiplier) -> BlendedNoise*;
        auto _destroy() const -> void;

        [[nodiscard]] auto compute(double limit_x, double limit_y, double limit_z) const -> double;
        auto get_values(const double* xs, int xs_len, const double* ys, int ys_len, const double* zs, int zs_len, double* result, int result_len) const -> void;
    };
} // namespace minecraft::noise
