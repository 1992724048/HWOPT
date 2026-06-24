#include <gtest/gtest.h>

#include <chrono>
#include <print>
#include <random>
#include <vector>

#include "../hwopt/Minecraft/Entity/AABB.hpp"

using minecraft::aabb::AABB;

// ── helpers ──────────────────────────────────────────────────
static auto fillGrid(std::vector<double>& boxes, int bx, int by, int bz,
                     double ox, double oy, double oz) -> void {
    boxes.clear();
    for (int y = 0; y < by; ++y) {
        for (int z = 0; z < bz; ++z) {
            for (int x = 0; x < bx; ++x) {
                boxes.push_back(ox + x);                     // minX
                boxes.push_back(oy + y);                     // minY
                boxes.push_back(oz + z);                     // minZ
                boxes.push_back(ox + x + 1.0);               // maxX
                boxes.push_back(oy + y + 1.0);               // maxY
                boxes.push_back(oz + z + 1.0);               // maxZ
            }
        }
    }
}

// ── batch_collide_axis benchmarks ────────────────────────────
class CollideAxisBench : public testing::Test {
protected:
    std::vector<double> boxes;
};

TEST_F(CollideAxisBench, OpenField) {
    // 0 boxes — entity walking in open terrain
    fillGrid(boxes, 0, 0, 0, 0, 0, 0);
    constexpr int ITERATIONS = 1000000;
    double sum = 0;

    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        sum += AABB::batch_collide_axis(0, 0.0, 0.0, 0.0, 0.6, 1.8, 0.6, boxes.data(), static_cast<int>(boxes.size()), 0.3);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    std::println("CollideAxis (0 boxes / open field) x{}: {} ms  ({} M calls/s)",
                 ITERATIONS, ms, ITERATIONS * 1000.0 / ms / 1'000'000);
    ASSERT_EQ(sum, 0.0);
}

TEST_F(CollideAxisBench, OneBox) {
    // 1 box — entity near a single block
    fillGrid(boxes, 1, 1, 1, 2, 0, 2);
    constexpr int ITERATIONS = 500000;
    double sum = 0;

    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        sum += AABB::batch_collide_axis(0, 1.5 + i * 0.001, 0.0, 1.5, 2.1 + i * 0.001, 1.8, 2.1, boxes.data(), static_cast<int>(boxes.size()), 0.5);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    std::println("CollideAxis (1 box) x{}: {} ms  ({} M calls/s)",
                 ITERATIONS, ms, ITERATIONS * 1000.0 / ms / 1'000'000);
    ASSERT_NE(sum, 0.0);
}

TEST_F(CollideAxisBench, DenseCave) {
    // ~400 boxes — cave/tunnel full of blocks
    fillGrid(boxes, 7, 3, 7, -1, -1, -1);
    // remove center to create a tunnel
    for (int y = 0; y < 3; ++y) {
        for (int z = 2; z < 5; ++z) {
            for (int x = 2; x < 5; ++x) {
                const int idx = (y * 7 * 7 + z * 7 + x) * 6;
                boxes[idx] = boxes[idx + 3] = 0;  // zero-size = skipped
            }
        }
    }
    constexpr int ITERATIONS = 100000;
    double sum = 0;

    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        sum += AABB::batch_collide_axis(1, 1.0, 1.0, 1.0, 1.6, 2.8, 1.6, boxes.data(), static_cast<int>(boxes.size()), -0.3);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    std::println("CollideAxis (~{} boxes / cave) x{}: {} ms  ({} calls/s)",
                 boxes.size() / 6, ITERATIONS, ms,
                 ITERATIONS * 1000.0 / ms);
    ASSERT_NE(sum, 0.0);
}

TEST_F(CollideAxisBench, ForestFloor) {
    // ~200 boxes — forest floor with logs, leaves
    std::mt19937 rng(42);
    boxes.clear();
    for (int i = 0; i < 200; ++i) {
        double x = rng() % 20 - 5;
        double z = rng() % 20 - 5;
        double y = 0.0;
        // random slab/stair-like height
        const double h = 0.5 + (rng() % 4) * 0.25;
        boxes.push_back(x); boxes.push_back(y); boxes.push_back(z);
        boxes.push_back(x + 1.0); boxes.push_back(y + h); boxes.push_back(z + 1.0);
    }
    constexpr int ITERATIONS = 100000;
    double sum = 0;

    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        sum += AABB::batch_collide_axis(2, 0.0, 0.0, 0.0, 0.6, 1.8, 0.6, boxes.data(), static_cast<int>(boxes.size()), 0.4);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    std::println("CollideAxis ({} boxes / forest) x{}: {} ms  ({} calls/s)",
                 boxes.size() / 6, ITERATIONS, ms,
                 ITERATIONS * 1000.0 / ms);
    ASSERT_NE(sum, 0.0);
}

// ── batch_find_collisions benchmarks ──────────────────────────
class FindCollisionsBench : public testing::Test {
protected:
    std::vector<double> aabbs;
    std::vector<int> outA, outB;
};

TEST_F(FindCollisionsBench, TenEntities) {
    // 10 entities in a small pen
    aabbs.clear();
    for (int i = 0; i < 10; ++i) {
        double x = i * 0.3;
        double z = i * 0.2;
        aabbs.push_back(x); aabbs.push_back(0); aabbs.push_back(z);
        aabbs.push_back(x + 0.6); aabbs.push_back(1.8); aabbs.push_back(z + 0.6);
    }
    outA.assign(100, 0);
    outB.assign(100, 0);
    constexpr int ITERATIONS = 500000;

    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        AABB::batch_find_collisions(
            aabbs.data(), static_cast<int>(aabbs.size()),
            outA.data(), static_cast<int>(outA.size()),
            outB.data(), static_cast<int>(outB.size()),
            10, 100);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    std::println("FindCollisions (10 entities) x{}: {} ms  ({} calls/s)",
                 ITERATIONS, ms, ITERATIONS * 1000.0 / ms);
}

TEST_F(FindCollisionsBench, OneHundredEntities) {
    // 100 entities in a mob farm / village
    std::mt19937 rng(123);
    aabbs.clear();
    for (int i = 0; i < 100; ++i) {
        double x = (rng() % 200) * 0.1;
        double z = (rng() % 200) * 0.1;
        aabbs.push_back(x); aabbs.push_back(0); aabbs.push_back(z);
        aabbs.push_back(x + 0.6); aabbs.push_back(1.8); aabbs.push_back(z + 0.6);
    }
    constexpr int max = 5000;
    outA.assign(max, 0);
    outB.assign(max, 0);
    constexpr int ITERATIONS = 50000;

    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        AABB::batch_find_collisions(
            aabbs.data(), static_cast<int>(aabbs.size()),
            outA.data(), static_cast<int>(outA.size()),
            outB.data(), static_cast<int>(outB.size()),
            100, max);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    std::println("FindCollisions (100 entities) x{}: {} ms  ({} calls/s)",
                 ITERATIONS, ms, ITERATIONS * 1000.0 / ms);
}

TEST_F(FindCollisionsBench, OneThousandEntities) {
    // 1000 entities — extreme case
    std::mt19937 rng(456);
    aabbs.clear();
    for (int i = 0; i < 1000; ++i) {
        double x = (rng() % 400) * 0.1;
        double z = (rng() % 400) * 0.1;
        aabbs.push_back(x); aabbs.push_back(0); aabbs.push_back(z);
        aabbs.push_back(x + 0.6); aabbs.push_back(1.8); aabbs.push_back(z + 0.6);
    }
    constexpr int max = 50000;
    outA.assign(max, 0);
    outB.assign(max, 0);
    constexpr int ITERATIONS = 5000;

    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        AABB::batch_find_collisions(
            aabbs.data(), static_cast<int>(aabbs.size()),
            outA.data(), static_cast<int>(outA.size()),
            outB.data(), static_cast<int>(outB.size()),
            1000, max);
    }
    const auto end = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    std::println("FindCollisions (1000 entities) x{}: {} ms  ({} calls/s)",
                 ITERATIONS, ms, ITERATIONS * 1000.0 / ms);
}
