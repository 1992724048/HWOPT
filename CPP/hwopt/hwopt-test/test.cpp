// 遂沫 test.cpp
// 2026-02-09 20:13:52

#include "pch.h"

TEST(NoiseBench, Perlin) {
    const minecraft::PerlinNoise noise(123456, {0, std::vector{1.0, 1.0, 1.0, 1.0}}, true);

    constexpr int n = 512;
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

TEST(TBB, CPU) {
    const double gflops = test_compute_tbb(181920, 50'000);
    const double bandwidth = test_bandwidth_tbb(181920);
    std::cout << "Compute:  " << gflops << " GFLOPS\n";
    std::cout << "Memory:   " << bandwidth << " GB/s\n";
}

TEST(SYCL, GPU) {
    static const auto exp = stdpp::sycl::Device::get_device();
    ASSERT_TRUE(exp.has_value()) << "获取设备失败: " << exp.error();

    std::cout << "设备列表:\n";
    for (auto& [type, name, platform] : exp.value()) {
        std::cout << name << " (" << platform << ")" << '\n';
    }

    std::cout << "\n";

    for (auto& info : exp.value()) {
        auto& [type, name, platform] = info;
        std::cout << "选择设备: " << name << " (" << platform << ")" << '\n';

        try {
            const auto gpu_queue = stdpp::sycl::Device::create_device(info);
            if (!stdpp::sycl::Device::test_device(gpu_queue)) {
                std::cout << "设备测试失败!\n";
                continue;
            }
            ASSERT_TRUE(stdpp::sycl::Device::enable_profiling(gpu_queue)) << "设置性能分析模式失败!";

            const double gflops = stdpp::sycl::Device::test_compute(gpu_queue, 50'000, 181920);
            const double bandwidth = stdpp::sycl::Device::test_bandwidth(gpu_queue, 181920);
            std::cout << "Compute:  " << gflops << " GFLOPS\n";
            std::cout << "Memory:   " << bandwidth << " GB/s\n";
        } catch (...) {
            std::cout << name << " 执行失败!\n";
        }
    }
}
