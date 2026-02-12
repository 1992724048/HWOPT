#pragma once
#include <expected>
#include <expected>
#include <xstring>
#include <optional>
#include <vector>

namespace minecraft_sycl {
    struct Tuple;
}

class  __declspec(dllexport) TEST {
public:
    static auto test_device(int queue_id) -> std::optional<std::string>;
    static auto test_compute(int queue_id, size_t n, size_t iters) -> std::expected<double, std::string>;
    static auto test_bandwidth(int queue_id, size_t n) -> std::expected<double, std::string>;
    static auto test_perlin_noise(int queue_id, const std::vector<minecraft_sycl::Tuple>& tuples) -> std::expected<double, std::string>;
};
