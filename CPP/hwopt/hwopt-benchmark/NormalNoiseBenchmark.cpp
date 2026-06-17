#include <gtest/gtest.h>

#include <print>
#include "../hwopt/Minecraft/Noise/NormalNoise.h"

static std::array amplitudes{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
static constexpr int FIRST_OCTAVE = -8;
static constexpr bool USE_NEW_INIT = true;

class NormalNoiseBenchmark : public testing::Test {
protected:
    minecraft::NormalNoise* noise{nullptr};

    auto SetUp() -> void override {
        noise = minecraft::NormalNoise::_create(42, 1);
        noise->set_first(FIRST_OCTAVE, amplitudes, 0, 0, 1);
        noise->set_second(FIRST_OCTAVE, amplitudes, 0, 0, 1);

        int8_t p{0};
        std::array<int8_t, 256> prem{};
        for (int8_t& prem1 : prem) {
            prem1 += ++p;
        }

        for (int i = 0; i < amplitudes.size(); ++i) {
            noise->add_noise_to_first(i, 1, 1, 1, prem);
            noise->add_noise_to_second(i, 1, 1, 1, prem);
        }
    }

    auto TearDown() -> void override {
        noise->_destroy();
    }
};

TEST_F(NormalNoiseBenchmark, GetValueSequential) {
    constexpr int ITERATIONS = 10000000;
    double result = 0.0;
    for (int i = 0; i < ITERATIONS; i++) {
        const double x = static_cast<double>(i) * 0.1;
        const double y = static_cast<double>(i) * 0.2;
        const double z = static_cast<double>(i) * 0.3;
        result += noise->get_value(x, y, z);
    }
    ASSERT_NE(result, 0.0);
    std::println("Sequential sum: {:f}", result);
}
