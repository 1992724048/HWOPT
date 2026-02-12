// 遂沫 PerlinNoise.h
// 2026-02-13 02:16:21

#pragma once
#include <cstdint>
#include <expected>
#include <string>
#include <utility>
#include <vector>

#include <glm/glm.hpp>

namespace minecraft_sycl {
    class ImprovedNoise;

    struct Tuple {
        glm::vec3 pos;
        glm::vec2 xy{0, 0};
        bool y_flat_hack{false};
    };

    class __declspec(dllexport) PerlinNoise final {
    public:
        PerlinNoise(int queue_id, bool& init_done, uint64_t seed, const std::pair<int, std::vector<double>>& pair, bool use_new_initialization);
        ~PerlinNoise();

        [[nodiscard]] auto get_max_value() const -> double;
        [[nodiscard]] auto get_value(double x, double y, double z) const -> double;
        [[nodiscard]] auto get_value(double x, double y, double z, double y_scale, double y_fudge, bool y_flat_hack) const -> double;
        [[nodiscard]] auto max_broken_value(double y_scale) const -> double;

        [[nodiscard]] auto get_values(const std::vector<Tuple>& pos_vec) const -> std::expected<std::vector<double>, std::string>;

        [[nodiscard]] auto get_first_octave() const -> int;
        [[nodiscard]] auto get_amplitudes() const -> std::vector<double>;
        [[nodiscard]] auto edge_value(double noise_value) const -> double;

    private:
        inline static auto wrap(double x) -> double;

        int queue_id;
        int first_octave;
        double max_value;
        double lowest_freq_value_factor;
        double lowest_freq_input_factor;
        size_t amplitudes_size;
        size_t noise_levels_size;

        /**
         * @brief GPU 显存地址
         * @warning 请勿直接读取
         */
        double* amplitudes;

        /**
         * @brief GPU 显存地址
         * @warning 请勿直接读取
         */
        ImprovedNoise* noise_levels;
    };
} // namespace minecraft_sycl
