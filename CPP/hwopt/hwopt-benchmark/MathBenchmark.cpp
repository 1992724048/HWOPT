#include <gtest/gtest.h>

#include <chrono>
#include <cstring>
#include <print>
#include <vector>

#include "../hwopt/Global.hpp"
#include "../hwopt/Minecraft/Math.hpp"
#include "../hwopt/Minecraft/Entity/AABB.hpp"

#include "../hwopt-sycl/sycl-plugin.h"

#include "Minecraft/Math.hpp"

class MathBenchmark : public testing::Test {
protected:
    std::vector<double> output;

    auto SetUp() -> void override {
        if (hwopt::global::handle.id == 0) {
            auto dev = sycl::Device::create_device();
            if (dev.has_value()) {
                hwopt::global::handle = std::move(*dev);
            }
        }
    }
};

TEST_F(MathBenchmark, BatchTrilerp_32) {
    init_sycl_device();
    constexpr int CELL_W = 32;
    constexpr int CELL_H = 32;
    constexpr int ITERATIONS = 10000;

    constexpr auto total = static_cast<size_t>(CELL_W) * CELL_W * CELL_H;
    output.assign(total, 0.0);

    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        constexpr double n111 = 0.8;
        constexpr double n011 = 0.7;
        constexpr double n101 = 0.6;
        constexpr double n001 = 0.5;
        constexpr double n110 = 0.4;
        constexpr double n010 = 0.3;
        constexpr double n100 = 0.2;
        constexpr double n000 = 0.1;
        minecraft::math::Math::batch_trilerp(n000, n100, n010, n110, n001, n101, n011, n111, CELL_W, CELL_H, output.data(), total);
    }
    const auto end = std::chrono::high_resolution_clock::now();

    const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    const auto per_sec = static_cast<double>(ITERATIONS) / (static_cast<double>(ms) / 1000.0);

    double sum = 0;
    for (auto v : output) {
        sum += v;
    }
    ASSERT_NE(sum, 0.0);

    std::println("batch_trilerp({}x{}x{}) x{}: {} ms ({} 次/s)", CELL_W, CELL_H, CELL_W, ITERATIONS, ms, per_sec);
}

/*
 *
 *constexpr uint32_t trilerp_f32_spv[] = {
    0x07230203,
    0x00010000,
    0x0008000b,
    0x000000a3,
    0x00000000,
    0x00020011,
    0x00000001,
    0x0006000b,
    0x00000001,
    0x4c534c47,
    0x6474732e,
    0x3035342e,
    0x00000000,
    0x0003000e,
    0x00000000,
    0x00000001,
    0x0006000f,
    0x00000005,
    0x00000004,
    0x6e69616d,
    0x00000000,
    0x0000000b,
    0x00060010,
    0x00000004,
    0x00000011,
    0x00000001,
    0x00000001,
    0x00000001,
    0x00030003,
    0x00000002,
    0x000001c2,
    0x00040005,
    0x00000004,
    0x6e69616d,
    0x00000000,
    0x00030005,
    0x00000008,
    0x00007969,
    0x00080005,
    0x0000000b,
    0x475f6c67,
    0x61626f6c,
    0x766e496c,
    0x7461636f,
    0x496e6f69,
    0x00000044,
    0x00030005,
    0x00000010,
    0x00007869,
    0x00030005,
    0x00000014,
    0x00007a69,
    0x00030005,
    0x0000001a,
    0x00007964,
    0x00040005,
    0x0000001e,
    0x61726150,
    0x0000736d,
    0x00050006,
    0x0000001e,
    0x00000000,
    0x3030306e,
    0x00000000,
    0x00050006,
    0x0000001e,
    0x00000001,
    0x3030316e,
    0x00000000,
    0x00050006,
    0x0000001e,
    0x00000002,
    0x3031306e,
    0x00000000,
    0x00050006,
    0x0000001e,
    0x00000003,
    0x3031316e,
    0x00000000,
    0x00050006,
    0x0000001e,
    0x00000004,
    0x3130306e,
    0x00000000,
    0x00050006,
    0x0000001e,
    0x00000005,
    0x3130316e,
    0x00000000,
    0x00050006,
    0x0000001e,
    0x00000006,
    0x3131306e,
    0x00000000,
    0x00050006,
    0x0000001e,
    0x00000007,
    0x3131316e,
    0x00000000,
    0x00050006,
    0x0000001e,
    0x00000008,
    0x6c6c6563,
    0x0000775f,
    0x00050006,
    0x0000001e,
    0x00000009,
    0x6c6c6563,
    0x0000685f,
    0x00030005,
    0x00000020,
    0x00000070,
    0x00030005,
    0x00000027,
    0x0000796c,
    0x00030005,
    0x0000002b,
    0x00007968,
    0x00030005,
    0x0000002d,
    0x00303076,
    0x00030005,
    0x0000003a,
    0x00303176,
    0x00030005,
    0x00000046,
    0x00313076,
    0x00030005,
    0x00000052,
    0x00313176,
    0x00030005,
    0x0000005e,
    0x00007864,
    0x00030005,
    0x00000066,
    0x0000786c,
    0x00030005,
    0x00000069,
    0x00007868,
    0x00030005,
    0x0000006b,
    0x0000307a,
    0x00030005,
    0x00000073,
    0x0000317a,
    0x00030005,
    0x0000007b,
    0x00007a64,
    0x00030005,
    0x00000082,
    0x00786469,
    0x00040005,
    0x00000095,
    0x4274754f,
    0x00006675,
    0x00050006,
    0x00000095,
    0x00000000,
    0x61746164,
    0x00000000,
    0x00030005,
    0x00000097,
    0x00000000,
    0x00040047,
    0x0000000b,
    0x0000000b,
    0x0000001c,
    0x00030047,
    0x0000001e,
    0x00000002,
    0x00050048,
    0x0000001e,
    0x00000000,
    0x00000023,
    0x00000000,
    0x00050048,
    0x0000001e,
    0x00000001,
    0x00000023,
    0x00000004,
    0x00050048,
    0x0000001e,
    0x00000002,
    0x00000023,
    0x00000008,
    0x00050048,
    0x0000001e,
    0x00000003,
    0x00000023,
    0x0000000c,
    0x00050048,
    0x0000001e,
    0x00000004,
    0x00000023,
    0x00000010,
    0x00050048,
    0x0000001e,
    0x00000005,
    0x00000023,
    0x00000014,
    0x00050048,
    0x0000001e,
    0x00000006,
    0x00000023,
    0x00000018,
    0x00050048,
    0x0000001e,
    0x00000007,
    0x00000023,
    0x0000001c,
    0x00050048,
    0x0000001e,
    0x00000008,
    0x00000023,
    0x00000020,
    0x00050048,
    0x0000001e,
    0x00000009,
    0x00000023,
    0x00000024,
    0x00040047,
    0x00000094,
    0x00000006,
    0x00000004,
    0x00030047,
    0x00000095,
    0x00000003,
    0x00050048,
    0x00000095,
    0x00000000,
    0x00000023,
    0x00000000,
    0x00040047,
    0x00000097,
    0x00000021,
    0x00000000,
    0x00040047,
    0x00000097,
    0x00000022,
    0x00000000,
    0x00040047,
    0x000000a2,
    0x0000000b,
    0x00000019,
    0x00020013,
    0x00000002,
    0x00030021,
    0x00000003,
    0x00000002,
    0x00040015,
    0x00000006,
    0x00000020,
    0x00000000,
    0x00040020,
    0x00000007,
    0x00000007,
    0x00000006,
    0x00040017,
    0x00000009,
    0x00000006,
    0x00000003,
    0x00040020,
    0x0000000a,
    0x00000001,
    0x00000009,
    0x0004003b,
    0x0000000a,
    0x0000000b,
    0x00000001,
    0x0004002b,
    0x00000006,
    0x0000000c,
    0x00000001,
    0x00040020,
    0x0000000d,
    0x00000001,
    0x00000006,
    0x0004002b,
    0x00000006,
    0x00000011,
    0x00000000,
    0x0004002b,
    0x00000006,
    0x00000015,
    0x00000002,
    0x00030016,
    0x00000018,
    0x00000020,
    0x00040020,
    0x00000019,
    0x00000007,
    0x00000018,
    0x00040015,
    0x0000001d,
    0x00000020,
    0x00000001,
    0x000c001e,
    0x0000001e,
    0x00000018,
    0x00000018,
    0x00000018,
    0x00000018,
    0x00000018,
    0x00000018,
    0x00000018,
    0x00000018,
    0x0000001d,
    0x0000001d,
    0x00040020,
    0x0000001f,
    0x00000009,
    0x0000001e,
    0x0004003b,
    0x0000001f,
    0x00000020,
    0x00000009,
    0x0004002b,
    0x0000001d,
    0x00000021,
    0x00000009,
    0x00040020,
    0x00000022,
    0x00000009,
    0x0000001d,
    0x0004002b,
    0x00000018,
    0x00000028,
    0x3f800000,
    0x0004002b,
    0x0000001d,
    0x0000002e,
    0x00000000,
    0x00040020,
    0x0000002f,
    0x00000009,
    0x00000018,
    0x0004002b,
    0x0000001d,
    0x00000034,
    0x00000002,
    0x0004002b,
    0x0000001d,
    0x0000003b,
    0x00000001,
    0x0004002b,
    0x0000001d,
    0x00000040,
    0x00000003,
    0x0004002b,
    0x0000001d,
    0x00000047,
    0x00000004,
    0x0004002b,
    0x0000001d,
    0x0000004c,
    0x00000006,
    0x0004002b,
    0x0000001d,
    0x00000053,
    0x00000005,
    0x0004002b,
    0x0000001d,
    0x00000058,
    0x00000007,
    0x0004002b,
    0x0000001d,
    0x00000061,
    0x00000008,
    0x0003001d,
    0x00000094,
    0x00000018,
    0x0003001e,
    0x00000095,
    0x00000094,
    0x00040020,
    0x00000096,
    0x00000002,
    0x00000095,
    0x0004003b,
    0x00000096,
    0x00000097,
    0x00000002,
    0x00040020,
    0x000000a0,
    0x00000002,
    0x00000018,
    0x0006002c,
    0x00000009,
    0x000000a2,
    0x0000000c,
    0x0000000c,
    0x0000000c,
    0x00050036,
    0x00000002,
    0x00000004,
    0x00000000,
    0x00000003,
    0x000200f8,
    0x00000005,
    0x0004003b,
    0x00000007,
    0x00000008,
    0x00000007,
    0x0004003b,
    0x00000007,
    0x00000010,
    0x00000007,
    0x0004003b,
    0x00000007,
    0x00000014,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x0000001a,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x00000027,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x0000002b,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x0000002d,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x0000003a,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x00000046,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x00000052,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x0000005e,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x00000066,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x00000069,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x0000006b,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x00000073,
    0x00000007,
    0x0004003b,
    0x00000019,
    0x0000007b,
    0x00000007,
    0x0004003b,
    0x00000007,
    0x00000082,
    0x00000007,
    0x00050041,
    0x0000000d,
    0x0000000e,
    0x0000000b,
    0x0000000c,
    0x0004003d,
    0x00000006,
    0x0000000f,
    0x0000000e,
    0x0003003e,
    0x00000008,
    0x0000000f,
    0x00050041,
    0x0000000d,
    0x00000012,
    0x0000000b,
    0x00000011,
    0x0004003d,
    0x00000006,
    0x00000013,
    0x00000012,
    0x0003003e,
    0x00000010,
    0x00000013,
    0x00050041,
    0x0000000d,
    0x00000016,
    0x0000000b,
    0x00000015,
    0x0004003d,
    0x00000006,
    0x00000017,
    0x00000016,
    0x0003003e,
    0x00000014,
    0x00000017,
    0x0004003d,
    0x00000006,
    0x0000001b,
    0x00000008,
    0x00040070,
    0x00000018,
    0x0000001c,
    0x0000001b,
    0x00050041,
    0x00000022,
    0x00000023,
    0x00000020,
    0x00000021,
    0x0004003d,
    0x0000001d,
    0x00000024,
    0x00000023,
    0x0004006f,
    0x00000018,
    0x00000025,
    0x00000024,
    0x00050088,
    0x00000018,
    0x00000026,
    0x0000001c,
    0x00000025,
    0x0003003e,
    0x0000001a,
    0x00000026,
    0x0004003d,
    0x00000018,
    0x00000029,
    0x0000001a,
    0x00050083,
    0x00000018,
    0x0000002a,
    0x00000028,
    0x00000029,
    0x0003003e,
    0x00000027,
    0x0000002a,
    0x0004003d,
    0x00000018,
    0x0000002c,
    0x0000001a,
    0x0003003e,
    0x0000002b,
    0x0000002c,
    0x00050041,
    0x0000002f,
    0x00000030,
    0x00000020,
    0x0000002e,
    0x0004003d,
    0x00000018,
    0x00000031,
    0x00000030,
    0x0004003d,
    0x00000018,
    0x00000032,
    0x00000027,
    0x00050085,
    0x00000018,
    0x00000033,
    0x00000031,
    0x00000032,
    0x00050041,
    0x0000002f,
    0x00000035,
    0x00000020,
    0x00000034,
    0x0004003d,
    0x00000018,
    0x00000036,
    0x00000035,
    0x0004003d,
    0x00000018,
    0x00000037,
    0x0000002b,
    0x00050085,
    0x00000018,
    0x00000038,
    0x00000036,
    0x00000037,
    0x00050081,
    0x00000018,
    0x00000039,
    0x00000033,
    0x00000038,
    0x0003003e,
    0x0000002d,
    0x00000039,
    0x00050041,
    0x0000002f,
    0x0000003c,
    0x00000020,
    0x0000003b,
    0x0004003d,
    0x00000018,
    0x0000003d,
    0x0000003c,
    0x0004003d,
    0x00000018,
    0x0000003e,
    0x00000027,
    0x00050085,
    0x00000018,
    0x0000003f,
    0x0000003d,
    0x0000003e,
    0x00050041,
    0x0000002f,
    0x00000041,
    0x00000020,
    0x00000040,
    0x0004003d,
    0x00000018,
    0x00000042,
    0x00000041,
    0x0004003d,
    0x00000018,
    0x00000043,
    0x0000002b,
    0x00050085,
    0x00000018,
    0x00000044,
    0x00000042,
    0x00000043,
    0x00050081,
    0x00000018,
    0x00000045,
    0x0000003f,
    0x00000044,
    0x0003003e,
    0x0000003a,
    0x00000045,
    0x00050041,
    0x0000002f,
    0x00000048,
    0x00000020,
    0x00000047,
    0x0004003d,
    0x00000018,
    0x00000049,
    0x00000048,
    0x0004003d,
    0x00000018,
    0x0000004a,
    0x00000027,
    0x00050085,
    0x00000018,
    0x0000004b,
    0x00000049,
    0x0000004a,
    0x00050041,
    0x0000002f,
    0x0000004d,
    0x00000020,
    0x0000004c,
    0x0004003d,
    0x00000018,
    0x0000004e,
    0x0000004d,
    0x0004003d,
    0x00000018,
    0x0000004f,
    0x0000002b,
    0x00050085,
    0x00000018,
    0x00000050,
    0x0000004e,
    0x0000004f,
    0x00050081,
    0x00000018,
    0x00000051,
    0x0000004b,
    0x00000050,
    0x0003003e,
    0x00000046,
    0x00000051,
    0x00050041,
    0x0000002f,
    0x00000054,
    0x00000020,
    0x00000053,
    0x0004003d,
    0x00000018,
    0x00000055,
    0x00000054,
    0x0004003d,
    0x00000018,
    0x00000056,
    0x00000027,
    0x00050085,
    0x00000018,
    0x00000057,
    0x00000055,
    0x00000056,
    0x00050041,
    0x0000002f,
    0x00000059,
    0x00000020,
    0x00000058,
    0x0004003d,
    0x00000018,
    0x0000005a,
    0x00000059,
    0x0004003d,
    0x00000018,
    0x0000005b,
    0x0000002b,
    0x00050085,
    0x00000018,
    0x0000005c,
    0x0000005a,
    0x0000005b,
    0x00050081,
    0x00000018,
    0x0000005d,
    0x00000057,
    0x0000005c,
    0x0003003e,
    0x00000052,
    0x0000005d,
    0x0004003d,
    0x00000006,
    0x0000005f,
    0x00000010,
    0x00040070,
    0x00000018,
    0x00000060,
    0x0000005f,
    0x00050041,
    0x00000022,
    0x00000062,
    0x00000020,
    0x00000061,
    0x0004003d,
    0x0000001d,
    0x00000063,
    0x00000062,
    0x0004006f,
    0x00000018,
    0x00000064,
    0x00000063,
    0x00050088,
    0x00000018,
    0x00000065,
    0x00000060,
    0x00000064,
    0x0003003e,
    0x0000005e,
    0x00000065,
    0x0004003d,
    0x00000018,
    0x00000067,
    0x0000005e,
    0x00050083,
    0x00000018,
    0x00000068,
    0x00000028,
    0x00000067,
    0x0003003e,
    0x00000066,
    0x00000068,
    0x0004003d,
    0x00000018,
    0x0000006a,
    0x0000005e,
    0x0003003e,
    0x00000069,
    0x0000006a,
    0x0004003d,
    0x00000018,
    0x0000006c,
    0x0000002d,
    0x0004003d,
    0x00000018,
    0x0000006d,
    0x00000066,
    0x00050085,
    0x00000018,
    0x0000006e,
    0x0000006c,
    0x0000006d,
    0x0004003d,
    0x00000018,
    0x0000006f,
    0x0000003a,
    0x0004003d,
    0x00000018,
    0x00000070,
    0x00000069,
    0x00050085,
    0x00000018,
    0x00000071,
    0x0000006f,
    0x00000070,
    0x00050081,
    0x00000018,
    0x00000072,
    0x0000006e,
    0x00000071,
    0x0003003e,
    0x0000006b,
    0x00000072,
    0x0004003d,
    0x00000018,
    0x00000074,
    0x00000046,
    0x0004003d,
    0x00000018,
    0x00000075,
    0x00000066,
    0x00050085,
    0x00000018,
    0x00000076,
    0x00000074,
    0x00000075,
    0x0004003d,
    0x00000018,
    0x00000077,
    0x00000052,
    0x0004003d,
    0x00000018,
    0x00000078,
    0x00000069,
    0x00050085,
    0x00000018,
    0x00000079,
    0x00000077,
    0x00000078,
    0x00050081,
    0x00000018,
    0x0000007a,
    0x00000076,
    0x00000079,
    0x0003003e,
    0x00000073,
    0x0000007a,
    0x0004003d,
    0x00000006,
    0x0000007c,
    0x00000014,
    0x00040070,
    0x00000018,
    0x0000007d,
    0x0000007c,
    0x00050041,
    0x00000022,
    0x0000007e,
    0x00000020,
    0x00000061,
    0x0004003d,
    0x0000001d,
    0x0000007f,
    0x0000007e,
    0x0004006f,
    0x00000018,
    0x00000080,
    0x0000007f,
    0x00050088,
    0x00000018,
    0x00000081,
    0x0000007d,
    0x00000080,
    0x0003003e,
    0x0000007b,
    0x00000081,
    0x0004003d,
    0x00000006,
    0x00000083,
    0x00000008,
    0x00050041,
    0x00000022,
    0x00000084,
    0x00000020,
    0x00000061,
    0x0004003d,
    0x0000001d,
    0x00000085,
    0x00000084,
    0x0004007c,
    0x00000006,
    0x00000086,
    0x00000085,
    0x00050084,
    0x00000006,
    0x00000087,
    0x00000083,
    0x00000086,
    0x00050041,
    0x00000022,
    0x00000088,
    0x00000020,
    0x00000061,
    0x0004003d,
    0x0000001d,
    0x00000089,
    0x00000088,
    0x0004007c,
    0x00000006,
    0x0000008a,
    0x00000089,
    0x00050084,
    0x00000006,
    0x0000008b,
    0x00000087,
    0x0000008a,
    0x0004003d,
    0x00000006,
    0x0000008c,
    0x00000010,
    0x00050041,
    0x00000022,
    0x0000008d,
    0x00000020,
    0x00000061,
    0x0004003d,
    0x0000001d,
    0x0000008e,
    0x0000008d,
    0x0004007c,
    0x00000006,
    0x0000008f,
    0x0000008e,
    0x00050084,
    0x00000006,
    0x00000090,
    0x0000008c,
    0x0000008f,
    0x00050080,
    0x00000006,
    0x00000091,
    0x0000008b,
    0x00000090,
    0x0004003d,
    0x00000006,
    0x00000092,
    0x00000014,
    0x00050080,
    0x00000006,
    0x00000093,
    0x00000091,
    0x00000092,
    0x0003003e,
    0x00000082,
    0x00000093,
    0x0004003d,
    0x00000006,
    0x00000098,
    0x00000082,
    0x0004003d,
    0x00000018,
    0x00000099,
    0x0000006b,
    0x0004003d,
    0x00000018,
    0x0000009a,
    0x0000007b,
    0x0004003d,
    0x00000018,
    0x0000009b,
    0x00000073,
    0x0004003d,
    0x00000018,
    0x0000009c,
    0x0000006b,
    0x00050083,
    0x00000018,
    0x0000009d,
    0x0000009b,
    0x0000009c,
    0x00050085,
    0x00000018,
    0x0000009e,
    0x0000009a,
    0x0000009d,
    0x00050081,
    0x00000018,
    0x0000009f,
    0x00000099,
    0x0000009e,
    0x00060041,
    0x000000a0,
    0x000000a1,
    0x00000097,
    0x0000002e,
    0x00000098,
    0x0003003e,
    0x000000a1,
    0x0000009f,
    0x000100fd,
    0x00010038
};

TEST_F(MathBenchmark, VulkanTrilerpF32_16) {
    constexpr int CELL_W = 32, CELL_H = 32, ITERATIONS = 10000;
    constexpr float n000 = 0.1f, n100 = 0.2f, n010 = 0.3f, n110 = 0.4f;
    constexpr float n001 = 0.5f, n101 = 0.6f, n011 = 0.7f, n111 = 0.8f;

    VkInstance instance;
    VkApplicationInfo app_info{};
    app_info.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app_info.apiVersion = VK_API_VERSION_1_3;
    VkInstanceCreateInfo inst_ci{};
    inst_ci.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    inst_ci.pApplicationInfo = &app_info;
    VK_OK(vkCreateInstance(&inst_ci, nullptr, &instance));

    uint32_t pd_count = 0;
    VK_OK(vkEnumeratePhysicalDevices(instance, &pd_count, nullptr));
    ASSERT_GT(pd_count, 0u);
    std::vector<VkPhysicalDevice> pds(pd_count);
    VK_OK(vkEnumeratePhysicalDevices(instance, &pd_count, pds.data()));
    VkPhysicalDevice pd = VK_NULL_HANDLE;
    for (auto d : pds) {
        VkPhysicalDeviceProperties props;
        vkGetPhysicalDeviceProperties(d, &props);
        if (props.deviceType == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
            pd = d;
            break;
        }
    }
    if (pd == VK_NULL_HANDLE) {
        for (auto d : pds) {
            VkPhysicalDeviceProperties props;
            vkGetPhysicalDeviceProperties(d, &props);
            if (props.deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                pd = d;
                break;
            }
        }
    }
    if (pd == VK_NULL_HANDLE) {
        pd = pds[0];
    }
    VkPhysicalDeviceProperties sel_props;
    vkGetPhysicalDeviceProperties(pd, &sel_props);
    std::println("Vulkan f32 设备: {} ({})",
                 sel_props.deviceName,
                 sel_props.deviceType == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU ? "核显" : sel_props.deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU ? "独显" : "其他");

    uint32_t qf_count = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(pd, &qf_count, nullptr);
    std::vector<VkQueueFamilyProperties> qf_props(qf_count);
    vkGetPhysicalDeviceQueueFamilyProperties(pd, &qf_count, qf_props.data());
    uint32_t qf_idx = UINT32_MAX;
    for (uint32_t i = 0; i < qf_count; ++i) {
        if (qf_props[i].queueFlags & VK_QUEUE_COMPUTE_BIT) {
            qf_idx = i;
            break;
        }
    }
    ASSERT_NE(qf_idx, UINT32_MAX);

    float q_prior = 1.0f;
    VkDeviceQueueCreateInfo q_ci{};
    q_ci.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    q_ci.queueFamilyIndex = qf_idx;
    q_ci.queueCount = 1;
    q_ci.pQueuePriorities = &q_prior;
    VkDeviceCreateInfo dev_ci{};
    dev_ci.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    dev_ci.queueCreateInfoCount = 1;
    dev_ci.pQueueCreateInfos = &q_ci;
    VkDevice device;
    VK_OK(vkCreateDevice(pd, &dev_ci, nullptr, &device));
    VkQueue queue;
    vkGetDeviceQueue(device, qf_idx, 0, &queue);

    VkShaderModuleCreateInfo sm_ci{};
    sm_ci.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    sm_ci.codeSize = sizeof(trilerp_f32_spv);
    sm_ci.pCode = trilerp_f32_spv;
    VkShaderModule shader;
    VK_OK(vkCreateShaderModule(device, &sm_ci, nullptr, &shader));

    VkDescriptorSetLayoutBinding bind{};
    bind.binding = 0;
    bind.descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    bind.descriptorCount = 1;
    bind.stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
    VkDescriptorSetLayoutCreateInfo ds_ci{};
    ds_ci.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    ds_ci.bindingCount = 1;
    ds_ci.pBindings = &bind;
    VkDescriptorSetLayout ds_layout;
    VK_OK(vkCreateDescriptorSetLayout(device, &ds_ci, nullptr, &ds_layout));

    VkPushConstantRange pc_range{};
    pc_range.stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
    pc_range.offset = 0;
    pc_range.size = 40;
    VkPipelineLayoutCreateInfo pl_ci{};
    pl_ci.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    pl_ci.setLayoutCount = 1;
    pl_ci.pSetLayouts = &ds_layout;
    pl_ci.pushConstantRangeCount = 1;
    pl_ci.pPushConstantRanges = &pc_range;
    VkPipelineLayout pipeline_layout;
    VK_OK(vkCreatePipelineLayout(device, &pl_ci, nullptr, &pipeline_layout));

    VkComputePipelineCreateInfo cp_ci{};
    cp_ci.sType = VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO;
    cp_ci.stage.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    cp_ci.stage.stage = VK_SHADER_STAGE_COMPUTE_BIT;
    cp_ci.stage.module = shader;
    cp_ci.stage.pName = "main";
    cp_ci.layout = pipeline_layout;
    VkPipeline pipeline;
    VK_OK(vkCreateComputePipelines(device, VK_NULL_HANDLE, 1, &cp_ci, nullptr, &pipeline));

    constexpr auto total = static_cast<VkDeviceSize>(CELL_W) * CELL_W * CELL_H * sizeof(float);
    VkBufferCreateInfo buf_ci{};
    buf_ci.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    buf_ci.size = total;
    buf_ci.usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
    VkBuffer buffer;
    VK_OK(vkCreateBuffer(device, &buf_ci, nullptr, &buffer));
    VkMemoryRequirements mem_req;
    vkGetBufferMemoryRequirements(device, buffer, &mem_req);
    VkPhysicalDeviceMemoryProperties mem_props;
    vkGetPhysicalDeviceMemoryProperties(pd, &mem_props);
    uint32_t mem_type = UINT32_MAX;
    for (uint32_t i = 0; i < mem_props.memoryTypeCount; ++i) {
        if ((mem_req.memoryTypeBits & (1u << i)) && (mem_props.memoryTypes[i].propertyFlags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) && (mem_props.memoryTypes[i].propertyFlags &
            VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)) {
            mem_type = i;
            break;
        }
    }
    ASSERT_NE(mem_type, UINT32_MAX);
    VkMemoryAllocateInfo alloc_info{};
    alloc_info.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    alloc_info.allocationSize = mem_req.size;
    alloc_info.memoryTypeIndex = mem_type;
    VkDeviceMemory buf_mem;
    VK_OK(vkAllocateMemory(device, &alloc_info, nullptr, &buf_mem));
    VK_OK(vkBindBufferMemory(device, buffer, buf_mem, 0));

    VkDescriptorPoolSize pool_size{};
    pool_size.type = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    pool_size.descriptorCount = 1;
    VkDescriptorPoolCreateInfo dp_ci{};
    dp_ci.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    dp_ci.maxSets = 1;
    dp_ci.poolSizeCount = 1;
    dp_ci.pPoolSizes = &pool_size;
    VkDescriptorPool desc_pool;
    VK_OK(vkCreateDescriptorPool(device, &dp_ci, nullptr, &desc_pool));
    VkDescriptorSetAllocateInfo ds_alloc{};
    ds_alloc.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
    ds_alloc.descriptorPool = desc_pool;
    ds_alloc.descriptorSetCount = 1;
    ds_alloc.pSetLayouts = &ds_layout;
    VkDescriptorSet desc_set;
    VK_OK(vkAllocateDescriptorSets(device, &ds_alloc, &desc_set));
    VkDescriptorBufferInfo buf_info{};
    buf_info.buffer = buffer;
    buf_info.range = VK_WHOLE_SIZE;
    VkWriteDescriptorSet write_ds{};
    write_ds.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
    write_ds.dstSet = desc_set;
    write_ds.descriptorCount = 1;
    write_ds.descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    write_ds.pBufferInfo = &buf_info;
    vkUpdateDescriptorSets(device, 1, &write_ds, 0, nullptr);

    VkCommandPoolCreateInfo cp_ci2{};
    cp_ci2.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    cp_ci2.queueFamilyIndex = qf_idx;
    VkCommandPool cmd_pool;
    VK_OK(vkCreateCommandPool(device, &cp_ci2, nullptr, &cmd_pool));
    VkFence fence;
    VkFenceCreateInfo f_ci{};
    f_ci.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    VK_OK(vkCreateFence(device, &f_ci, nullptr, &fence));

    VkCommandBufferAllocateInfo alloc_cmd{};
    alloc_cmd.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    alloc_cmd.commandPool = cmd_pool;
    alloc_cmd.commandBufferCount = 1;
    VkCommandBuffer cmd;
    VK_OK(vkAllocateCommandBuffers(device, &alloc_cmd, &cmd));
    VkCommandBufferBeginInfo begin{};
    begin.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    VK_OK(vkBeginCommandBuffer(cmd, &begin));
    vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline);
    vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline_layout, 0, 1, &desc_set, 0, nullptr);
    struct PcData {
        float n000, n100, n010, n110, n001, n101, n011, n111;
        int32_t cell_w, cell_h;
    };
    PcData pc{n000, n100, n010, n110, n001, n101, n011, n111, CELL_W, CELL_H};
    vkCmdPushConstants(cmd, pipeline_layout, VK_SHADER_STAGE_COMPUTE_BIT, 0, 40, &pc);
    vkCmdDispatch(cmd, CELL_W, CELL_H, CELL_W);
    VK_OK(vkEndCommandBuffer(cmd));

    const auto start = std::chrono::high_resolution_clock::now();
    for (int i = 0; i < ITERATIONS; ++i) {
        VkSubmitInfo submit{};
        submit.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
        submit.commandBufferCount = 1;
        submit.pCommandBuffers = &cmd;
        VK_OK(vkQueueSubmit(queue, 1, &submit, fence));
        VK_OK(vkWaitForFences(device, 1, &fence, VK_TRUE, UINT64_MAX));
        VK_OK(vkResetFences(device, 1, &fence));
    }
    const auto end = std::chrono::high_resolution_clock::now();

    float sum = 0;
    void* mapped = nullptr;
    if (vkMapMemory(device, buf_mem, 0, total, 0, &mapped) == VK_SUCCESS) {
        for (size_t i = 0; i < total / sizeof(float); ++i) {
            sum += static_cast<float*>(mapped)[i];
        }
        vkUnmapMemory(device, buf_mem);
    }
    ASSERT_NE(sum, 0.0f);

    const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    const auto per_sec = static_cast<double>(ITERATIONS) / (static_cast<double>(ms) / 1000.0);
    std::println("Vulkan trilerp f32({}x{}x{}) x{}: {} ms ({} 次/s)", CELL_W, CELL_H, CELL_W, ITERATIONS, ms, per_sec);

    vkFreeCommandBuffers(device, cmd_pool, 1, &cmd);
    vkDestroyFence(device, fence, nullptr);
    vkDestroyCommandPool(device, cmd_pool, nullptr);
    vkDestroyDescriptorPool(device, desc_pool, nullptr);
    vkDestroyDescriptorSetLayout(device, ds_layout, nullptr);
    vkFreeMemory(device, buf_mem, nullptr);
    vkDestroyBuffer(device, buffer, nullptr);
    vkDestroyPipeline(device, pipeline, nullptr);
    vkDestroyPipelineLayout(device, pipeline_layout, nullptr);
    vkDestroyShaderModule(device, shader, nullptr);
    vkDestroyDevice(device, nullptr);
    vkDestroyInstance(instance, nullptr);
}
 *
 */
