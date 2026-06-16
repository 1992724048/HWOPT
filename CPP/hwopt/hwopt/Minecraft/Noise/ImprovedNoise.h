#pragma once
#include <array>
#include <random>
#include "../../JavaNative.h"

namespace minecraft {
    class ImprovedNoise final : JavaNative<ImprovedNoise> {
    public:
        double xo{};
        double yo{};
        double zo{};
        std::array<uint8_t, 256> p{};

        ImprovedNoise() = default;
        ImprovedNoise(double xo, double yo, double zo, std::span<int8_t> perm);

        [[nodiscard]] auto noise(double x, double y, double z) const -> double;

        [[nodiscard]] auto noise(double x, double y, double z, double y_scale, double y_fudge) const -> double;

        [[nodiscard]] auto noise_with_derivative(double x, double y, double z, std::span<double> derivative_out) const -> double;

        static auto add_methods() -> void;
        static auto _create(double xo, double yo, double zo, std::span<int8_t> array) -> ImprovedNoise*;
        auto _destroy() const -> void;
    private:
        inline static auto grad_dot(int hash, double x, double y, double z) -> double;

        [[nodiscard]] auto perm(int x) const -> int;

        [[nodiscard]] auto sample_and_lerperm(int x, int y, int z, double xr, double yr, double zr, double yr_original) const -> double;

        [[nodiscard]] auto sample_with_derivative(int x, int y, int z, double xr, double yr, double zr, std::span<double> derivative_out) const -> double;

        inline static auto smoothstep(double x) -> double;

        inline static auto smoothstep_derivative(double x) -> double;

        inline static auto lerp(double alpha1, double p0, double p1) -> double;

        inline static auto lerp2(double alpha1, double alpha2, double x00, double x10, double x01, double x11) -> double;

        inline static auto lerp3(double alpha1, double alpha2, double alpha3, double x000, double x100, double x010, double x110, double x001, double x101, double x011, double x111) -> double;

        static auto dot(const int* g, double x, double y, double z) -> double;

        static constexpr std::array<int, 48> GRADIENT{
            1,
            1,
            0,
            -1,
            1,
            0,
            1,
            -1,
            0,
            -1,
            -1,
            0,
            1,
            0,
            1,
            -1,
            0,
            1,
            1,
            0,
            -1,
            -1,
            0,
            -1,
            0,
            1,
            1,
            0,
            -1,
            1,
            0,
            1,
            -1,
            0,
            -1,
            -1,
            1,
            1,
            0,
            0,
            -1,
            1,
            -1,
            1,
            0,
            0,
            -1,
            -1
        };
    };
} // namespace minecraft

namespace fortran {
    using namespace minecraft;
    #define DLL_API __declspec(dllimport)

    extern "C" {
        DLL_API auto ImprovedNoise_noise_3(const ImprovedNoise* state, double x, double y, double z) -> double;
        DLL_API auto ImprovedNoise_noise_5(const ImprovedNoise* state, double x, double y, double z, double yScale, double yFudge) -> double;
        DLL_API auto ImprovedNoise_noise_with_derivative(const ImprovedNoise* state, double x, double y, double z, double* derivativeOut) -> double;
    }
} // namespace fortran
