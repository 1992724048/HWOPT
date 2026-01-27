#include "PerlinNoise.h"

PerlinNoise::PerlinNoise(const std::pair<int, std::vector<double>>& pair, const bool use_new_initialization) {
    this->first_octave = pair.first;
    this->amplitudes = pair.second;
    const int octaves = this->amplitudes.size();
    const int zero_octave_index = -this->first_octave;
    this->noise_levels.resize(octaves);
    if (use_new_initialization) {
        for (int i = 0; i < octaves; i++) {
            if (this->amplitudes[i] != 0.0) {
                this->noise_levels[i] = std::make_shared<ImprovedNoise>();
            }
        }
    }
    else {
        const auto zero_octave = std::make_shared<ImprovedNoise>();
        if (zero_octave_index >= 0 && zero_octave_index < octaves) {
            if (this->amplitudes[zero_octave_index]) {
                this->noise_levels[zero_octave_index] = zero_octave;
            }
        }

        for (int ix = zero_octave_index - 1; ix >= 0; ix--) {
            if (ix < octaves) {
                if (this->amplitudes[ix]) {
                    this->noise_levels[ix] = std::make_shared<ImprovedNoise>();
                }
            }
        }

        if (zero_octave_index < octaves - 1) {
            throw std::runtime_error("Positive octaves are temporarily disabled");
        }
    }

    this->lowest_freq_input_factor = std::pow(2.0, -zero_octave_index);
    this->lowest_freq_value_factor = std::pow(2.0, octaves - 1) / (std::pow(2.0, octaves) - 1.0);
    this->max_value = this->edge_value(2.0);
}

auto PerlinNoise::get_max_value() const -> double {
    return this->max_value;
}

auto PerlinNoise::get_value(const double x, const double y, const double z) const -> double {
    return this->get_value(x, y, z, 0.0, 0.0, false);
}

auto PerlinNoise::get_value(const double x, const double y, const double z, const double y_scale, const double y_fudge, const bool y_flat_hack) const -> double {
    double value = 0.0;
    double factor = this->lowest_freq_input_factor;
    double value_factor = this->lowest_freq_value_factor;

    for (int i = 0; i < this->noise_levels.size(); i++) {
        if (const auto noise = this->noise_levels[i]) {
            const double noise_val = noise->noise(wrap(x * factor), y_flat_hack ? -noise->yo : wrap(y * factor), wrap(z * factor), y_scale * factor, y_fudge * factor);
            value += this->amplitudes[i] * noise_val * value_factor;
        }

        factor *= 2.0;
        value_factor /= 2.0;
    }

    return value;
}

auto PerlinNoise::max_broken_value(const double y_scale) const -> double {
    return this->edge_value(y_scale + 2.0);
}

auto PerlinNoise::get_octave_noise(const int i) -> std::shared_ptr<ImprovedNoise> {
    return this->noise_levels[this->noise_levels.size() - 1 - i];
}

auto PerlinNoise::wrap(const double x) -> double {
    return x - std::floor(x / 3.3554432E7 + 0.5) * 3.3554432E7;
}

auto PerlinNoise::get_first_octave() const -> int {
    return this->first_octave;
}

auto PerlinNoise::get_amplitudes() -> std::vector<double> {
    return this->amplitudes;
}

auto PerlinNoise::edge_value(const double noise_value) const -> double {
    double value = 0.0;
    double value_factor = this->lowest_freq_value_factor;

    for (int i = 0; i < this->noise_levels.size(); i++) {
        if (this->noise_levels[i]) {
            value += this->amplitudes[i] * noise_value * value_factor;
        }

        value_factor /= 2.0;
    }

    return value;
}
