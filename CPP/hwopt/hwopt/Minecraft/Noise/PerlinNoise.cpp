// 2026-06-17 03:23:58

#include "PerlinNoise.hpp"
#include <cmath>

#include "../../util.hpp"
using namespace minecraft::noise;

static constexpr double ROUND_OFF = 3.3554432E7;

PerlinNoise::PerlinNoise(const int first_octave, std::span<double> amplitudes, const double lowest_freq_value_factor, const double lowest_freq_input_factor, const double max_value) {
    JavaNative::touch();
    this->first_octave_ = first_octave;
    this->amplitudes_.assign(amplitudes.data(), amplitudes.data() + amplitudes.size());
    this->noise_levels_.resize(amplitudes.size());
    this->lowest_freq_input_factor_ = lowest_freq_input_factor;
    this->lowest_freq_value_factor_ = lowest_freq_value_factor;
    this->max_value_ = max_value;
}

auto PerlinNoise::add_methods() -> void {
    "PerlinNoise::_create"_jf.reg<_create>();
    "PerlinNoise::_destroy"_jf.reg<&PerlinNoise::_destroy>();
    "PerlinNoise::get_value3"_jf.reg<static_cast<double(PerlinNoise::*)(double, double, double) const>(&PerlinNoise::get_value)>();
    "PerlinNoise::get_value5"_jf.reg<static_cast<double(PerlinNoise::*)(double, double, double, double, double) const>(&PerlinNoise::get_value)>();
    "PerlinNoise::get_values"_jf.reg<&PerlinNoise::get_values>();
    "PerlinNoise::edge_value"_jf.reg<&PerlinNoise::edge_value>();
    "PerlinNoise::_amplitudes"_jf.reg<&PerlinNoise::amplitudes>();
    "PerlinNoise::max_broken_value"_jf.reg<&PerlinNoise::max_broken_value>();
    "PerlinNoise::add_noise"_jf.reg<&PerlinNoise::add_noise>();
}

auto PerlinNoise::_create(const int first_octave, double* amplitudes, const int amplitudes_size, double lowest_freq_value_factor, double lowest_freq_input_factor, double max_value) -> PerlinNoise* {
    std::span sp(amplitudes, amplitudes_size);
    return hwopt::util::mi_new<PerlinNoise>(first_octave, sp, lowest_freq_value_factor, lowest_freq_input_factor, max_value);
}

auto PerlinNoise::_destroy() const -> void {
    hwopt::util::mi_delete(this);
}

auto PerlinNoise::add_noise(const int index, ImprovedNoise* noise) -> void {
    noise_levels_[index] = noise;
}

auto PerlinNoise::get_value(const double x, const double y, const double z) const -> double {
    return this->get_value(x, y, z, 0.0, 0.0);
}

auto PerlinNoise::get_value(const double x, const double y, const double z, const double y_scale, const double y_fudge) const -> double {
    double value = 0.0;
    double factor = this->lowest_freq_input_factor_;
    double value_factor = this->lowest_freq_value_factor_;
    const auto& nl = this->noise_levels_;
    const auto& amp = this->amplitudes_;

    for (size_t i = 0; i < nl.size(); i++) {
        if (amp[i] != 0.0) {
            const double t = factor;
            value += amp[i] * nl[i]->noise(wrap(x * t), wrap(y * t), wrap(z * t), y_scale * t, y_fudge * t) * value_factor;
        }
        factor *= 2.0;
        value_factor *= 0.5;
    }
    return value;
}

auto PerlinNoise::get_values(const double* xs, int xs_len, const double* ys, int ys_len, const double* zs, int zs_len, double* result, int result_len) const -> void {
    for (int i = 0; i < result_len; i++) {
        result[i] = get_value(xs[i], ys[i], zs[i]);
    }
}

auto PerlinNoise::edge_value(const double noise_value) const -> double {
    double value = 0.0;
    double value_factor = this->lowest_freq_value_factor_;
    for (const double i : this->amplitudes_) {
        if (i != 0.0) {
            value += i * noise_value * value_factor;
        }
        value_factor *= 0.5;
    }
    return value;
}

auto PerlinNoise::amplitudes() -> std::span<double> {
    return this->amplitudes_;
}

auto PerlinNoise::max_broken_value(const double y_scale) const -> double {
    return this->edge_value(y_scale + 2.0F);
}

auto PerlinNoise::get_octave_noise(const int i) const -> ImprovedNoise* {
    return this->noise_levels_[this->noise_levels_.size() - 1 - i];
}

auto PerlinNoise::wrap(const double x) -> double {
    return x - std::floor(x / static_cast<double>(3.3554432E7F) + static_cast<double>(0.5F)) * static_cast<double>(3.3554432E7F);
}
