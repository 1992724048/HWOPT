#pragma once
#include <cstddef>

extern __declspec(dllexport) auto test_compute_tbb(const std::size_t n, const std::size_t iters) -> double;
extern __declspec(dllexport) auto test_bandwidth_tbb(const std::size_t n) -> double;