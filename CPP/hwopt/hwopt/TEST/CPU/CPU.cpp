// 遂沫 CPU.cpp
// 2026-02-09 01:48:10

#include <chrono>
#include <future>
#include <vector>

#include <windows.h>
#include <magic_enum/magic_enum.hpp>
#include <tbb/tbb.h>

using ClockT = std::chrono::high_resolution_clock;

auto elapsed_ms(const ClockT::time_point a, const ClockT::time_point b) -> double {
    return std::chrono::duration<double, std::milli>(b - a).count();
}

auto __declspec(dllexport) test_compute_tbb(const std::size_t n, const std::size_t iters) -> double {
    std::vector<float> out(n);

    const auto t0 = ClockT::now();

    parallel_for(tbb::blocked_range<std::size_t>(0, n),
                      [&](const tbb::blocked_range<std::size_t>& r) {
                          for (auto i = r.begin(); i != r.end(); ++i) {
                              float x = 1.0f;
                              float y = 2.0f;
                              float z = 3.0f;
#pragma omp simd
                              for (std::size_t k = 0; k < iters; ++k) {
                                  x = std::fma(x, y, z);
                                  y = std::fma(y, z, x);
                                  z = std::fma(z, x, y);
                              }

                              out[i] = x + y + z;
                          }
                      });

    const auto t1 = ClockT::now();

    const double ms = elapsed_ms(t0, t1);

    const double flops = static_cast<double>(n) * iters * 6.0;
    return flops / (ms * 1e-3) / 1e9;
}

auto __declspec(dllexport) test_bandwidth_tbb(const std::size_t n) -> double {
    const std::vector A(n, 1.0f);
    const std::vector B(n, 2.0f);
    std::vector<float> c(n);

    const auto t0 = ClockT::now();

    parallel_for(tbb::blocked_range<std::size_t>(0, n),
                      [&](const tbb::blocked_range<std::size_t>& r) {
#pragma ivdep
#pragma vector always
                          for (auto i = r.begin(); i != r.end(); ++i) {
                              c[i] = A[i] + 2.0f * B[i];
                          }
                      });

    const auto t1 = ClockT::now();

    const double ms = elapsed_ms(t0, t1);

    const double bytes = static_cast<double>(n) * 12.0;
    return bytes / (ms * 1e-3) / 1e9;
}
