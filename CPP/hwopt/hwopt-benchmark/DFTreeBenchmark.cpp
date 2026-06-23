#include <gtest/gtest.h>

#include <chrono>
#include <cstdint>
#include <print>
#include <random>

#include "../hwopt/Minecraft/Chunk/DensityFunctionTree.hpp"
#include "../hwopt/Minecraft/Chunk/Noise/BlendedNoise.hpp"
#include "../hwopt/Minecraft/Chunk/Noise/ImprovedNoise.hpp"
#include "../hwopt/Minecraft/Chunk/Noise/NormalNoise.hpp"
#include "../hwopt/Minecraft/Chunk/Noise/PerlinNoise.hpp"

using namespace minecraft::dftree;

// ── noise helpers ──────────────────────────────────────────────
static constexpr std::array AMPLITUDES_8{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
static constexpr std::array AMPLITUDES_16{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
static constexpr int FIRST_OCTAVE = -8;
static constexpr int8_t PERM[]{151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96};
static constexpr int PERM_LEN = 11;

static auto make_noise(double seed, double xz, double y) -> minecraft::noise::NormalNoise* {
    auto* n = minecraft::noise::NormalNoise::_create(seed, seed);
    auto* first = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_8.data()), AMPLITUDES_8.size(), xz, y, 2.0);
    auto* second = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_8.data()), AMPLITUDES_8.size(), xz, y, 2.0);
    for (int i = 0; i < 8; ++i) {
        first->add_noise(i, minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, PERM, PERM_LEN));
        second->add_noise(i, minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, PERM, PERM_LEN));
    }
    n->set_perlin_noise(first, second);
    return n;
}

static auto make_blended() -> minecraft::noise::BlendedNoise* {
    auto* minL = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_16.data()), AMPLITUDES_16.size(), 1.0, 1.0, 2.0);
    auto* maxL = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_16.data()), AMPLITUDES_16.size(), 1.0, 1.0, 2.0);
    auto* main = minecraft::noise::PerlinNoise::_create(FIRST_OCTAVE, const_cast<double*>(AMPLITUDES_8.data()), AMPLITUDES_8.size(), 1.0, 1.0, 2.0);
    for (int i = 0; i < 16; ++i) {
        auto* im = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, PERM, PERM_LEN);
        auto* ia = minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, PERM, PERM_LEN);
        minL->add_noise(i, im);
        maxL->add_noise(i, ia);
        if (i < 8) {
            main->add_noise(i, minecraft::noise::ImprovedNoise::_create(1.0, 1.0, 1.0, PERM, PERM_LEN));
        }
    }
    return minecraft::noise::BlendedNoise::_create(minL, maxL, main, 0.25, 0.125, 1.0, 1.0, 0.5);
}

// ── tree builder ───────────────────────────────────────────────
// Node layout: [type, c0, c1, d0, d1]  (5 doubles per node)
// Types: CONSTANT=0, ADD=1, MUL=2, MIN=3, MAX=4, TRANSFORM=5,
//        RANGE_CHOICE=6, BLENDED_NOISE=7, NOISE=8, Y_GRADIENT=9

struct TreeBuilder {
    std::vector<double> nodes;
    std::vector<minecraft::noise::BlendedNoise*> bn_list;
    std::vector<minecraft::noise::NormalNoise*> noise_list;

    auto add(double t, double c0, double c1, double d0, double d1) -> int {
        int idx = static_cast<int>(nodes.size()) / 5;
        nodes.push_back(t);
        nodes.push_back(c0);
        nodes.push_back(c1);
        nodes.push_back(d0);
        nodes.push_back(d1);
        return idx;
    }

    auto bn(minecraft::noise::BlendedNoise* p) -> int {
        int id = static_cast<int>(bn_list.size());
        bn_list.push_back(p);
        return add(BLENDED_NOISE, -1, -1, id, 0);
    }

    auto noise(minecraft::noise::NormalNoise* p, double xzScale) -> int {
        int id = static_cast<int>(noise_list.size());
        noise_list.push_back(p);
        return add(NOISE, -1, -1, id, xzScale);
    }

    auto constant(double v) -> int {
        return add(CONSTANT, -1, -1, v, 0);
    }

    auto add_n(int a, int b) -> int {
        return add(ADD, a, b, 0, 0);
    }

    auto mul(int a, int b) -> int {
        return add(MUL, a, b, 0, 0);
    }

    auto min_n(int a, int b) -> int {
        return add(MIN, a, b, 0, 0);
    }

    auto max_n(int a, int b) -> int {
        return add(MAX, a, b, 0, 0);
    }

    auto transform(int child, double scale, double offset) -> int {
        return add(TRANSFORM, child, -1, scale, offset);
    }

    auto range_choice(int input, int in_range, int out_range, double lo, double hi) -> int {
        int idx = static_cast<int>(nodes.size()) / 5;
        // range_choice packs extra child into d1 slot
        nodes.push_back(RANGE_CHOICE);
        nodes.push_back(input);
        nodes.push_back(in_range);
        nodes.push_back(lo);
        nodes.push_back(hi);
        nodes.push_back(out_range);
        nodes.push_back(-1);
        nodes.push_back(0);
        nodes.push_back(0);
        return idx;
    }

    auto y_gradient(double fy, double ty, double fv, double tv) -> int {
        return add(Y_GRADIENT, fy, ty, fv, tv);
    }

    auto build() -> std::tuple<std::vector<double>, std::vector<long long>, std::vector<long long>> {
        std::vector<long long> bn_ptrs;
        for (auto* p : bn_list) {
            bn_ptrs.push_back(reinterpret_cast<long long>(p));
        }
        std::vector<long long> noise_ptrs;
        for (auto* p : noise_list) {
            noise_ptrs.push_back(reinterpret_cast<long long>(p));
        }
        return {std::move(nodes), std::move(bn_ptrs), std::move(noise_ptrs)};
    }
};

// ── benchmark fixture ──────────────────────────────────────────
class DFTreeBatchBenchmark : public testing::Test {
protected:
    static constexpr int CELL_W = 4;
    static constexpr int CELL_H = 8;
    static constexpr int CELLS_XZ = 12;
    static constexpr int CELLS_Y = 48;
    static constexpr int SX = (CELLS_XZ + 1) * CELL_W; // 52
    static constexpr int SY = CELLS_Y * CELL_H; // 384
    static constexpr int SZ = SX; // 52
    static constexpr int TOTAL = SX * SY * SZ; // 1,038,336

    std::vector<double> nodes;
    std::vector<long long> bn_ptrs;
    std::vector<long long> noise_ptrs;
    std::vector<double> output;
    minecraft::noise::BlendedNoise* blended{nullptr};
    minecraft::noise::NormalNoise* n1{nullptr};
    minecraft::noise::NormalNoise* n2{nullptr};

    auto SetUp() -> void override {
        blended = make_blended();
        n1 = make_noise(1.0, 1.0, 1.0);
        n2 = make_noise(2.0, 2.0, 1.0);

        // Build a tree similar to real overworld terrain:
        //   root = RANGE_CHOICE(
        //       BLENDED_NOISE(main_terrain),
        //       ADD(BLENDED_NOISE, detail),
        //       CONSTANT(-64),
        //       -0.1,  0.1)
        TreeBuilder tb;
        int bn0 = tb.bn(blended);
        int n_0 = tb.noise(n1, 1.0);
        int n_1 = tb.noise(n2, 1.5);
        int yg = tb.y_gradient(-64, 320, -1.0, 1.0);
        int detail = tb.add_n(n_0, n_1);
        int blended_y = tb.add_n(bn0, tb.transform(yg, 0.5, 0));
        int root = tb.range_choice(blended_y, detail, tb.constant(-64), -0.1, 0.1);

        auto [n, b, ns] = tb.build();
        nodes = std::move(n);
        bn_ptrs = std::move(b);
        noise_ptrs = std::move(ns);
        output.resize(TOTAL);
    }

    auto TearDown() -> void override {}
};

TEST_F(DFTreeBatchBenchmark, OverworldChunk) {
    constexpr int ITERATIONS = 30;

    std::vector<double> temp(TOTAL);

    auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        constexpr int MIN_Z = 0;
        constexpr int MIN_Y = -64;
        constexpr int MIN_X = 0;
        DensityFunctionTree::compute_densities_batch(nodes.data(),
                                                     static_cast<int>(nodes.size()),
                                                     bn_ptrs.data(),
                                                     static_cast<int>(bn_ptrs.size()),
                                                     noise_ptrs.data(),
                                                     static_cast<int>(noise_ptrs.size()),
                                                     MIN_X,
                                                     MIN_Y,
                                                     MIN_Z,
                                                     SX,
                                                     SY,
                                                     SZ,
                                                     temp.data(),
                                                     static_cast<int>(temp.size()));
    }
    auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();

    // sum output to prevent optimizer from eliding
    double sum = 0;
    for (auto v : temp) {
        sum += v;
    }

    std::println("\n===== DensityFunctionTree::compute_densities_batch =====");
    std::println("  Tree nodes : {}", nodes.size() / 5);
    std::println("  Dimensions  : {} x {} x {} = {} pts/chunk", SX, SY, SZ, TOTAL);
    std::println("  Iterations : {}", ITERATIONS);
    std::println("  Total time : {} ms", ms);
    std::println("  Avg/chunk  : {:.3f} ms", static_cast<double>(ms) / ITERATIONS);
    std::println("  Throughput : {:.0f} chunks/s", ITERATIONS * 1000.0 / ms);
    std::println("  Throughput : {:.0f} Mpts/s", TOTAL * ITERATIONS * 1000.0 / ms / 1'000'000);
    std::println("  Sanity sum : {:.2f}", sum);
    ASSERT_NE(sum, 0.0);
}

TEST_F(DFTreeBatchBenchmark, SingleChunkWarm) {
    // Measure single-chunk time (cached TBB state)
    constexpr int MIN_X = 0;
    constexpr int MIN_Y = -64;
    constexpr int MIN_Z = 0;

    // warmup
    DensityFunctionTree::compute_densities_batch(nodes.data(),
                                                 static_cast<int>(nodes.size()),
                                                 bn_ptrs.data(),
                                                 static_cast<int>(bn_ptrs.size()),
                                                 noise_ptrs.data(),
                                                 static_cast<int>(noise_ptrs.size()),
                                                 MIN_X,
                                                 MIN_Y,
                                                 MIN_Z,
                                                 SX,
                                                 SY,
                                                 SZ,
                                                 output.data(),
                                                 static_cast<int>(output.size()));

    auto start = std::chrono::high_resolution_clock::now();
    DensityFunctionTree::compute_densities_batch(nodes.data(),
                                                 static_cast<int>(nodes.size()),
                                                 bn_ptrs.data(),
                                                 static_cast<int>(bn_ptrs.size()),
                                                 noise_ptrs.data(),
                                                 static_cast<int>(noise_ptrs.size()),
                                                 MIN_X,
                                                 MIN_Y,
                                                 MIN_Z,
                                                 SX,
                                                 SY,
                                                 SZ,
                                                 output.data(),
                                                 static_cast<int>(output.size()));
    auto end = std::chrono::high_resolution_clock::now();
    auto us = std::chrono::duration_cast<std::chrono::microseconds>(end - start).count();

    double sum = 0;
    for (auto v : output) {
        sum += v;
    }

    std::println("\n===== Single-chunk latency =====");
    std::println("  Time      : {} μs", us);
    std::println("  Sanity sum: {:.2f}", sum);
    ASSERT_NE(sum, 0.0);
}

TEST_F(DFTreeBatchBenchmark, SubBatch) {
    // sub-chunk batch: 16 cells height (128 blocks)
    constexpr int SY16 = 16 * CELL_H;
    constexpr int TOT16 = SX * SY16 * SZ;
    constexpr int ITERATIONS = 50;

    constexpr int MIN_X = 0;
    constexpr int MIN_Y = -64;
    constexpr int MIN_Z = 0;

    std::vector<double> temp(TOT16);
    auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        DensityFunctionTree::compute_densities_batch(nodes.data(),
                                                     static_cast<int>(nodes.size()),
                                                     bn_ptrs.data(),
                                                     static_cast<int>(bn_ptrs.size()),
                                                     noise_ptrs.data(),
                                                     static_cast<int>(noise_ptrs.size()),
                                                     MIN_X,
                                                     MIN_Y,
                                                     MIN_Z,
                                                     SX,
                                                     SY16,
                                                     SZ,
                                                     temp.data(),
                                                     static_cast<int>(temp.size()));
    }
    auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();

    double sum = 0;
    for (auto v : temp) {
        sum += v;
    }

    std::println("\n===== DensityFunctionTree (16 cells Y) =====");
    std::println("  Dimensions  : {} x {} x {} = {} pts", SX, SY16, SZ, TOT16);
    std::println("  Iterations : {}", ITERATIONS);
    std::println("  Avg/batch  : {:.3f} ms", static_cast<double>(ms) / ITERATIONS);
    std::println("  Sanity sum : {:.2f}", sum);
    ASSERT_NE(sum, 0.0);
}
