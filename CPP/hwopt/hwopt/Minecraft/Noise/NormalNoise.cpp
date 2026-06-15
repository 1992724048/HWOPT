// 2026-06-15

#include "NormalNoise.h"
#include <algorithm>
#include <climits>
#include <cmath>
#include <random>
using namespace minecraft;

static constexpr double INPUT_FACTOR = 1.0181268882175227;

NormalNoise::NormalNoise(const long seed, const int firstOctave, const double* amplitudes, const int size, const bool use_new_init) {
    JavaNative::touch();
    std::mt19937_64 mt(static_cast<long long>(seed));
    this->first_ = new PerlinNoise(mt, firstOctave, amplitudes, size, use_new_init);
    this->second_ = new PerlinNoise(mt, firstOctave, amplitudes, size, use_new_init);

    int min_octave = INT_MAX;
    int max_octave = INT_MIN;
    for (int i = 0; i < size; i++) {
        if (amplitudes[i] != 0.0) {
            min_octave = std::min(min_octave, i);
            max_octave = std::max(max_octave, i);
        }
    }

    const double expectedDev = 0.1 * (1.0 + (1.0 / static_cast<double>(max_octave - min_octave + 1)));
    this->value_factor_ = (1.0 / 6.0) / expectedDev;
}

NormalNoise::~NormalNoise() {
    delete this->first_;
    delete this->second_;
}

auto NormalNoise::add_methods() -> void {
    "NormalNoise::_create"_jf.reg<_create>();
    "NormalNoise::_destroy"_jf.reg<&NormalNoise::_destroy>();
    "NormalNoise::get_value"_jf.reg<&NormalNoise::get_value>();
    "NormalNoise::max_value"_jf.reg<&NormalNoise::max_value>();
    "NormalNoise::expected_deviation"_jf.reg<&NormalNoise::expected_deviation>();
}

auto NormalNoise::_create(const long seed, const int firstOctave, const double* amplitudes, const int size, const bool useNewInit) -> NormalNoise* {
    return new NormalNoise(seed, firstOctave, amplitudes, size, useNewInit);
}

auto NormalNoise::_destroy() const -> void {
    delete this;
}

auto NormalNoise::get_value(const double x, const double y, const double z) const -> double {
    const double x2 = x * INPUT_FACTOR;
    const double y2 = y * INPUT_FACTOR;
    const double z2 = z * INPUT_FACTOR;
    return (this->first_->get_value5(x, y, z, 0.0, 0.0) + this->second_->get_value5(x2, y2, z2, 0.0, 0.0)) * this->value_factor_;
}

auto NormalNoise::max_value() const -> double {
    return (this->first_->max_value_ + this->second_->max_value_) * this->value_factor_;
}

auto NormalNoise::expected_deviation(int octaveSpan) -> double {
    return 0.1 * (1.0 + (1.0 / static_cast<double>(octaveSpan + 1)));
}
