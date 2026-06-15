// 2026-06-15

#include "PerlinNoise.h"
#include <cmath>
#include <cstring>
#include <random>

#include "../../util.h"
using namespace minecraft;

static constexpr double ROUND_OFF = 3.3554432E7;

PerlinNoise::PerlinNoise(std::mt19937_64& mt, const int first_octave, const double* amplitudes, const int size, const bool use_new_init) {
    JavaNative::touch();
    this->first_octave_ = first_octave;
    this->amplitudes_.assign(amplitudes, amplitudes + size);
    this->noise_levels_.resize(size);

    const int zeroOctaveIndex = -first_octave;

    if (use_new_init) {
        for (int i = 0; i < size; i++) {
            if (this->amplitudes_[i] != 0.0) {
                std::mt19937_64 octave_mt(mt());
                this->noise_levels_[i] = ImprovedNoise(octave_mt);
            }
        }
    } else {
        if (zeroOctaveIndex >= 0 && zeroOctaveIndex < size) {
            if (this->amplitudes_[zeroOctaveIndex] != 0.0) {
                this->noise_levels_[zeroOctaveIndex] = ImprovedNoise(mt);
            }
        }
        for (int ix = zeroOctaveIndex - 1; ix >= 0; ix--) {
            if (ix < size && this->amplitudes_[ix] != 0.0) {
                this->noise_levels_[ix] = ImprovedNoise(mt);
            }
        }
    }

    this->lowest_freq_input_factor_ = std::pow(2.0, -static_cast<double>(zeroOctaveIndex));
    this->lowest_freq_value_factor_ = std::pow(2.0, static_cast<double>(size) - 1.0) / (std::pow(2.0, static_cast<double>(size)) - 1.0);
    this->max_value_ = this->edge_value(2.0);
}

auto PerlinNoise::add_methods() -> void {
    "PerlinNoise::_create"_jf.reg<_create>();
    "PerlinNoise::_destroy"_jf.reg<&PerlinNoise::_destroy>();
    "PerlinNoise::get_value3"_jf.reg<&PerlinNoise::get_value3>();
    "PerlinNoise::get_value5"_jf.reg<&PerlinNoise::get_value5>();
    "PerlinNoise::edge_value"_jf.reg<&PerlinNoise::edge_value>();
    "PerlinNoise::_amplitudes"_jf.reg<&PerlinNoise::amplitudes>();
    "PerlinNoise::_amplitudes_size"_jf.reg<&PerlinNoise::amplitudes_size>();
}

auto PerlinNoise::_create(const long seed, const int firstOctave, const double* amplitudes, const int size, const bool useNewInit) -> PerlinNoise* {
    std::mt19937_64 mt(static_cast<long long>(seed));
    return hwopt::util::mi_new<PerlinNoise>(mt, firstOctave, amplitudes, size, useNewInit);
}

auto PerlinNoise::_destroy() const -> void {
    hwopt::util::mi_delete(this);
}

auto PerlinNoise::get_value3(const double x, const double y, const double z) const -> double {
    return this->get_value5(x, y, z, 0.0, 0.0);
}

auto PerlinNoise::get_value5(const double x, const double y, const double z, const double yScale, const double yFudge) const -> double {
    double value = 0.0;
    double factor = this->lowest_freq_input_factor_;
    double value_factor = this->lowest_freq_value_factor_;
    const auto& nl = this->noise_levels_;
    const auto& amp = this->amplitudes_;

    for (size_t i = 0; i < nl.size(); i++) {
        const double a = amp[i];
        if (a != 0.0) {
            const double t = factor;
            value += a * nl[i].noise(wrap(x * t), wrap(y * t), wrap(z * t), yScale * t, yFudge * t) * value_factor;
        }
        factor *= 2.0;
        value_factor *= 0.5;
    }

    return value;
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

auto PerlinNoise::amplitudes_size() const -> int {
    return static_cast<int>(this->amplitudes_.size());
}

auto PerlinNoise::amplitudes(double* out, const int size) const -> int {
    const int count = std::min(size, static_cast<int>(this->amplitudes_.size()));
    std::memcpy(out, this->amplitudes_.data(), static_cast<size_t>(count) * sizeof(double));
    return count;
}

auto PerlinNoise::wrap(const double x) -> double {
    if (std::abs(x) < ROUND_OFF * 0.5) [[likely]] {
        return x;
    }
    return x - (std::floor((x / ROUND_OFF) + 0.5) * ROUND_OFF);
}
