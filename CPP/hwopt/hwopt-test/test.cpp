#include "pch.h"
#include "../hwopt/Minecraft/Noise/PerlinNoise.h"

TEST(NoiseBench, Perlin) {
    const minecraft::PerlinNoise noise(123456, {0, std::vector<double>{1.0, 1.0, 1.0, 1.0}}, true);

    constexpr int n = 1024;
    double sum = 0.0;

    for (int z = 0; z < n; ++z) {
        for (int y = 0; y < n; ++y) {
            for (int x = 0; x < n; ++x) {
                sum += noise.get_value(x * 0.01, y * 0.01, z * 0.01);
            }
        }
    }

    EXPECT_NE(sum, 0.0);
}
