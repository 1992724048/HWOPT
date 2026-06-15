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
    constexpr int ITERATIONS = 100000000;
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

TEST_F(NormalNoiseBenchmark, GetValueRandomAccess) {
    constexpr int ITERATIONS = 100000000;
    double result = 0.0;
    for (int i = 0; i < ITERATIONS; i++) {
        const double x = i * 31 % 1000;
        const double y = i * 37 % 1000;
        const double z = i * 41 % 1000;
        result += noise->get_value(x, y, z);
    }
    ASSERT_NE(result, 0.0);
    std::println("Random sum: {:f}", result);
}

TEST_F(NormalNoiseBenchmark, GetValueChunkGrid) {
    constexpr int CHUNK_SIZE = 128;
    constexpr int CHUNKS_X = 8;
    constexpr int CHUNKS_Z = 8;
    constexpr int BLOCKS = CHUNKS_X * CHUNKS_Z * CHUNK_SIZE * CHUNK_SIZE;
    double result = 0.0;
    for (int cx = 0; cx < CHUNKS_X; cx++) {
        for (int cz = 0; cz < CHUNKS_Z; cz++) {
            const double baseX = cx * CHUNK_SIZE;
            const double baseZ = cz * CHUNK_SIZE;
            for (int dx = 0; dx < CHUNK_SIZE; dx++) {
                for (int dz = 0; dz < CHUNK_SIZE; dz++) {
                    result += noise->get_value(baseX + dx, 64.0, baseZ + dz);
                }
            }
        }
    }
    ASSERT_NE(result, 0.0);
    std::println("Chunk-grid sum ({} samples): {:f}", BLOCKS, result);
}
