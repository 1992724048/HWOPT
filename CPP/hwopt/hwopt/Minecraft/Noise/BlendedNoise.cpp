#include "BlendedNoise.hpp"

#include "../Math.hpp"

#include "../../util.hpp"
using namespace minecraft::noise;

auto BlendedNoise::add_methods() -> void {
    "BlendedNoise::_create"_jf.reg<_create>();
    "BlendedNoise::_destroy"_jf.reg<&BlendedNoise::_destroy>();
    "BlendedNoise::compute"_jf.reg<&BlendedNoise::compute>();
    "BlendedNoise::get_values"_jf.reg<&BlendedNoise::get_values>();
}

auto BlendedNoise::_create(PerlinNoise* min_limit_noise,
                           PerlinNoise* max_limit_noise,
                           PerlinNoise* main_noise,
                           const double xz_scale,
                           const double y_scale,
                           const double xz_factor,
                           const double y_factor,
                           const double smear_scale_multiplier) -> BlendedNoise* {
    auto* noise = hwopt::util::mi_new<BlendedNoise>();
    noise->min_limit_noise = min_limit_noise;
    noise->max_limit_noise = max_limit_noise;
    noise->main_noise = main_noise;
    noise->xz_scale = xz_scale;
    noise->y_scale = y_scale;
    noise->xz_factor = xz_factor;
    noise->y_factor = y_factor;
    noise->smear_scale_multiplier = smear_scale_multiplier;
    noise->xz_multiplier = 684.412 * noise->xz_scale;
    noise->y_multiplier = 684.412 * noise->y_scale;
    noise->max_value = min_limit_noise->max_broken_value(noise->y_multiplier);
    return noise;
}

auto BlendedNoise::_destroy() const -> void {
    hwopt::util::mi_delete(this);
}

auto BlendedNoise::compute(const double limit_x, const double limit_y, const double limit_z) const -> double {
    const double mainX = limit_x / this->xz_factor;
    const double mainY = limit_y / this->y_factor;
    const double mainZ = limit_z / this->xz_factor;
    const double limitSmear = this->y_multiplier * this->smear_scale_multiplier;
    const double mainSmear = limitSmear / this->y_factor;
    double blend_min = 0.0F;
    double blend_max = 0.0F;
    double main_noise_value = 0.0F;
    double pow = 1.0F;

    #pragma omp simd
    for (int i = 0; i < 8; ++i) {
        if (const ImprovedNoise* noise = this->main_noise->get_octave_noise(i)) {
            main_noise_value += noise->noise(PerlinNoise::wrap(mainX * pow), PerlinNoise::wrap(mainY * pow), PerlinNoise::wrap(mainZ * pow), mainSmear * pow, mainY * pow) / pow;
        }
        pow *= 0.5F;
    }

    const double factor = ((main_noise_value / static_cast<double>(10.0F)) + static_cast<double>(1.0F)) / static_cast<double>(2.0F);
    const bool isMax = factor >= static_cast<double>(1.0F);
    const bool isMin = factor <= static_cast<double>(0.0F);
    pow = 1.0F;

    #pragma omp simd
    for (int i = 0; i < 16; ++i) {
        const double wx = PerlinNoise::wrap(limit_x * pow);
        const double wy = PerlinNoise::wrap(limit_y * pow);
        const double wz = PerlinNoise::wrap(limit_z * pow);
        const double yScalePow = limitSmear * pow;
        if (!isMax) {
            if (const ImprovedNoise* min_noise = this->min_limit_noise->get_octave_noise(i)) {
                blend_min += min_noise->noise(wx, wy, wz, yScalePow, limit_y * pow) / pow;
            }
        }

        if (!isMin) {
            if (const ImprovedNoise* max_noise = this->max_limit_noise->get_octave_noise(i)) {
                blend_max += max_noise->noise(wx, wy, wz, yScalePow, limit_y * pow) / pow;
            }
        }
        pow *= 0.5F;
    }

    return math::Math::clamped_lerp(factor, blend_min / static_cast<double>(512.0F), blend_max / static_cast<double>(512.0F)) / static_cast<double>(128.0F);
}

auto BlendedNoise::get_values(const double* xs, int xs_len, const double* ys, int ys_len, const double* zs, int zs_len, double* result, int result_len) const -> void {
    for (int i = 0; i < result_len; i++) {
        result[i] = compute(xs[i], ys[i], zs[i]);
    }
}
