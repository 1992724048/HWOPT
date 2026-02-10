#pragma once
#include <expected>
#include <expected>
#include <xstring>
#include <optional>

class TEST {
public:
    static __declspec(dllexport) auto test_device(int queue_id) -> std::optional<std::string>;
    static __declspec(dllexport) auto test_compute(int queue_id, size_t n, size_t iters) -> std::expected<double, std::string>;
    static __declspec(dllexport) auto test_bandwidth(int queue_id, size_t n) -> std::expected<double, std::string>;
};
