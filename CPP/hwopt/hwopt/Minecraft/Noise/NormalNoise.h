#pragma once
#include "PerlinNoise.h"

namespace minecraft {
    class HWOPT_API NormalNoise final : JavaNative<NormalNoise> {
    public:
        PerlinNoise* first_ = nullptr;
        PerlinNoise* second_ = nullptr;
        double value_factor_ = 0.0;

        NormalNoise() = default;
        NormalNoise(long seed, int firstOctave, const double* amplitudes, int size, bool use_new_init);
        ~NormalNoise();

        static auto add_methods() -> void;
        static auto _create(long seed, int firstOctave, const double* amplitudes, int size, bool useNewInit) -> NormalNoise*;
        auto _destroy() const -> void;

        [[nodiscard]] auto get_value(double x, double y, double z) const -> double;
        [[nodiscard]] auto max_value() const -> double;
        static auto expected_deviation(int octaveSpan) -> double;
    };
} // namespace minecraft
