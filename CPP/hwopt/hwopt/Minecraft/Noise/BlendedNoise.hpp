#pragma once
#include "PerlinNoise.hpp"

#include "../../JavaNative.hpp"

namespace minecraft::noise {
    class HWOPT_API BlendedNoise final : JavaNative<BlendedNoise> {
    public:
        PerlinNoise* minLimitNoise{};
        PerlinNoise* maxLimitNoise{};
        PerlinNoise* mainNoise{};
        double xzMultiplier{};
        double yMultiplier{};
        double xzFactor{};
        double yFactor{};
        double smearScaleMultiplier{};
        double maxValue{};
        double xzScale{};
        double yScale{};

        static auto add_methods() -> void;
        static auto _create(PerlinNoise* minLimitNoise,
                            PerlinNoise* maxLimitNoise,
                            PerlinNoise* mainNoise,
                            double xzScale,
                            double yScale,
                            double xzFactor,
                            double yFactor,
                            double smearScaleMultiplier) -> BlendedNoise*;
        auto _destroy() const -> void;

        [[nodiscard]] auto compute(double limitX, double limitY, double limitZ) const -> double;
        auto get_values(const double* xs, int xs_len, const double* ys, int ys_len, const double* zs, int zs_len, double* result, int result_len) const -> void;
    };
} // namespace minecraft::noise
