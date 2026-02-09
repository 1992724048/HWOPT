// 遂沫 sycl-plugin.h
// 2026-02-08 23:47:28

#pragma once

#include <expected>
#include <string>
#include <tuple>
#include <vector>

namespace stdpp::sycl {
#define DLL_API __declspec(dllexport)

    enum DeviceType {
        CPU,
        GPU,
        FPGA,
        OTHER
    };

    class DLL_API Device {
    public:
        using DeviceInfo = std::tuple<DeviceType, std::string, std::string>;

        static auto switch_device(int queue_id, const DeviceInfo& device_info) -> bool;
        static auto create_device(const DeviceInfo& device_info) -> int;
        static auto create_device() -> int;
        static auto free(int queue_id) -> bool;
        static auto enable_profiling(int queue_id) -> bool;

        static auto get_device() -> std::expected<std::vector<DeviceInfo>, std::string>;

        static auto test_device(int queue_id) -> bool;
        static auto test_compute(int queue_id, size_t n, size_t iters) -> double;
        static auto test_bandwidth(int queue_id, size_t n) -> double;
    };
}
