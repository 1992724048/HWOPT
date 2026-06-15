#include <gtest/gtest.h>

#include <print>
#include "../hwopt/Minecraft/Noise/NormalNoise.h"

static constexpr std::array AMPLITUDES{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
static constexpr int FIRST_OCTAVE = -8;
static constexpr bool USE_NEW_INIT = true;

class NormalNoiseBenchmark : public testing::Test {
protected:
    minecraft::NormalNoise* noise{nullptr};

    auto SetUp() -> void override {
        noise = minecraft::NormalNoise::_create(42, FIRST_OCTAVE, AMPLITUDES.data(), AMPLITUDES.size(), USE_NEW_INIT);
    }

    auto TearDown() -> void override {
        noise->_destroy();
    }
};

TEST_F(NormalNoiseBenchmark, GetValueSequential) {
    constexpr int ITERATIONS = 10000000;
    double result = 0.0;
    for (int i = 0; i < ITERATIONS; i++) {
        const double x = static_cast<double>(i) * 0.001;
        const double y = static_cast<double>(i) * 0.002;
        const double z = static_cast<double>(i) * 0.003;
        result += noise->get_value(x, y, z);
    }
    ASSERT_NE(result, 0.0);
    std::println("Sequential sum: {:f}", result);
}
