#pragma once
#include "PerlinNoise.h"

namespace minecraft {
    class HWOPT_API NormalNoise final : JavaNative<NormalNoise> {
    public:
        PerlinNoise first;
        PerlinNoise second;
        double value_factor = 0.0;
        double max_value_field = 0.0;

        NormalNoise() = default;
        NormalNoise(double value_factor, double max_value);

        static auto add_methods() -> void;
        static auto _create(double value_factor, double max_value) -> NormalNoise*;
        auto _destroy() const -> void;

        auto set_first(int first_octave, double* amplitudes, int amplitudes_size, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value) -> void;
        auto set_second(int first_octave, double* amplitudes, int amplitudes_size, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value) -> void;
        auto add_noise_to_first(int index, double xo, double yo, double zo, int8_t* array, int array_size) -> void;
        auto add_noise_to_second(int index, double xo, double yo, double zo, int8_t* array, int array_size) -> void;

        [[nodiscard]] auto get_value(double x, double y, double z) const -> double;
        [[nodiscard]] auto max_value() const -> double;
        static auto expected_deviation(int octave_span) -> double;
    };
} // namespace minecraft
