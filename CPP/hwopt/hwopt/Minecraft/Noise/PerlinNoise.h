// 遂沫 PerlinNoise.h
// 2026-02-16 00:09:28

#pragma once
#include <cstdint>
#include <optional>
#include <utility>
#include <vector>

#include "ImprovedNoise.h"
#include "../../JavaNative.h"

namespace minecraft {
    class __declspec(dllexport) PerlinNoise final : JavaNative<PerlinNoise> {
    public:
        PerlinNoise(uint64_t seed, const std::pair<int, std::vector<double>>& pair, bool use_new_initialization);

        [[nodiscard]] auto get_max_value() const -> double;
        [[nodiscard]] auto get_value(double x, double y, double z) const -> double;
        [[nodiscard]] auto get_value(double x, double y, double z, double y_scale, double y_fudge, bool y_flat_hack) const -> double;
        [[nodiscard]] auto max_broken_value(double y_scale) const -> double;

        inline static auto wrap(double x) -> double;
        [[nodiscard]] auto get_first_octave() const -> int;
        [[nodiscard]] auto get_amplitudes() const -> std::vector<double>;
        [[nodiscard]] auto edge_value(double noise_value) const -> double;

        static auto add_methods() -> void;

    private:
        int first_octave;
        double max_value;
        double lowest_freq_value_factor;
        double lowest_freq_input_factor;
        std::vector<double> amplitudes;
        std::vector<std::optional<ImprovedNoise>> noise_levels;

        static auto _create(uint64_t seed, int first_octave, double* amplitudes, int size, bool use_new_initialization) -> PerlinNoise*;
        auto _destroy() const -> void;
        auto _amplitudes(double* amplitudes, int size) const -> int;
        [[nodiscard]] auto _amplitudes_size() const -> int;
    };
}
