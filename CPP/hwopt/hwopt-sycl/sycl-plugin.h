#pragma once

#include <string>
#include <vector>
#include <expected>

namespace stdpp::sycl {
#define DLL_API __declspec(dllexport)

    enum DeviceType { CPU, GPU, FPGA, OTHER };

    class DLL_API Device {
    public:
        static auto switch_device(int queue_id, DeviceType type, const std::string& name) -> bool;
        static auto create_device(DeviceType type, const std::string& name) -> int;
        static auto get_device() -> std::expected<phmap::flat_hash_map<DeviceType, std::vector<std::pair<std::string, std::string>>>, std::string>;
    };
}
