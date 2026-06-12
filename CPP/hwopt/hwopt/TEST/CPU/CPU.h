#pragma once
#include <cstddef>

extern __declspec(dllexport) auto test_compute_tbb(std::size_t n, std::size_t iters) -> double;
extern __declspec(dllexport) auto test_bandwidth_tbb(std::size_t n) -> double;
extern __declspec(dllexport) auto test_perlin_noise(size_t n) -> double;
