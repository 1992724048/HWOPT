#include <windows.h>

#include <sycl.hpp>
#include <cl/cl.h>
#include <omp.h>

#include <print>

#include <parallel_hashmap/phmap.h>

#include "sycl-plugin.h"

auto DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) -> BOOL {
    return TRUE;
}

static phmap::parallel_node_hash_map<int, std::atomic<std::shared_ptr<sycl::queue>>> queue_map;

namespace stdpp::sycl {
    static auto match_type(const ::sycl::device& dev, const DeviceType type) -> bool {
        switch (type) {
            case CPU:
                return dev.is_cpu();
            case GPU:
                return dev.is_gpu();
            case FPGA:
                return dev.is_accelerator();
            case OTHER:
                return true;
        }
        return false;
    }

    inline auto Device::switch_device(const int queue_id, const DeviceType type, const std::string& name) -> bool {
        try {
            const auto devices = ::sycl::device::get_devices();

            for (auto& dev : devices) {
                if (!match_type(dev, type)) {
                    continue;
                }

                if (dev.get_info<::sycl::info::device::name>().find(name) != std::string::npos) {
                    queue_map[queue_id] = std::make_shared<::sycl::queue>(dev);
                    return true;
                }
            }
        } catch (const ::sycl::exception& e) {
            std::string last_error_ = e.what();
        }

        return false;
    }

    auto Device::create_device(const DeviceType type, const std::string& name) -> int {
        try {
            const auto devices = ::sycl::device::get_devices();

            for (auto& dev : devices) {
                if (!match_type(dev, type)) {
                    continue;
                }

                if (dev.get_info<::sycl::info::device::name>().find(name) != std::string::npos) {
                    static std::atomic<uint64_t> idgen{0};
                    const auto id = ++idgen;
                    queue_map[idgen++] = std::make_shared<::sycl::queue>(dev);
                    return id;
                }
            }
        } catch (const ::sycl::exception& e) {
            std::string last_error_ = e.what();
        }

        return -1;
    }

    inline auto Device::get_device() -> std::expected<phmap::flat_hash_map<DeviceType, std::vector<std::pair<std::string, std::string>>>, std::string> {
        try {
            phmap::flat_hash_map<DeviceType, std::vector<std::pair<std::string, std::string>>> result;
            for (auto& dev : ::sycl::device::get_devices()) {
                DeviceType type;
                auto p = dev.get_platform();

                std::string name = dev.get_info<::sycl::info::device::name>();
                if (!dev.has_extension("cl_khr_il_program") || !p.has_extension("cl_khr_il_program")) {
                    std::println("[SYCL] [WARN] {} : Device not support SPIR-V(cl_khr_il_program)", name);
                    continue;
                }

                if (dev.is_cpu()) {
                    type = CPU;
                } else if (dev.is_gpu()) {
                    type = GPU;
                } else if (dev.is_accelerator()) {
                    type = FPGA;
                } else {
                    type = OTHER;
                }

                result[type].emplace_back(name, p.get_info<::sycl::info::platform::name>());
            }
            return result;
        } catch (const ::sycl::exception& e) {
            std::println("[SYCL] [ERROR] {}", e.what());
            return std::unexpected(std::string(e.what()));
        } catch (...) {
            std::println("[SYCL] [ERROR] Test error: Unkonw error");
            return std::unexpected("Unkonw error");
        }
    }
}
