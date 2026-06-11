#include "PerlinNoise.h"

#include "../../util.h"
using namespace minecraft;

#include <utility>

PerlinNoise::PerlinNoise(const uint64_t seed, const std::pair<int, std::vector<double>>& pair, const bool use_new_initialization) {
    JavaNative::touch();
    std::mt19937_64 rng(seed);
    init(rng, pair, use_new_initialization);
}

PerlinNoise::PerlinNoise(std::mt19937_64& rng, const std::pair<int, std::vector<double>>& pair, const bool use_new_initialization) {
    JavaNative::touch();
    init(rng, pair, use_new_initialization);
}

auto PerlinNoise::get_max_value() const -> double {
    return this->max_value;
}

auto PerlinNoise::get_value(const double x, const double y, const double z) const -> double {
    return this->get_value(x, y, z, 0.0, 0.0);
}

auto PerlinNoise::get_value(const double x, const double y, const double z, const double y_scale, const double y_fudge) const -> double {
    double value = 0.0F;
    double factor = this->lowest_freq_input_factor;
    double value_factor = this->lowest_freq_value_factor;

    for (int i = 0; i < this->noise_levels.size(); ++i) {
        const auto& noise = this->noise_levels[i];
        if (noise.has_value()) {
            double noise_val = noise->noise(wrap(x * factor), wrap(y * factor), wrap(z * factor), y_scale * factor, y_fudge * factor);
            value += this->amplitudes[i] * noise_val * value_factor;
        }

        factor *= static_cast<double>(2.0F);
        value_factor /= static_cast<double>(2.0F);
    }

    return value;
}

auto PerlinNoise::max_broken_value(const double y_scale) const -> double {
    return this->edge_value(y_scale + 2.0);
}

auto PerlinNoise::wrap(const double x) -> double {
    constexpr double c = 3.3554432E7;
    const double k = std::floor(x / c + 0.5);
    return x - k * c;
}

auto PerlinNoise::get_first_octave() const -> int {
    return this->first_octave;
}

auto PerlinNoise::get_amplitudes() const -> std::vector<double> {
    return this->amplitudes;
}

auto PerlinNoise::edge_value(const double noise_value) const -> double {
    double value = 0.0;
    double value_factor = this->lowest_freq_value_factor;

    for (size_t i = 0; i < this->noise_levels.size(); i++) {
        value += this->amplitudes[i] * noise_value * value_factor;
        value_factor /= 2.0;
    }

    return value;
}

auto PerlinNoise::add_methods() -> void {
    "PerlinNoise::_create"_jf.reg<_create>();
    "PerlinNoise::_destroy"_jf.reg<&PerlinNoise::_destroy>();
    "PerlinNoise::get_value3"_jf.reg<static_cast<double(PerlinNoise::*)(double, double, double) const>(&PerlinNoise::get_value)>();
    "PerlinNoise::get_value5"_jf.reg<static_cast<double(PerlinNoise::*)(double, double, double, double, double) const>(&PerlinNoise::get_value)>();
    "PerlinNoise::edge_value"_jf.reg<&PerlinNoise::edge_value>();
    "PerlinNoise::_amplitudes"_jf.reg<&PerlinNoise::_amplitudes>();
    "PerlinNoise::_amplitudes_size"_jf.reg<&PerlinNoise::_amplitudes_size>();
}

auto PerlinNoise::init(std::mt19937_64& mt, const std::pair<int, std::vector<double>>& pair, const bool use_new_initialization) -> void {
    this->first_octave = pair.first;
    this->amplitudes = pair.second;
    const size_t octaves = this->amplitudes.size();
    const int zero_octave_index = -this->first_octave;
    this->noise_levels.resize(octaves);

    if (use_new_initialization) {
        const uint64_t fork_seed = mt();
        for (size_t i = 0; i < octaves; ++i) {
            if (this->amplitudes[i] != 0.0) {
                const int octave = this->first_octave + static_cast<int>(i);
                std::string octave_name = "octave_" + std::to_string(octave);
                constexpr std::hash<std::string> hasher;
                uint64_t octave_seed = fork_seed ^ hasher(octave_name);
                std::mt19937_64 octave_rng(octave_seed);
                noise_levels[i] = std::make_optional<ImprovedNoise>(octave_rng);
            } else {
                noise_levels[i] = std::nullopt;
            }
        }
    } else {
        ImprovedNoise zero_octave(mt);
        if (zero_octave_index >= 0 && std::cmp_less(zero_octave_index, octaves)) {
            if (this->amplitudes[zero_octave_index] != 0.0) {
                noise_levels[zero_octave_index] = zero_octave;
            } else {
                noise_levels[zero_octave_index] = std::nullopt;
            }
        }

        for (int ix = zero_octave_index - 1; ix >= 0; --ix) {
            if (std::cmp_less(ix, octaves)) {
                if (this->amplitudes[ix] != 0.0) {
                    noise_levels[ix] = std::make_optional<ImprovedNoise>(mt);
                } else {
                    noise_levels[ix] = std::nullopt;
                    for (int s = 0; s < 262; ++s) {
                        mt();
                    }
                }
            } else {
                for (int s = 0; s < 262; ++s) {
                    mt();
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

auto PerlinNoise::_create(const uint64_t seed, const int first_octave, double* amplitudes, const int size, const bool use_new_initialization) -> PerlinNoise* {
    return new PerlinNoise(seed, std::pair{first_octave, JavaUtil::to_vector<double>(amplitudes, size)}, use_new_initialization);
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
