#pragma once
#include <random>
#include <vector>
#include "ImprovedNoise.h"

namespace minecraft {
    class HWOPT_API PerlinNoise final : JavaNative<PerlinNoise> {
    public:
        int first_octave_ = 0;
        double max_value_ = 0.0;
        double lowest_freq_value_factor_ = 0.0;
        double lowest_freq_input_factor_ = 0.0;

        std::vector<double> amplitudes_;
        std::vector<ImprovedNoise> noise_levels_;

        PerlinNoise() = default;
        PerlinNoise(int first_octave, std::span<double> amplitudes, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value);

        static auto add_methods() -> void;
        static auto _create(int first_octave, double* amplitudes, int amplitudes_size, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value) -> PerlinNoise*;
        auto _destroy() const -> void;

        auto add_noise(int index, double xo, double yo, double zo, int8_t* array, int array_size) -> void;

        [[nodiscard]] auto get_value3(double x, double y, double z) const -> double;
        [[nodiscard]] auto get_value5(double x, double y, double z, double y_scale, double y_fudge) const -> double;
        [[nodiscard]] auto edge_value(double noise_value) const -> double;
        [[nodiscard]] auto amplitudes() -> std::span<double>;

        static auto wrap(double x) -> double;
    };
} // namespace minecraft
