// 遂沫 sycl-plugin.h
// 2026-02-10 20:48:53

#pragma once

#include <expected>
#include <optional>
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

        static auto switch_device(int queue_id, const DeviceInfo& device_info) -> std::optional<std::string>;
        static auto create_device(const DeviceInfo& device_info) -> std::expected<int, std::string>;
        static auto create_device() -> std::expected<int, std::string>;
        static auto free(int queue_id) -> std::optional<std::string>;
        static auto enable_profiling(int queue_id) -> std::optional<std::string>;

        static auto get_device() -> std::expected<std::vector<DeviceInfo>, std::string>;
    };
}
