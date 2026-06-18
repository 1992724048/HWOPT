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

auto BlendedNoise::_create(PerlinNoise* minLimitNoise,
                           PerlinNoise* maxLimitNoise,
                           PerlinNoise* mainNoise,
                           const double xzScale,
                           const double yScale,
                           const double xzFactor,
                           const double yFactor,
                           const double smearScaleMultiplier) -> BlendedNoise* {
    auto* noise = hwopt::util::mi_new<BlendedNoise>();
    noise->minLimitNoise = minLimitNoise;
    noise->maxLimitNoise = maxLimitNoise;
    noise->mainNoise = mainNoise;
    noise->xzScale = xzScale;
    noise->yScale = yScale;
    noise->xzFactor = xzFactor;
    noise->yFactor = yFactor;
    noise->smearScaleMultiplier = smearScaleMultiplier;
    noise->xzMultiplier = 684.412 * noise->xzScale;
    noise->yMultiplier = 684.412 * noise->yScale;
    noise->maxValue = minLimitNoise->max_broken_value(noise->yMultiplier);
    return noise;
}

auto BlendedNoise::_destroy() const -> void {
    hwopt::util::mi_delete(this);
}

auto BlendedNoise::compute(const double limitX, const double limitY, const double limitZ) const -> double {
    const double mainX = limitX / this->xzFactor;
    const double mainY = limitY / this->yFactor;
    const double mainZ = limitZ / this->xzFactor;
    const double limitSmear = this->yMultiplier * this->smearScaleMultiplier;
    const double mainSmear = limitSmear / this->yFactor;
    double blend_min = 0.0F;
    double blend_max = 0.0F;
    double main_noise_value = 0.0F;
    double pow = 1.0F;

    for (int i = 0; i < 8; ++i) {
        if (const ImprovedNoise* noise = this->mainNoise->get_octave_noise(i)) {
            main_noise_value += noise->noise(PerlinNoise::wrap(mainX * pow), PerlinNoise::wrap(mainY * pow), PerlinNoise::wrap(mainZ * pow), mainSmear * pow, mainY * pow) / pow;
        }
        pow *= 0.5F;
    }

    const double factor = ((main_noise_value / static_cast<double>(10.0F)) + static_cast<double>(1.0F)) / static_cast<double>(2.0F);
    const bool isMax = factor >= static_cast<double>(1.0F);
    const bool isMin = factor <= static_cast<double>(0.0F);
    pow = 1.0F;

    for (int i = 0; i < 16; ++i) {
        const double wx = PerlinNoise::wrap(limitX * pow);
        const double wy = PerlinNoise::wrap(limitY * pow);
        const double wz = PerlinNoise::wrap(limitZ * pow);
        const double yScalePow = limitSmear * pow;
        if (!isMax) {
            if (const ImprovedNoise* min_noise = this->minLimitNoise->get_octave_noise(i)) {
                blend_min += min_noise->noise(wx, wy, wz, yScalePow, limitY * pow) / pow;
            }
        }

        if (!isMin) {
            if (const ImprovedNoise* max_noise = this->maxLimitNoise->get_octave_noise(i)) {
                blend_max += max_noise->noise(wx, wy, wz, yScalePow, limitY * pow) / pow;
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
