#pragma once
#include <random>
#include <vector>
#include "ImprovedNoise.h"

namespace minecraft {
    class PerlinNoise final : JavaNative<PerlinNoise> {
    public:
        int first_octave_ = 0;
        double max_value_ = 0.0;
        double lowest_freq_value_factor_ = 0.0;
        double lowest_freq_input_factor_ = 0.0;

        std::vector<double> amplitudes_;
        std::vector<ImprovedNoise> noise_levels_;

        PerlinNoise() = default;
        PerlinNoise(std::mt19937_64& mt, int firstOctave, const double* amplitudes, int size, bool useNewInit);

        static auto add_methods() -> void;
        static auto _create(long seed, int firstOctave, const double* amplitudes, int size, bool useNewInit) -> PerlinNoise*;
        auto _destroy() const -> void;

        [[nodiscard]] auto get_value3(double x, double y, double z) const -> double;
        [[nodiscard]] auto get_value5(double x, double y, double z, double yScale, double yFudge) const -> double;
        [[nodiscard]] auto edge_value(double noiseValue) const -> double;
        [[nodiscard]] auto amplitudes_size() const -> int;
        auto amplitudes(double* out, int size) const -> int;

        static auto wrap(double x) -> double;
    };
} // namespace minecraft
