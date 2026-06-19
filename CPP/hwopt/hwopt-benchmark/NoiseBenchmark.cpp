#include <gtest/gtest.h>

#include <cmath>
#include <print>
#include <random>
#include <span>
#include <vector>

#include "../hwopt/Minecraft/Noise/BlendedNoise.hpp"
#include "../hwopt/Minecraft/Noise/ImprovedNoise.hpp"
#include "../hwopt/Minecraft/Noise/NormalNoise.hpp"
#include "../hwopt/Minecraft/Noise/PerlinNoise.hpp"
#include "../hwopt/Minecraft/Noise/SimplexNoise.hpp"

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

// =============================================================================
// ImprovedNoise Unit Tests
// =============================================================================

class ImprovedNoiseTest : public testing::Test {
protected:
    std::array<int8_t, 256> perm = make_perm_i8();
};

TEST_F(ImprovedNoiseTest, CreateDestroy) {
    auto* n = minecraft::noise::ImprovedNoise::_create(0.5, 0.25, 0.125, perm.data(), perm.size());
    ASSERT_NE(n, nullptr);
    EXPECT_DOUBLE_EQ(n->xo, 0.5);
    EXPECT_DOUBLE_EQ(n->yo, 0.25);
    EXPECT_DOUBLE_EQ(n->zo, 0.125);
    for (int i = 0; i < 256; ++i) {
        EXPECT_EQ(n->p[i], perm[i]);
    }
    n->_destroy();
}

TEST_F(ImprovedNoiseTest, DeterministicNoise) {
    const auto* n = minecraft::noise::ImprovedNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    const double r1 = n->noise(1.5, 2.3, 3.7);
    const double r2 = n->noise(1.5, 2.3, 3.7);
    EXPECT_DOUBLE_EQ(r1, r2);
    n->_destroy();
}

TEST_F(ImprovedNoiseTest, Noise3And5MatchWithZeroParams) {
    const auto* n = minecraft::noise::ImprovedNoise::_create(0.1, 0.2, 0.3, perm.data(), perm.size());
    const double r3 = n->noise(1.0, 2.0, 3.0);
    const double r5 = n->noise(1.0, 2.0, 3.0, 0.0, 0.0);
    EXPECT_DOUBLE_EQ(r3, r5);
    n->_destroy();
}

TEST_F(ImprovedNoiseTest, NoiseOutputNonZero) {
    const auto* n = minecraft::noise::ImprovedNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    const double r = n->noise(10.5, 20.3, 30.7);
    EXPECT_NE(r, 0.0);
    n->_destroy();
}

TEST_F(ImprovedNoiseTest, NoiseWithDerivative) {
    const auto* n = minecraft::noise::ImprovedNoise::_create(0.1, 0.2, 0.3, perm.data(), perm.size());
    double deriv[3]{};
    const double v = n->noise_with_derivative(1.5, 2.5, 3.5, deriv, 3);
    EXPECT_NE(v, 0.0);
    EXPECT_NE(deriv[0], 0.0);
    EXPECT_NE(deriv[1], 0.0);
    EXPECT_NE(deriv[2], 0.0);
    n->_destroy();
}

TEST_F(ImprovedNoiseTest, DifferentOffsetsProduceDifferentOutput) {
    const auto perm2 = make_perm_i8();
    const auto* n1 = minecraft::noise::ImprovedNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    const auto* n2 = minecraft::noise::ImprovedNoise::_create(1.0, 2.0, 3.0, perm2.data(), perm2.size());
    const double r1 = n1->noise(5.5, 5.5, 5.5);
    const double r2 = n2->noise(5.5, 5.5, 5.5);
    EXPECT_NE(r1, r2);
    n1->_destroy();
    n2->_destroy();
}

// =============================================================================
// PerlinNoise Unit Tests
// =============================================================================

class PerlinNoiseTest : public testing::Test {
protected:
    minecraft::noise::PerlinNoise* perlin{nullptr};
    std::array<int8_t, 256> perm = make_perm_i8();

    auto SetUp() -> void override {
        perlin = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_8.data()), AMPLITUDES_8.size(), 1.0, 1.0, 2.0);
    }

    auto TearDown() -> void override {
        perlin->_destroy();
    }
};

class PerlinNoiseWithOctavesTest : public PerlinNoiseTest {
protected:
    std::vector<minecraft::noise::ImprovedNoise*> cleaners;

    auto SetUp() -> void override {
        PerlinNoiseTest::SetUp();
        for (int i = 0; i < OCTAVE_COUNT_8; ++i) {
            auto* inoise = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, perm.data(), perm.size());
            perlin->add_noise(i, inoise);
            cleaners.push_back(inoise);
        }
    }

    auto TearDown() -> void override {
        for (const auto* c : cleaners) {
            c->_destroy();
        }
        cleaners.clear();
        PerlinNoiseTest::TearDown();
    }
};

TEST_F(PerlinNoiseTest, CreateDestroy) {
    ASSERT_NE(perlin, nullptr);
    EXPECT_EQ(perlin->first_octave_, FIRST_OCTAVE);
    EXPECT_EQ(perlin->amplitudes_.size(), OCTAVE_COUNT_8);
    ASSERT_EQ(perlin->noise_levels_.size(), OCTAVE_COUNT_8);
    for (const auto* nl : perlin->noise_levels_) {
        EXPECT_EQ(nl, nullptr);
    }
}

TEST_F(PerlinNoiseTest, AddNoise) {
    auto* inoise = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, perm.data(), perm.size());
    perlin->add_noise(0, inoise);
    ASSERT_EQ(perlin->noise_levels_.size(), OCTAVE_COUNT_8);
    ASSERT_NE(perlin->noise_levels_[0], nullptr);
    EXPECT_EQ(perlin->noise_levels_[0], inoise);
    inoise->_destroy();
}

TEST_F(PerlinNoiseWithOctavesTest, DeterministicGetValue3) {
    const double r1 = perlin->get_value(10.125, 20.375, 30.625);
    const double r2 = perlin->get_value(10.125, 20.375, 30.625);
    EXPECT_DOUBLE_EQ(r1, r2);
}

TEST_F(PerlinNoiseWithOctavesTest, NonZeroGetValue3) {
    const double r = perlin->get_value(10.125, 20.375, 30.625);
    EXPECT_NE(r, 0.0);
}

TEST_F(PerlinNoiseWithOctavesTest, GetValue3MatchesGetValue5ZeroParams) {
    const double r3 = perlin->get_value(10.125, 20.375, 30.625);
    const double r5 = perlin->get_value(10.125, 20.375, 30.625, 0.0, 0.0);
    EXPECT_DOUBLE_EQ(r3, r5);
}

TEST_F(PerlinNoiseWithOctavesTest, NonZeroGetValue5) {
    const double r = perlin->get_value(10.125, 20.375, 30.625, 4.0, 5.0);
    EXPECT_NE(r, 0.0);
}

TEST_F(PerlinNoiseTest, EdgeValue) {
    const double ev = perlin->edge_value(1.0);
    // sum_{i=0}^{7} (1.0 * 1.0 * 0.5^i) = 2 * (1 - 0.5^8) = 1.9921875
    EXPECT_DOUBLE_EQ(ev, 1.9921875);
}

TEST_F(PerlinNoiseTest, EdgeValueZeroAmplitude) {
    const auto* p = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_8.data()), AMPLITUDES_8.size(), 0.0, 1.0, 0.0);
    const double ev = p->edge_value(1.0);
    EXPECT_DOUBLE_EQ(ev, 0.0);
    p->_destroy();
}

TEST_F(PerlinNoiseTest, MaxBrokenValue) {
    const double mbv = perlin->max_broken_value(1.0);
    // edge_value(y_scale + 2.0) = edge_value(3.0) = 3.0 * 1.9921875 = 5.9765625
    EXPECT_DOUBLE_EQ(mbv, 5.9765625);
}

TEST_F(PerlinNoiseTest, Amplitudes) {
    const std::span<double> amp = perlin->amplitudes();
    ASSERT_EQ(amp.size(), OCTAVE_COUNT_8);
    for (size_t i = 0; i < amp.size(); ++i) {
        EXPECT_DOUBLE_EQ(amp[i], AMPLITUDES_8[i]);
    }
}

TEST_F(PerlinNoiseWithOctavesTest, GetOctaveNoise) {
    for (int i = 0; i < OCTAVE_COUNT_8; ++i) {
        auto* on = perlin->get_octave_noise(i);
        ASSERT_NE(on, nullptr);
        EXPECT_EQ(on, perlin->noise_levels_[OCTAVE_COUNT_8 - 1 - i]);
    }
}

TEST(PerlinNoiseStaticTest, WrapSmallValues) {
    EXPECT_DOUBLE_EQ(minecraft::noise::PerlinNoise::wrap(0.5), 0.5);
    EXPECT_DOUBLE_EQ(minecraft::noise::PerlinNoise::wrap(-0.5), -0.5);
    EXPECT_DOUBLE_EQ(minecraft::noise::PerlinNoise::wrap(0.0), 0.0);
}

TEST(PerlinNoiseStaticTest, WrapLargeValues) {
    constexpr double large = 1.0e8;
    const double r = minecraft::noise::PerlinNoise::wrap(large);
    EXPECT_TRUE(std::isfinite(r));
    EXPECT_LT(std::abs(r), large);
}

// =============================================================================
// NormalNoise Unit Tests
// =============================================================================

class NormalNoiseTest : public testing::Test {
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
        // NormalNoise does NOT own PerlinNoise pointers
        first->_destroy();
        second->_destroy();
        for (const auto* c : cleaners) {
            c->_destroy();
        }
        cleaners.clear();
    }
};

TEST_F(NormalNoiseTest, CreateDestroy) {
    SUCCEED();
}

TEST_F(NormalNoiseTest, SetPerlinNoise) {
    EXPECT_EQ(noise->first, first);
    EXPECT_EQ(noise->second, second);
}

TEST_F(NormalNoiseTest, DeterministicGetValue) {
    const double r1 = noise->get_value(1.5, 2.5, 3.5);
    const double r2 = noise->get_value(1.5, 2.5, 3.5);
    EXPECT_DOUBLE_EQ(r1, r2);
}

TEST_F(NormalNoiseTest, NonZeroGetValue) {
    const double r = noise->get_value(1.5, 2.5, 3.5);
    EXPECT_NE(r, 0.0);
}

TEST_F(NormalNoiseTest, GetValueVariesWithInput) {
    const double r1 = noise->get_value(0.125, 0.25, 0.375);
    const double r2 = noise->get_value(10.125, 20.375, 30.625);
    EXPECT_NE(r1, r2);
}

TEST_F(NormalNoiseTest, MaxValue) {
    EXPECT_DOUBLE_EQ(noise->max_value(), 1.0);
}

TEST(NormalNoiseStaticTest, ExpectedDeviation) {
    const double ed = minecraft::noise::NormalNoise::expected_deviation(0);
    EXPECT_DOUBLE_EQ(ed, 0.2);
}

TEST(NormalNoiseStaticTest, ExpectedDeviationLargeSpan) {
    const double ed = minecraft::noise::NormalNoise::expected_deviation(7);
    EXPECT_DOUBLE_EQ(ed, 0.1125);
}

// =============================================================================
// SimplexNoise Unit Tests
// =============================================================================

class SimplexNoiseTest : public testing::Test {
protected:
    std::array<int, 256> perm = make_perm_int();
};

TEST_F(SimplexNoiseTest, CreateDestroy) {
    auto* s = minecraft::noise::SimplexNoise::_create(0.5, 0.25, 0.125, perm.data(), perm.size());
    ASSERT_NE(s, nullptr);
    EXPECT_DOUBLE_EQ(s->xo, 0.5);
    EXPECT_DOUBLE_EQ(s->yo, 0.25);
    EXPECT_DOUBLE_EQ(s->zo, 0.125);
    s->_destroy();
}

TEST_F(SimplexNoiseTest, DeterministicGetValue2) {
    const auto* s = minecraft::noise::SimplexNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    const double r1 = s->get_value(1.5, 2.5);
    const double r2 = s->get_value(1.5, 2.5);
    EXPECT_DOUBLE_EQ(r1, r2);
    s->_destroy();
}

TEST_F(SimplexNoiseTest, DeterministicGetValue3) {
    const auto* s = minecraft::noise::SimplexNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    const double r1 = s->get_value(1.5, 2.5, 3.5);
    const double r2 = s->get_value(1.5, 2.5, 3.5);
    EXPECT_DOUBLE_EQ(r1, r2);
    s->_destroy();
}

TEST_F(SimplexNoiseTest, NonZeroGetValue2) {
    const auto* s = minecraft::noise::SimplexNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    const double r = s->get_value(10.5, 20.3);
    EXPECT_NE(r, 0.0);
    s->_destroy();
}

TEST_F(SimplexNoiseTest, NonZeroGetValue3) {
    const auto* s = minecraft::noise::SimplexNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    const double r = s->get_value(10.5, 20.3, 30.7);
    EXPECT_NE(r, 0.0);
    s->_destroy();
}

TEST_F(SimplexNoiseTest, GetValue2DiffersFromGetValue3) {
    const auto* s = minecraft::noise::SimplexNoise::_create(0.0, 0.0, 0.0, perm.data(), perm.size());
    const double r2 = s->get_value(5.5, 6.5);
    const double r3 = s->get_value(5.5, 6.5, 0.5);
    EXPECT_NE(r2, r3);
    s->_destroy();
}

TEST_F(SimplexNoiseTest, Dot) {
    double grad[3]{2.0, -3.0, 1.0};
    double d = minecraft::noise::SimplexNoise::dot(grad, 1.5, 2.0, -0.5);
    EXPECT_DOUBLE_EQ(d, (2.0 * 1.5) + (-3.0 * 2.0) + (1.0 * -0.5));
}

// =============================================================================
// BlendedNoise Unit Tests
// =============================================================================

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

TEST_F(BlendedNoiseTest, CreateDestroy) {
    ASSERT_NE(blended, nullptr);
    EXPECT_EQ(blended->min_limit_noise, minLimit);
    EXPECT_EQ(blended->max_limit_noise, maxLimit);
    EXPECT_EQ(blended->main_noise, mainNoise);
    EXPECT_DOUBLE_EQ(blended->xz_scale, 0.25);
    EXPECT_DOUBLE_EQ(blended->y_scale, 0.125);
    EXPECT_DOUBLE_EQ(blended->xz_multiplier, 684.412 * 0.25);
    EXPECT_DOUBLE_EQ(blended->y_multiplier, 684.412 * 0.125);
}

TEST_F(BlendedNoiseTest, ComputeNonZero) {
    const double r = blended->compute(10.125, 20.375, 30.625);
    EXPECT_NE(r, 0.0);
}

TEST_F(BlendedNoiseTest, ComputeDeterministic) {
    const double r1 = blended->compute(1.5, 2.5, 3.5);
    const double r2 = blended->compute(1.5, 2.5, 3.5);
    EXPECT_DOUBLE_EQ(r1, r2);
}

TEST_F(BlendedNoiseTest, ComputeVariesWithInput) {
    const double r1 = blended->compute(0.125, 0.25, 0.375);
    const double r2 = blended->compute(10.125, 20.375, 30.625);
    EXPECT_NE(r1, r2);
}

TEST_F(BlendedNoiseTest, ComputeFiniteOutput) {
    const double r = blended->compute(100.0, 200.0, 300.0);
    EXPECT_TRUE(std::isfinite(r));
}

// =============================================================================
// Performance Benchmarks
// =============================================================================

#include <chrono>

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
