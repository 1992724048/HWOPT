#pragma once
#include <array>
#include <random>
#include "../../../JavaNative.hpp"

namespace minecraft::noise {
    class HWOPT_API ImprovedNoise final : JavaNative<ImprovedNoise> {
    public:
        double xo{};
        double yo{};
        double zo{};
        std::array<uint8_t, 256> p{};

        ImprovedNoise() = default;
        ImprovedNoise(double xo, double yo, double zo, std::span<int8_t> perm);

        [[nodiscard]] auto noise(double x, double y, double z) const -> double;

        [[nodiscard]] auto noise(double x, double y, double z, double y_scale, double y_fudge) const -> double;

        [[nodiscard]] auto noise_with_derivative(double x, double y, double z, double* derivative_out, int derivative_out_size) const -> double;
        auto get_values(const double* xs, int xs_len, const double* ys, int ys_len, const double* zs, int zs_len, double* result, int result_len) const -> void;

        static auto add_methods() -> void;
        static auto _create(double xo, double yo, double zo, const int8_t* array, int array_size) -> ImprovedNoise*;
        auto _destroy() const -> void;
    private:
        static auto grad_dot(int hash, double x, double y, double z) -> double;

        [[nodiscard]] auto perm(int x) const -> int;

        [[nodiscard]] auto sample_and_lerperm(int x, int y, int z, double xr, double yr, double zr, double yr_original) const -> double;

        [[nodiscard]] auto sample_with_derivative(int x, int y, int z, double xr, double yr, double zr, double* derivative_out, int derivative_out_size) const -> double;

        static constexpr std::array<double, 48> GRADIENT{
            1.0,
            1.0,
            0.0,
            -1.0,
            1.0,
            0.0,
            1.0,
            -1.0,
            0.0,
            -1.0,
            -1.0,
            0.0,
            1.0,
            0.0,
            1.0,
            -1.0,
            0.0,
            1.0,
            1.0,
            0.0,
            -1.0,
            -1.0,
            0.0,
            -1.0,
            0.0,
            1.0,
            1.0,
            0.0,
            -1.0,
            1.0,
            0.0,
            1.0,
            -1.0,
            0.0,
            -1.0,
            -1.0,
            1.0,
            1.0,
            0.0,
            -1.0,
            1.0,
            0.0,
            0.0,
            -1.0,
            1.0,
            0.0,
            -1.0,
            -1.0
        };
    };
} // namespace minecraft

namespace fortran {
    using namespace minecraft;
    #define DLL_API __declspec(dllimport)

    // 比C++慢太多，已弃用

    extern "C" {
        DLL_API auto ImprovedNoise_noise_3(const noise::ImprovedNoise* state, double x, double y, double z) -> double;
        DLL_API auto ImprovedNoise_noise_5(const noise::ImprovedNoise* state, double x, double y, double z, double yScale, double yFudge) -> double;
        DLL_API auto ImprovedNoise_noise_with_derivative(const noise::ImprovedNoise* state, double x, double y, double z, double* derivativeOut) -> double;
    }
} // namespace fortran
