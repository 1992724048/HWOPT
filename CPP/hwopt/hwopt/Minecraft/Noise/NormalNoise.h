#pragma once
#include "PerlinNoise.h"

namespace minecraft {
    class HWOPT_API NormalNoise final : JavaNative<NormalNoise> {
    public:
        PerlinNoise first_{};
        PerlinNoise second_{};
        double value_factor_ = 0.0;

        NormalNoise() = default;
        NormalNoise(int first_octave, const double* amplitudes, int size, bool use_new_init);
        ~NormalNoise();

        static auto add_methods() -> void;
        static auto _create(long long seed, int first_octave, const double* amplitudes, int size, bool use_new_init) -> NormalNoise*;
        auto _destroy() const -> void;

        [[nodiscard]] auto get_value(double x, double y, double z) const -> double;
        [[nodiscard]] auto max_value() const -> double;
        static auto expected_deviation(int octave_span) -> double;
    };
} // namespace minecraft
