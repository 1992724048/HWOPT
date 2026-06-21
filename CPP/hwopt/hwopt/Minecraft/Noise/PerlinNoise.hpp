#pragma once
#include <random>
#include <vector>

#include "ImprovedNoise.hpp"

#include "mimalloc/mimalloc.h"

namespace minecraft::noise {
    class HWOPT_API PerlinNoise final : JavaNative<PerlinNoise> {
    public:
        int first_octave_ = 0;
        double max_value_ = 0.0;
        double lowest_freq_value_factor_ = 0.0;
        double lowest_freq_input_factor_ = 0.0;

        std::vector<double, mi_stl_allocator<double>> amplitudes_;
        std::vector<ImprovedNoise*, mi_stl_allocator<ImprovedNoise*>> noise_levels_;

        PerlinNoise() = default;
        PerlinNoise(int first_octave, std::span<double> amplitudes, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value);

        static auto add_methods() -> void;
        static auto _create(int first_octave, double* amplitudes, int amplitudes_size, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value) -> PerlinNoise*;
        auto _destroy() const -> void;

        auto add_noise(int index, ImprovedNoise* noise) -> void;

        [[nodiscard]] auto get_value(double x, double y, double z) const -> double;
        [[nodiscard]] auto get_value(double x, double y, double z, double y_scale, double y_fudge) const -> double;
        auto get_values(const double* xs, int xs_len, const double* ys, int ys_len, const double* zs, int zs_len, double* result, int result_len) const -> void;
        [[nodiscard]] auto edge_value(double noise_value) const -> double;
        [[nodiscard]] auto amplitudes() -> std::span<double>;
        [[nodiscard]] auto max_broken_value(double y_scale) const -> double;
        [[nodiscard]] auto get_octave_noise(int i) const -> ImprovedNoise*;

        static auto wrap(double x) -> double;
    };
} // namespace minecraft::noise
