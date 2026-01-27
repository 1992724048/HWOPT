#pragma once
#include <utility>
#include <vector>
#include <cmath>
#include <random>
#include <memory>
#include "ImprovedNoise.h"

class PerlinNoise {
public:
    PerlinNoise(const std::pair<int, std::vector<double>>& pair, const bool use_new_initialization);

    [[nodiscard]] auto get_max_value() const -> double ;

    [[nodiscard]] auto get_value(const double x, const double y, const double z) const -> double ;

    [[nodiscard]] auto get_value(const double x, const double y, const double z, const double y_scale, const double y_fudge, const bool y_flat_hack) const -> double ;

    [[nodiscard]] auto max_broken_value(const double y_scale) const -> double ;


    auto get_octave_noise(const int i) -> std::shared_ptr<ImprovedNoise> ;

    static auto wrap(const double x) -> double ;

    [[nodiscard]] auto get_first_octave() const -> int ;

    auto get_amplitudes() -> std::vector<double> ;

    [[nodiscard]] auto edge_value(const double noise_value) const -> double ;

private:
    std::vector<std::shared_ptr<ImprovedNoise>> noise_levels;
    int first_octave;
    std::vector<double> amplitudes;
    double lowest_freq_value_factor;
    double lowest_freq_input_factor;
    double max_value;
};
