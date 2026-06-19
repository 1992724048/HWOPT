// 遂沫 sycl-plugin.h
// 2026-02-10 20:48:53

#pragma once

#include <expected>
#include <memory>
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

        struct Handle {
            explicit Handle(const uint64_t id) {
                this->id = id;
            }

            Handle(const Handle& other) = delete;
            Handle(Handle&& other) noexcept = delete;
            auto operator=(const Handle& other) -> Handle& = delete;
            auto operator=(Handle&& other) noexcept -> Handle& = delete;

            ~Handle() noexcept {
                if (id != 0) {
                    try {
                        free(id);
                    } catch (const std::exception&) {
                        // ignore
                    }
                }
            }

            uint64_t id{0};

            friend Device;
        };

        static auto switch_device(const std::unique_ptr<Handle>& queue_id, const DeviceInfo& device_info) -> std::optional<std::string>;
        static auto create_device(const DeviceInfo& device_info) -> std::expected<std::unique_ptr<Handle>, std::string>;
        static auto create_device() -> std::expected<std::unique_ptr<Handle>, std::string>;
        static auto enable_profiling(const std::unique_ptr<Handle>& queue_id) -> std::optional<std::string>;

        static auto get_device() -> std::expected<std::vector<DeviceInfo>, std::string>;
    private:
        static auto free(uint64_t queue_id) -> std::optional<std::string>;
    };
} // namespace stdpp::sycl
