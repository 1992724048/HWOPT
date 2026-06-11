#pragma once
#include <cstdint>
#include <vector>
#include "PerlinNoise.h"

namespace minecraft {
    class __declspec(dllexport) NormalNoise final : JavaNative<NormalNoise> {
    public:
        NormalNoise(const uint64_t& random, const std::pair<int, std::vector<double>>& pair, bool use_new_initialization);

        [[nodiscard]] auto max_value() const noexcept -> double {
            return max_value_;
        }

        [[nodiscard]] auto get_value(double x, double y, double z) const -> double;

        static auto add_methods() -> void;
        static auto _create(uint64_t seed, int first_octave, double* amplitudes, int size, bool use_new_initialization) -> NormalNoise*;
        auto _destroy() const -> void;

    private:
        static constexpr double INPUT_FACTOR = 1.0181268882175227;

        PerlinNoise first_;
        PerlinNoise second_;
        double value_factor_;
        double max_value_;
        int first_octave;
        std::vector<double> amplitudes;

        static auto expected_deviation(int octave_span) noexcept -> double;
    };
} // namespace minecraft