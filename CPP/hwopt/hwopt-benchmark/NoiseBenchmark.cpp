#include <gtest/gtest.h>

#include <chrono>
#include <cmath>
#include <print>
#include <random>
#include <span>
#include <vector>

#include "../hwopt/Minecraft/Chunk/Noise//BlendedNoise.hpp"
#include "../hwopt/Minecraft/Chunk/Noise/ImprovedNoise.hpp"
#include "../hwopt/Minecraft/Chunk/Noise/NormalNoise.hpp"
#include "../hwopt/Minecraft/Chunk/Noise/PerlinNoise.hpp"
#include "../hwopt/Minecraft/Chunk/Noise/SimplexNoise.hpp"

static constexpr std::array AMPLITUDES_8{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
static constexpr std::array AMPLITUDES_16{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
static constexpr int FIRST_OCTAVE = -8;
static constexpr int OCTAVE_COUNT_8 = 8;
static constexpr int OCTAVE_COUNT_16 = 16;

static auto make_perm_i8() -> std::array<int8_t, 256> {
    std::array<int8_t, 256> perm{};
    int8_t p{0};
    for (auto& v : perm) {
        v = ++p;
    }
    return perm;
}

static auto make_perm_int() -> std::array<int, 256> {
    std::array<int, 256> perm{};
    int p{0};
    for (auto& v : perm) {
        v = ++p;
    }
    return perm;
}

class NoiseBenchmark : public testing::Test {
protected:
    minecraft::noise::NormalNoise* noise{nullptr};
    minecraft::noise::PerlinNoise* first{nullptr};
    minecraft::noise::PerlinNoise* second{nullptr};
    std::array<int8_t, 256> perm = make_perm_i8();
    std::vector<minecraft::noise::ImprovedNoise*> cleaners;

    auto SetUp() -> void override {
        noise = minecraft::noise::NormalNoise::_create(1.0, 1.0);
        first = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_8.data()), AMPLITUDES_8.size(), 1.0, 1.0, 2.0);
        second = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_8.data()), AMPLITUDES_8.size(), 1.0, 1.0, 2.0);
        for (int i = 0; i < OCTAVE_COUNT_8; ++i) {
            auto* in1 = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, perm.data(), perm.size());
            auto* in2 = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, perm.data(), perm.size());
            first->add_noise(i, in1);
            second->add_noise(i, in2);
            cleaners.push_back(in1);
            cleaners.push_back(in2);
        }
        noise->set_perlin_noise(first, second);
    }

    auto TearDown() -> void override {
        noise->_destroy();
        first->_destroy();
        second->_destroy();
        for (const auto* c : cleaners) {
            c->_destroy();
        }
        cleaners.clear();
    }
};

TEST_F(NoiseBenchmark, NormalNoiseGetValue) {
    constexpr int ITERATIONS = 5000000;
    double result = 0.0;
    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; i++) {
        result += noise->get_value(i * 0.1, i * 0.2, i * 0.3);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    ASSERT_NE(result, 0.0);
    std::println("NormalNoise::get_value x{}: {} ms (sum={:f})", ITERATIONS, ms, result);
}

class ImprovedNoiseBenchmark : public testing::Test {
protected:
    minecraft::noise::ImprovedNoise* n{nullptr};
    std::array<int8_t, 256> perm = make_perm_i8();

    auto SetUp() -> void override {
        n = minecraft::noise::ImprovedNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    }

    auto TearDown() -> void override {
        n->_destroy();
    }
};

TEST_F(ImprovedNoiseBenchmark, Noise3) {
    constexpr int ITERATIONS = 10000000;
    double result = 0.0;
    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; i++) {
        result += n->noise(i * 0.1, i * 0.2, i * 0.3);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    ASSERT_NE(result, 0.0);
    std::println("ImprovedNoise::noise3 x{}: {} ms (sum={:f})", ITERATIONS, ms, result);
}

TEST_F(ImprovedNoiseBenchmark, Noise5) {
    constexpr int ITERATIONS = 10000000;
    double result = 0.0;
    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; i++) {
        result += n->noise(i * 0.1, i * 0.2, i * 0.3, 4.0, 5.0);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    ASSERT_NE(result, 0.0);
    std::println("ImprovedNoise::noise5 x{}: {} ms (sum={:f})", ITERATIONS, ms, result);
}

TEST_F(ImprovedNoiseBenchmark, NoiseWithDerivative) {
    constexpr int ITERATIONS = 5000000;
    double result = 0.0;
    double deriv[3]{};
    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; i++) {
        deriv[0] = deriv[1] = deriv[2] = 0.0;
        result += n->noise_with_derivative(i * 0.1, i * 0.2, i * 0.3, deriv, 3);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    ASSERT_NE(result, 0.0);
    std::println("ImprovedNoise::noise_with_derivative x{}: {} ms (sum={:f})", ITERATIONS, ms, result);
}

class PerlinNoiseBenchmark : public testing::Test {
protected:
    minecraft::noise::PerlinNoise* perlin{nullptr};
    std::array<int8_t, 256> perm = make_perm_i8();
    std::vector<minecraft::noise::ImprovedNoise*> cleaners;

    auto SetUp() -> void override {
        perlin = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_8.data()), AMPLITUDES_8.size(), 1.0, 1.0, 2.0);
        for (int i = 0; i < OCTAVE_COUNT_8; ++i) {
            auto* inoise = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, perm.data(), perm.size());
            perlin->add_noise(i, inoise);
            cleaners.push_back(inoise);
        }
    }

    auto TearDown() -> void override {
        perlin->_destroy();
        for (const auto* c : cleaners) {
            c->_destroy();
        }
        cleaners.clear();
    }
};

TEST_F(PerlinNoiseBenchmark, GetValue3) {
    constexpr int ITERATIONS = 5000000;
    double result = 0.0;
    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; i++) {
        result += perlin->get_value(i * 0.1, i * 0.2, i * 0.3);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    ASSERT_NE(result, 0.0);
    std::println("PerlinNoise::get_value x{}: {} ms (sum={:f})", ITERATIONS, ms, result);
}

TEST_F(PerlinNoiseBenchmark, GetValue5) {
    constexpr int ITERATIONS = 5000000;
    double result = 0.0;
    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; i++) {
        result += perlin->get_value(i * 0.1, i * 0.2, i * 0.3, 4.0, 5.0);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    ASSERT_NE(result, 0.0);
    std::println("PerlinNoise::get_value x{}: {} ms (sum={:f})", ITERATIONS, ms, result);
}

class SimplexNoiseBenchmark : public testing::Test {
protected:
    minecraft::noise::SimplexNoise* s{nullptr};
    std::array<int, 256> perm = make_perm_int();

    auto SetUp() -> void override {
        s = minecraft::noise::SimplexNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    }

    auto TearDown() -> void override {
        s->_destroy();
    }
};

TEST_F(SimplexNoiseBenchmark, GetValue2) {
    constexpr int ITERATIONS = 10000000;
    double result = 0.0;
    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; i++) {
        result += s->get_value(i * 0.1, i * 0.2);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    ASSERT_NE(result, 0.0);
    std::println("SimplexNoise::get_value x{}: {} ms (sum={:f})", ITERATIONS, ms, result);
}

TEST_F(SimplexNoiseBenchmark, GetValue3) {
    constexpr int ITERATIONS = 10000000;
    double result = 0.0;
    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; i++) {
        result += s->get_value(i * 0.1, i * 0.2, i * 0.3);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    ASSERT_NE(result, 0.0);
    std::println("SimplexNoise::get_value x{}: {} ms (sum={:f})", ITERATIONS, ms, result);
}

class BlendedNoiseTest : public testing::Test {
protected:
    minecraft::noise::BlendedNoise* blended{nullptr};
    minecraft::noise::PerlinNoise* minLimit{nullptr};
    minecraft::noise::PerlinNoise* maxLimit{nullptr};
    minecraft::noise::PerlinNoise* mainNoise{nullptr};
    std::array<int8_t, 256> perm = make_perm_i8();
    std::vector<minecraft::noise::ImprovedNoise*> cleaners;

    auto SetUp() -> void override {
        minLimit = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_16.data()), AMPLITUDES_16.size(), 1.0, 1.0, 2.0);
        maxLimit = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_16.data()), AMPLITUDES_16.size(), 1.0, 1.0, 2.0);
        mainNoise = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_8.data()), AMPLITUDES_8.size(), 1.0, 1.0, 2.0);

        // BlendedNoise::compute iterates:
        //   mainNoise: 0..7 (8 octaves)
        //   minLimit/maxLimit: 0..15 (16 octaves via get_octave_noise)
        for (int i = 0; i < OCTAVE_COUNT_16; ++i) {
            auto* in_min = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, perm.data(), perm.size());
            auto* in_max = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, perm.data(), perm.size());
            minLimit->add_noise(i, in_min);
            maxLimit->add_noise(i, in_max);
            cleaners.push_back(in_min);
            cleaners.push_back(in_max);
            if (i < OCTAVE_COUNT_8) {
                auto* in_main = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, perm.data(), perm.size());
                mainNoise->add_noise(i, in_main);
                cleaners.push_back(in_main);
            }
        }

        blended = minecraft::noise::BlendedNoise::_create(minLimit, maxLimit, mainNoise, 0.25, 0.125, 1.0, 1.0, 0.5);
    }

    auto TearDown() -> void override {
        blended->_destroy();
        mainNoise->_destroy();
        minLimit->_destroy();
        maxLimit->_destroy();
        for (const auto* c : cleaners) {
            c->_destroy();
        }
        cleaners.clear();
    }
};

class BlendedNoiseBenchmark : public BlendedNoiseTest {};

TEST_F(BlendedNoiseBenchmark, Compute) {
    constexpr int ITERATIONS = 500000;
    double result = 0.0;
    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; i++) {
        result += blended->compute(i * 0.1, i * 0.2, i * 0.3);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    ASSERT_NE(result, 0.0);
    std::println("BlendedNoise::compute x{}: {} ms (sum={:f})", ITERATIONS, ms, result);
}
