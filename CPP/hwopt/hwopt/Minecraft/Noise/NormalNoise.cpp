#include "NormalNoise.h"
#include <algorithm>

#include "../../util.h"

namespace minecraft {
    NormalNoise::NormalNoise(const uint64_t& random, const std::pair<int, std::vector<double>>& pair, const bool use_new_initialization) : first_{},
        second_{},
        first_octave(pair.first),
        amplitudes{pair.second} {
        JavaNative::touch();
        std::mt19937_64 rng(random);
        first_ = PerlinNoise(rng, std::pair(pair.first, pair.second), use_new_initialization);
        second_ = PerlinNoise(rng, std::pair(pair.first, pair.second), use_new_initialization);
        int min_octave = std::numeric_limits<int>::max();
        int max_octave = std::numeric_limits<int>::min();
        for (size_t i = 0; i < pair.second.size(); ++i) {
            if (pair.second[i] != 0.0) {
                int octave_index = static_cast<int>(i);
                min_octave = std::min(min_octave, octave_index);
                max_octave = std::max(max_octave, octave_index);
            }
        }
        const int octaveSpan = max_octave - min_octave;
        value_factor_ = 1.0 / 6.0 / expected_deviation(octaveSpan);
        max_value_ = (first_.get_max_value() + second_.get_max_value()) * value_factor_;
    }

    auto NormalNoise::get_value(const double x, const double y, const double z) const -> double {
        const double x2 = x * INPUT_FACTOR;
        const double y2 = y * INPUT_FACTOR;
        const double z2 = z * INPUT_FACTOR;
        const double val1 = first_.get_value(x, y, z);
        const double val2 = second_.get_value(x2, y2, z2);
        return (val1 + val2) * value_factor_;
    }

    auto NormalNoise::add_methods() -> void {
        "NormalNoise::_create"_jf.reg<_create>();
        "NormalNoise::_destroy"_jf.reg<&NormalNoise::_destroy>();
        "NormalNoise::get_value"_jf.reg<&NormalNoise::get_value>();
        "NormalNoise::max_value"_jf.reg<&NormalNoise::max_value>();
        "NormalNoise::expected_deviation"_jf.reg<&NormalNoise::expected_deviation>();
    }

    auto NormalNoise::_create(uint64_t seed, int first_octave, double* amplitudes, const int size, bool use_new_initialization) -> NormalNoise* {
        return new NormalNoise(seed, std::pair{first_octave, JavaUtil::to_vector<double>(amplitudes, size)}, use_new_initialization);
    }

    auto NormalNoise::_destroy() const -> void {
        delete this;
    }

    auto NormalNoise::expected_deviation(const int octave_span) noexcept -> double {
        return 0.1 * (1.0 + 1.0 / static_cast<double>(octave_span + 1));
    }
} // namespace minecraft
