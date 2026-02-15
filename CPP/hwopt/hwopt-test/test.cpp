// 遂沫 test.cpp
// 2026-02-13 12:10:43

#include "pch.h"

auto format_duration(auto duration) -> std::string {
    const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(duration).count();
    const auto s = std::chrono::duration_cast<std::chrono::seconds>(duration).count();
    if (s > 0) {
        return std::format("{}.{:03}s", s, ms % 1000);
    }
    return std::format("{}ms", ms);
}

TEST(NoiseBench, Perlin) {
    const minecraft::PerlinNoise noise(114514, {2, std::vector{1.0, 1.0, 1.0}}, true);

    constexpr int n = 50'000'000;
    double sum = 0.0;

    const auto total_start = std::chrono::high_resolution_clock::now();
    for (int z = 0; z < n; ++z) {
        sum += noise.get_value(100.3, 64, 200);
    }
    const auto total_end = std::chrono::high_resolution_clock::now();
    std::cout << "总用时: " << format_duration(total_end - total_start) << "\n";

    EXPECT_NE(sum, 0.0);
}

TEST(TBB, CPU) {
    const auto total_start = std::chrono::high_resolution_clock::now();

    const auto compute_start = std::chrono::high_resolution_clock::now();
    const double gflops = test_compute_tbb(181920, 50'000);
    const auto compute_end = std::chrono::high_resolution_clock::now();

    const auto bandwidth_start = std::chrono::high_resolution_clock::now();
    const double bandwidth = test_bandwidth_tbb(181920);
    const auto bandwidth_end = std::chrono::high_resolution_clock::now();

    const auto noise_start = std::chrono::high_resolution_clock::now();
    test_perlin_noise(50'000'000);
    const auto noise_end = std::chrono::high_resolution_clock::now();

    const auto total_end = std::chrono::high_resolution_clock::now();

    std::cout << "Compute:  " << gflops << " GFLOPS" << " [用时: " << format_duration(compute_end - compute_start) << "]\n";
    std::cout << "Memory:   " << bandwidth << " GB/s" << " [用时: " << format_duration(bandwidth_end - bandwidth_start) << "]\n";
    std::cout << "PerlinNoise 用时: " << format_duration(noise_end - noise_start) << '\n';
    std::cout << "总用时: " << format_duration(total_end - total_start) << "\n";
}

TEST(SYCL, GPU) {
    try {
        const auto total_start = std::chrono::high_resolution_clock::now();

        static const auto exp = stdpp::sycl::Device::get_device();
        ASSERT_TRUE(exp.has_value()) << "获取设备失败: " << exp.error();

        for (auto& info : exp.value()) {
            auto& [type, name, platform] = info;
            std::cout << "\n设备: " << name << " (" << platform << ")\n";

            if (const auto gpu_queue = stdpp::sycl::Device::create_device(info)) {
                auto opt = stdpp::sycl::Device::enable_profiling(*gpu_queue);
                ASSERT_TRUE(!opt.has_value()) << "设置性能分析模式失败! 错误: " << *opt << '\n';

                const auto compute_start = std::chrono::high_resolution_clock::now();
                if (auto a = TEST::test_compute(*gpu_queue, 181920, 50'000)) {
                    const auto compute_end = std::chrono::high_resolution_clock::now();
                    std::cout << "Compute:  " << *a << " GFLOPS" << " [用时: " << format_duration(compute_end - compute_start) << "]\n";
                } else {
                    std::cout << "Compute 测试失败:" << a.error() << '\n';
                }

                const auto bandwidth_start = std::chrono::high_resolution_clock::now();
                if (auto b = TEST::test_bandwidth(*gpu_queue, 181920)) {
                    const auto bandwidth_end = std::chrono::high_resolution_clock::now();
                    std::cout << "Memory:   " << *b << " GB/s" << " [用时: " << format_duration(bandwidth_end - bandwidth_start) << "]\n";
                } else {
                    std::cout << "Memory 测试失败: " << b.error() << '\n';
                }

                int n = 50'000'000;
                std::vector<minecraft_sycl::Tuple> tuples(n);
                for (auto& value : tuples) {
                    value.pos = {--n, ++n, --n};
                }
                const auto noise_start = std::chrono::high_resolution_clock::now();
                if (auto c = TEST::test_perlin_noise(*gpu_queue, tuples)) {
                    const auto noise_end = std::chrono::high_resolution_clock::now();
                    std::cout << "PerlinNoise 用时: " << format_duration(noise_end - noise_start) << '\n';
                } else {
                    std::cout << "PerlinNoise 测试失败: " << c.error() << '\n';
                }
            } else {
                std::cout << "创建设备队列失败\n";
            }
        }

        const auto total_end = std::chrono::high_resolution_clock::now();
        std::cout << "\n总用时: " << format_duration(total_end - total_start) << "\n";
    } catch (const std::exception& e) {
        std::cout << "执行失败! 异常: " << e.what() << "\n";
    } catch (...) {
        std::cout << "执行失败! 未知异常\n";
    }
}
