// 遂沫 SYCL-TEST.cpp
// 2026-02-10 22:02:55

#include "SYCL-TEST.h"
#include "../sycl-queue.h"

#include <print>

static auto elapsed_ms(const sycl::event& e) -> std::expected<double, std::string> try {
    const auto start = e.get_profiling_info<sycl::info::event_profiling::command_start>();
    const auto end = e.get_profiling_info<sycl::info::event_profiling::command_end>();
    return (end - start) * 1e-6;
} catch (const sycl::exception& e2) {
    return std::unexpected(std::format("[SYCL] [ERROR] {}", e2.what()));
}

auto TEST::test_device(const int queue_id) -> std::optional<std::string> try {
    constexpr int n = 10;
    sycl::buffer<float> buf{sycl::range(n)};

    const auto& p = queue_map[queue_id].load();
    p->submit([&](sycl::handler& h) {
        const auto acc = buf.get_access<sycl::access::mode::write>(h);
        h.parallel_for(sycl::range(n),
                       [=](const sycl::id<> i) {
                           float x = 1.0f;
                           float y = 2.0f;
                           float z = 3.0f;

                           for (size_t k = 0; k < 10; ++k) {
                               x = sycl::fma(x, y, z);
                               y = sycl::fma(x, y, z);
                               z = sycl::fma(x, y, z);
                           }

                           acc[i] = x + y + z;
                       });
    });

    p->wait();
    return std::nullopt;
} catch (const sycl::exception& e2) {
    return std::format("[SYCL] [ERROR] {}", e2.what());
}

auto TEST::test_compute(const int queue_id, const size_t n, const size_t iters) -> std::expected<double, std::string> try {
    sycl::buffer<float> buf{sycl::range(n)};

    sycl::event e = queue_map[queue_id].load()->submit([&](sycl::handler& h) {
        const auto acc = buf.get_access<sycl::access::mode::write>(h);
        h.parallel_for(sycl::range(n),
                       [=](const sycl::id<> i) {
                           float x = 1.0f;
                           float y = 2.0f;
                           float z = 3.0f;

                           for (size_t k = 0; k < iters; ++k) {
                               x = sycl::fma(x, y, z);
                               y = sycl::fma(x, y, z);
                               z = sycl::fma(x, y, z);
                           }

                           acc[i] = x + y + z;
                       });
    });
    e.wait();

    auto exp = elapsed_ms(e);
    if (exp) {
        const double ms = exp.value();
        const double flops = iters * static_cast<double>(n) * 6.0;
        return flops / (ms * 1e-3) / 1e9;
    }
    return std::unexpected(exp.error());
} catch (const sycl::exception& e2) {
    return std::unexpected(std::format("[SYCL] [ERROR] {}", e2.what()));
}

auto TEST::test_bandwidth(const int queue_id, const size_t n) -> std::expected<double, std::string> try {
    sycl::buffer<float> a{sycl::range(n)};
    sycl::buffer<float> b{sycl::range(n)};
    sycl::buffer<float> c{sycl::range(n)};

    sycl::event e = queue_map[queue_id].load()->submit([&](sycl::handler& h) {
        const auto A = a.get_access<sycl::access::mode::read>(h);
        const auto B = b.get_access<sycl::access::mode::read>(h);
        const auto C = c.get_access<sycl::access::mode::write>(h);

        h.parallel_for(sycl::range(n),
                       [=](const sycl::id<> i) {
                           C[i] = A[i] + 2.0f * B[i];
                       });
    });

    e.wait();

    auto exp = elapsed_ms(e);
    if (exp) {
        const double ms = exp.value();
        const double bytes = static_cast<double>(n) * 12.0;
        return bytes / (ms * 1e-3) / 1e9;
    }
    return std::unexpected(exp.error());
} catch (const sycl::exception& e2) {
    return std::unexpected(std::format("[SYCL] [ERROR] {}", e2.what()));
}
