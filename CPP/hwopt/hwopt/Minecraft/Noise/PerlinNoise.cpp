#include "PerlinNoise.h"
using namespace minecraft;

#include <stdpp/exception.h>

PerlinNoise::PerlinNoise(const uint64_t seed, const std::pair<int, std::vector<double>>& pair, const bool use_new_initialization) {
    JavaNative::touch();
    std::mt19937_64 mt(seed);
    this->first_octave = pair.first;
    this->amplitudes = pair.second;
    const int octaves = this->amplitudes.size();
    const int zero_octave_index = -this->first_octave;
    this->noise_levels.resize(octaves);
    if (use_new_initialization) {
        for (int i = 0; i < octaves; i++) {
            if (this->amplitudes[i] != 0.0) {
                const int octave = this->first_octave + i;
                std::mt19937_64 mt_(octave);
                this->noise_levels[i] = ImprovedNoise(mt);
            }
        }
    } else {
        if (zero_octave_index >= 0 && zero_octave_index < octaves) {
            if (this->amplitudes[zero_octave_index]) {
                this->noise_levels[zero_octave_index] = ImprovedNoise(mt);
            }
        }

        for (int ix = zero_octave_index - 1; ix >= 0; ix--) {
            if (ix < octaves) {
                if (this->amplitudes[ix]) {
                    this->noise_levels[ix] = ImprovedNoise(mt);
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

    const size_t n = this->noise_levels.size();
    const auto* __restrict noises = this->noise_levels.data();
    const auto* __restrict amps = this->amplitudes.data();

    for (size_t i = 0; i < n; ++i) {
        const ImprovedNoise& noise = noises[i];

        const double xf = wrap(x * factor);
        const double yf = y_flat_hack ? -noise.yo : wrap(y * factor);
        const double zf = wrap(z * factor);

        const double noise_val = noise.noise(xf, yf, zf, y_scale * factor, y_fudge * factor);

        value += amps[i] * noise_val * value_factor;

        factor *= 2.0;
        value_factor *= 0.5;
    }

    return value;
}

auto PerlinNoise::max_broken_value(const double y_scale) const -> double {
    return this->edge_value(y_scale + 2.0);
}

auto PerlinNoise::get_octave_noise(const int i) -> ImprovedNoise& {
    return this->noise_levels[this->noise_levels.size() - 1 - i];
}

auto PerlinNoise::wrap(const double x) -> double {
    constexpr double c = 3.3554432E7;
    constexpr double inv_c = 1.0 / c;
    const double k = std::floor(x * inv_c + 0.5);
    return std::fma(-k, c, x);
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
        value += this->amplitudes[i] * noise_value * value_factor;
        value_factor /= 2.0;
    }

    return value;
}

auto PerlinNoise::add_methods() -> void {
    register_method<_create>("PerlinNoise::_create");
    register_method<&PerlinNoise::_destroy>("PerlinNoise::_destroy");
    register_method<static_cast<double(PerlinNoise::*)(double, double, double) const>(&PerlinNoise::get_value)>("PerlinNoise::get_value3");
    register_method<static_cast<double(PerlinNoise::*)(double, double, double, double, double, bool) const>(&PerlinNoise::get_value)>("PerlinNoise::get_value6");
    register_method<&PerlinNoise::edge_value>("PerlinNoise::edge_value");
    register_method<&PerlinNoise::_amplitudes>("PerlinNoise::_amplitudes");
    register_method<&PerlinNoise::_amplitudes_size>("PerlinNoise::_amplitudes_size");
}

auto PerlinNoise::_create(const uint64_t seed, const int first_octave, double* amplitudes, const int size, const bool use_new_initialization) -> PerlinNoise* {
    thread_local auto _ = _set_se_translator(stdpp::exception::NativeException::seh_to_ce);
    return new PerlinNoise(seed, {first_octave, JavaUtil::to_vector<double>(amplitudes, size)}, use_new_initialization);
}

auto PerlinNoise::_destroy() const -> void {
    delete this;
}

auto PerlinNoise::_amplitudes(double* amplitudes, const int size) const -> int {
    return JavaUtil::vector_copy(this->amplitudes, amplitudes, size);
}

auto PerlinNoise::_amplitudes_size() const -> int {
    return this->amplitudes.size();
}
