// 遂沫 PerlinNoise.cpp
// 2026-02-13 03:47:34

#include "PerlinNoise.h"
#include <Windows.h>
#include <glm/glm.hpp>
#include <sycl/sycl.hpp>
#include "ImprovedNoise.h"
#include "sycl-queue.h"

using namespace minecraft_sycl;

PerlinNoise::PerlinNoise(const int queue_id, bool& init_done, const uint64_t seed, const std::pair<int, std::vector<double>>& pair, const bool use_new_initialization) : queue_id{queue_id},
    amplitudes_size{0},
    noise_levels{nullptr} {
    if (!queue_map.contains(queue_id)) {
        init_done = false;
        return;
    }

    const auto queue = queue_map[queue_id].load();

    std::mt19937_64 mt(seed);
    this->first_octave = pair.first;
    this->amplitudes_size = pair.second.size();
    const size_t octaves = this->amplitudes_size;
    const int zero_octave_index = this->first_octave < 0 ? -this->first_octave : this->first_octave;
    this->noise_levels_size = octaves;

    this->amplitudes = sycl::malloc_device<double>(amplitudes_size, *queue);
    this->noise_levels = sycl::malloc_device<ImprovedNoise>(noise_levels_size, *queue);

    std::vector<ImprovedNoise> host_noise(octaves);

    if (use_new_initialization) {
        for (int i = 0; std::cmp_less(i, octaves); i++) {
            if (pair.second[i] != 0.0) {
                const int octave = this->first_octave + i;
                std::mt19937_64 mt_(octave);
                host_noise[i] = ImprovedNoise(mt_);
            }
        }
    } else {
        if (zero_octave_index >= 0 && std::cmp_less(zero_octave_index, octaves)) {
            if (pair.second[zero_octave_index]) {
                host_noise[zero_octave_index] = ImprovedNoise(mt);
            }
        }

        for (int ix = zero_octave_index - 1; ix >= 0; ix--) {
            if (std::cmp_less(ix, octaves)) {
                if (pair.second[ix]) {
                    host_noise[ix] = ImprovedNoise(mt);
                }
            }
        }

        if (zero_octave_index < octaves - 1) {
            init_done = false;
            sycl::free(this->amplitudes, *queue);
            sycl::free(this->noise_levels, *queue);
            return;
        }
    }

    auto event_amp = queue->memcpy(this->amplitudes, pair.second.data(), this->amplitudes_size * sizeof(double));
    auto event_noise = queue->memcpy(this->noise_levels, host_noise.data(), host_noise.size() * sizeof(ImprovedNoise));

    this->lowest_freq_input_factor = std::pow(2.0, -zero_octave_index);
    this->lowest_freq_value_factor = std::pow(2.0, octaves - 1) / (std::pow(2.0, octaves) - 1.0);
    this->max_value = this->edge_value(2.0);

    event_amp.wait();
    event_noise.wait();
    init_done = true;
}

PerlinNoise::~PerlinNoise() {
    const auto queue = queue_map[queue_id].load();
    sycl::free(this->amplitudes, *queue);
    sycl::free(this->noise_levels, *queue);
}

auto PerlinNoise::get_max_value() const -> double {
    return this->max_value;
}

auto PerlinNoise::get_value(const double x, const double y, const double z) const -> double {
    return this->get_value(x, y, z, 0.0, 0.0, false);
}

auto PerlinNoise::get_value(const double x, const double y, const double z, const double y_scale, const double y_fudge, const bool y_flat_hack) const -> double {
    struct KernelParams {
        double value{0.0};
        double factor{0.0};
        double value_factor{0.0};
    } host_params;

    host_params.factor = this->lowest_freq_input_factor;
    host_params.value_factor = this->lowest_freq_value_factor;

    const auto queue = queue_map[queue_id].load();
    auto params = sycl::DeviceMemory<KernelParams>::alloc(1, queue);
    params.copy_from(&host_params);

    const size_t n = this->noise_levels_size;
    const auto* noises = this->noise_levels;
    const auto* amps = this->amplitudes;

    queue->single_task([=, params_ = params.ptr] {
        for (size_t i = 0; i < n; ++i) {
            const ImprovedNoise& noise = noises[i];

            const double xf = wrap(x * params_->factor);
            const double yf = y_flat_hack ? -noise.yo : wrap(y * params_->factor);
            const double zf = wrap(z * params_->factor);

            const double noise_val = noise.noise(xf, yf, zf, y_scale * params_->factor, y_fudge * params_->factor);

            params_->value += amps[i] * noise_val * params_->value_factor;
            params_->factor *= 2.0;
            params_->value_factor *= 0.5;
        }
    }).wait();

    params.copy_to(&host_params);
    return host_params.value;
}

auto PerlinNoise::max_broken_value(const double y_scale) const -> double {
    return this->edge_value(y_scale + 2.0);
}

auto PerlinNoise::get_values(const std::vector<Tuple>& pos_vec) const -> std::expected<std::vector<double>, std::string> try {
    const auto queue = queue_map[queue_id].load();
    if (!queue) {
        return std::unexpected("[SYCL] Invalid queue");
    }

    const size_t N = pos_vec.size();
    if (N == 0) {
        return std::vector<double>{};
    }

    auto tuple = sycl::DeviceMemory<Tuple>::alloc(pos_vec.size(), queue);
    auto values = sycl::DeviceMemory<double>::alloc(pos_vec.size(), queue);

    const size_t n = this->noise_levels_size;
    const auto* noises = this->noise_levels;
    const auto* amps = this->amplitudes;

    double factor_ = this->lowest_freq_input_factor;
    double value_factor_ = this->lowest_freq_value_factor;

    constexpr size_t WG_SIZE = 256;
    const size_t global_size = (N + WG_SIZE - 1) / WG_SIZE * WG_SIZE;

    std::vector<double> ret(values.count);
    queue->submit([&](sycl::handler& h) {
        sycl::local_accessor<ImprovedNoise> local_noises(sycl::range(n), h);
        sycl::local_accessor<double> local_amps(sycl::range(n), h);

        h.memcpy(tuple.ptr, pos_vec.data(), pos_vec.size() * sizeof(Tuple));

        h.parallel_for(sycl::nd_range<>(global_size, WG_SIZE),
                       [N, local_noises, local_amps, factor_, value_factor_, n, noises, amps, params = tuple.ptr, vals = values.ptr](const sycl::nd_item<> item) {
                           if (item.get_local_id(0) < n) {
                               local_noises[item.get_local_id(0)] = noises[item.get_local_id(0)];
                               local_amps[item.get_local_id(0)] = amps[item.get_local_id(0)];
                           }
                           item.barrier(sycl::access::fence_space::local_space);

                           const size_t i = item.get_global_id(0);
                           if (i >= N) {
                               return;
                           }

                           double factor = factor_;
                           double value_factor = value_factor_;
                           double local_sum = 0.0;

                           const auto& [pos, xy, y_flat_hack] = params[i];

                           for (size_t noise_idx = 0; noise_idx < n; ++noise_idx) {
                               const ImprovedNoise& noise = noises[noise_idx];

                               const double zf = wrap(pos.z * factor);
                               const double xf = wrap(pos.x * factor);
                               const double yf = y_flat_hack ? -noise.yo : wrap(pos.y * factor);

                               const double noise_val = noise.noise(xf, yf, zf, xy.x * factor, xy.y * factor);
                               local_sum += amps[noise_idx] * noise_val * value_factor;

                               factor *= 2.0;
                               value_factor *= 0.5;
                           }

                           vals[i] = local_sum;
                       });
        h.memcpy(ret.data(), values.ptr, values.count * sizeof(double));
    }).wait();

    return ret;
} catch (const sycl::exception& exception) {
    return std::unexpected(std::format("[SYCL] [ERROR] {}", exception.what()));
}

auto PerlinNoise::get_first_octave() const -> int {
    return this->first_octave;
}

auto PerlinNoise::get_amplitudes() const -> std::vector<double> {
    if (!queue_map.contains(queue_id)) {
        return {};
    }
    const auto queue = queue_map[queue_id].load();
    std::vector<double> vec(amplitudes_size);
    queue->memcpy(vec.data(), this->amplitudes, vec.size()).wait();
    return vec;
}

auto PerlinNoise::edge_value(const double noise_value) const -> double {
    double value = 0.0;
    double value_factor = this->lowest_freq_value_factor;

    for (const double amp : get_amplitudes()) {
        value += amp * noise_value * value_factor;
        value_factor /= 2.0;
    }

    return value;
}

auto PerlinNoise::wrap(const double x) -> double {
    constexpr double c = 3.3554432E7;
    constexpr double inv_c = 1.0 / c;
    const double k = sycl::floor(x * inv_c + 0.5);
    return sycl::fma(-k, c, x);
}
