#pragma once
#include <utility>
#include <vector>
#include <cmath>
#include <random>
#include <memory>

#include <stdpp/exception.h>
#include "ImprovedNoise.h"
#include "../JavaNative.h"

class PerlinNoise : JavaNative<PerlinNoise> {
public:
    PerlinNoise(const std::pair<int, std::vector<double>>& pair, bool use_new_initialization);

    [[nodiscard]] auto get_max_value() const -> double;

    [[nodiscard]] auto get_value(double x, double y, double z) const -> double;

    [[nodiscard]] auto get_value(double x, double y, double z, double y_scale, double y_fudge, bool y_flat_hack) const -> double;

    [[nodiscard]] auto max_broken_value(double y_scale) const -> double;


    auto get_octave_noise(int i) -> std::shared_ptr<ImprovedNoise>;

    static auto wrap(double x) -> double;

    [[nodiscard]] auto get_first_octave() const -> int;

    auto get_amplitudes() -> std::vector<double>;

    [[nodiscard]] auto edge_value(double noise_value) const -> double;

    static auto add_methods() -> void {
        register_method<static_cast<double(PerlinNoise::*)(double, double, double) const>(&PerlinNoise::get_value)>("PerlinNoise::get_value3");
        register_method<static_cast<double(PerlinNoise::*)(double, double, double, double, double, bool) const>(&PerlinNoise::get_value)>("PerlinNoise::get_value6");
        register_method<_destroy>("PerlinNoise::_destroy");
        register_method<_create>("PerlinNoise::_create");
    }

    static auto _create(const int first_octave, const double* amplitudes, const int size, const bool use_new_initialization) -> std::uint64_t try {
        thread_local auto _ = _set_se_translator(stdpp::exception::NativeException::seh_to_ce);
        std::vector<double> amps(size);
        std::memcpy(amps.data(), amplitudes, sizeof(double) * size);
        return reinterpret_cast<std::uint64_t>(new PerlinNoise({first_octave, amps}, use_new_initialization));
    } catch (const stdpp::exception::NativeException& exception) {
        ELOG << std::hex << exception.code() << " " << exception.what();
        return 0;
    } catch (std::exception& exception) {
        ELOG << exception.what();
        return 0;
    }

    static auto _destroy(const PerlinNoise* this_) -> void {
        if (this_) {
            delete this_;
        }
    }

private:
    std::vector<std::shared_ptr<ImprovedNoise>> noise_levels;
    int first_octave;
    std::vector<double> amplitudes;
    double lowest_freq_value_factor;
    double lowest_freq_input_factor;
    double max_value;
};
