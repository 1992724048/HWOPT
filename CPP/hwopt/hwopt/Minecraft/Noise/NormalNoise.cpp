// 2026-06-17 03:23:49

#include "NormalNoise.hpp"

#include "../../util.hpp"
using namespace minecraft::noise;

static constexpr double INPUT_FACTOR = 1.0181268882175227;

NormalNoise::NormalNoise() {
    JavaNative::touch();
}

NormalNoise::NormalNoise(const double value_factor, const double max_value) {
    this->value_factor = value_factor;
    this->max_value_field = max_value;
}

auto NormalNoise::add_methods() -> void {
    "NormalNoise::_create"_jf.reg<_create>();
    "NormalNoise::_destroy"_jf.reg<&NormalNoise::_destroy>();
    "NormalNoise::get_value"_jf.reg<&NormalNoise::get_value>();
    "NormalNoise::max_value"_jf.reg<&NormalNoise::max_value>();
    "NormalNoise::expected_deviation"_jf.reg<&NormalNoise::expected_deviation>();
    "NormalNoise::set_perlin_noise"_jf.reg<&NormalNoise::set_perlin_noise>();
}

auto NormalNoise::_create(double value_factor, double max_value) -> NormalNoise* {
    return hwopt::util::mi_new<NormalNoise>(value_factor, max_value);
}

auto NormalNoise::_destroy() const -> void {
    hwopt::util::mi_delete(this);
}

auto NormalNoise::set_perlin_noise(PerlinNoise* first, PerlinNoise* second) -> void {
    this->first = first;
    this->second = second;
}

auto NormalNoise::get_value(const double x, const double y, const double z) const -> double {
    const double x2 = x * INPUT_FACTOR;
    const double y2 = y * INPUT_FACTOR;
    const double z2 = z * INPUT_FACTOR;
    return (this->first->get_value3(x, y, z) + this->second->get_value3(x2, y2, z2)) * this->value_factor;
}

auto NormalNoise::max_value() const -> double {
    return max_value_field;
}

auto NormalNoise::expected_deviation(const int octave_span) -> double {
    return 0.1 * (1.0 + (1.0 / static_cast<double>(octave_span + 1)));
}
