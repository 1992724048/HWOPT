#pragma once

#include <expected>
#include <memory>
#include <optional>
#include <string>
#include <vector>

#define DLL_API __declspec(dllexport)

namespace sycl {
    enum DeviceType : int8_t {
        CPU,
        GPU,
        ACCELERATOR,
        OTHER
    };

    struct DeviceInfo {
        DeviceType type;
        std::string name;
        std::string platform;
    };

    class DLL_API Device {
    public:
        struct DLL_API Handle {
            explicit Handle(const uint64_t id) : id(id) {}
            Handle(const Handle&) = delete;
            Handle(Handle&& other) noexcept;
            auto operator=(const Handle&) -> Handle& = delete;
            auto operator=(Handle&& other) noexcept -> Handle&;
            ~Handle();

            uint64_t id{0};
        };

        static auto switch_device(Handle& handle, const DeviceInfo& device_info) -> std::optional<std::string>;
        static auto create_device(const DeviceInfo& device_info) -> std::expected<Handle, std::string>;
        static auto create_device() -> std::expected<Handle, std::string>;
        static auto enable_profiling(Handle& handle) -> std::optional<std::string>;

        static auto get_device() -> std::expected<std::vector<DeviceInfo>, std::string>;
        static auto get_default_device_info() -> std::optional<DeviceInfo>;
    };
} // namespace sycl
