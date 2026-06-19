// 2026-06-11 12:51:14

#pragma comment(lib, "ntdll.lib")
// ReSharper disable CppUnusedIncludeDirective
// ReSharper disable CppWrongIncludesOrder
#include <mimalloc/mimalloc.h>
#include <vector>
#include <string>
#include <unordered_map>
#include <future>
#include <iostream>
#include <print>
#include <ostream>
#include <expected>
#include <shared_mutex>
#include <tuple>
#include <windows.h>

#include "sycl-plugin.h"
#include "sycl-queue.h"

#define API __declspec(dllexport)

auto DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) -> BOOL {
    if (ul_reason_for_call == DLL_PROCESS_ATTACH) {
        mi_stats_reset();
    }
    return TRUE;
}

std::shared_mutex queue_mutex;
std::atomic<uint64_t> id_gen{0};
std::unordered_map<uint64_t, std::atomic<std::shared_ptr<sycl::queue>>> queue_map{};

namespace stdpp::sycl {
    auto match_type(const ::sycl::device& dev, const DeviceType type) -> bool {
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

    inline auto Device::switch_device(const std::unique_ptr<Handle>& queue_id, const DeviceInfo& device_info) -> std::optional<std::string> try {
        if (queue_id == nullptr) {
            return std::nullopt;
        }

        std::unique_lock _(queue_mutex);
        if (!queue_map.contains(queue_id->id)) {
            return std::format("[SYCL] [ERROR] invalid queue id: {}\n", queue_id->id);
        }

        for (auto& dev : ::sycl::device::get_devices()) {
            if (!match_type(dev, std::get<0>(device_info))) {
                continue;
            }

            if (dev.get_info<::sycl::info::device::name>().contains(std::get<1>(device_info)) && dev.get_platform().get_info<::sycl::info::platform::name>().contains(std::get<2>(device_info))) {
                queue_map[queue_id->id] = std::make_shared<::sycl::queue>(dev);
                return std::nullopt;
            }
        }

        return "[SYCL] [ERROR] 切换失败!";
    } catch (const ::sycl::exception& e) {
        return std::format("[SYCL] [ERROR] {}\n", e.what());
    }

    auto Device::create_device(const DeviceInfo& device_info) -> std::expected<std::unique_ptr<Handle>, std::string> try {
        for (auto& dev : ::sycl::device::get_devices()) {
            if (!match_type(dev, std::get<0>(device_info))) {
                continue;
            }

            if (dev.get_info<::sycl::info::device::name>().contains(std::get<1>(device_info)) && dev.get_platform().get_info<::sycl::info::platform::name>().contains(std::get<2>(device_info))) {
                const auto id = ++id_gen;
                std::unique_lock _(queue_mutex);
                queue_map[id] = std::make_shared<::sycl::queue>(dev);
                return std::make_unique<Handle>(id);
            }
        }
        return nullptr;
    } catch (const ::sycl::exception& e) {
        return std::unexpected(std::format("[SYCL] [ERROR] {}", e.what()));
    }

    auto Device::create_device() -> std::expected<std::unique_ptr<Handle>, std::string> try {
        const auto id = ++id_gen;
        std::unique_lock _(queue_mutex);
        queue_map[id] = std::make_shared<::sycl::queue>();
        return std::make_unique<Handle>(id);
    } catch (const ::sycl::exception& e) {
        return std::unexpected(std::format("[SYCL] [ERROR] {}", e.what()));
    }

    auto Device::enable_profiling(const std::unique_ptr<Handle>& queue_id) -> std::optional<std::string> try {
        if (queue_id == nullptr) {
            return std::nullopt;
        }

        std::unique_lock _(queue_mutex);
        if (!queue_map.contains(queue_id->id)) {
            return std::format("[SYCL] [ERROR] invalid queue id: {}", queue_id->id);
        }

        const auto old_queue = queue_map[queue_id->id].load();
        const auto dev = old_queue->get_device();

        auto new_queue = std::make_shared<::sycl::queue>(dev,
                                                         [](const ::sycl::exception_list& elist) -> void {
                                                             for (const auto& e : elist) {
                                                                 try {
                                                                     std::rethrow_exception(e);
                                                                 } catch (const ::sycl::exception& ex) {
                                                                     std::println("[SYCL] [ASYNC ERROR] {}", ex.what());
                                                                 }
                                                             }
                                                             std::abort();
                                                         },
                                                         ::sycl::property::queue::enable_profiling{});

        queue_map[queue_id->id] = std::move(new_queue);
        return std::nullopt;
    } catch (const ::sycl::exception& e) {
        return std::format("[SYCL] [ERROR] enable_profiling failed: {}", e.what());
    }

    inline auto Device::get_device() -> std::expected<std::vector<DeviceInfo>, std::string> try {
        std::vector<std::tuple<DeviceType, std::string, std::string>> result;
        for (auto& dev : ::sycl::device::get_devices()) {
            DeviceType type;
            auto p = dev.get_platform();

            if (dev.is_cpu()) {
                type = CPU;
            } else if (dev.is_gpu()) {
                type = GPU;
            } else if (dev.is_accelerator()) {
                type = FPGA;
            } else {
                type = OTHER;
            }

            try {
                ::sycl::buffer<float> buf{::sycl::range(1)};
                ::sycl::queue q(dev);
                q.submit([&](::sycl::handler& handle) -> void {
                    const auto acc = buf.get_access<::sycl::access::mode::write>(handle);
                    handle.single_task([=] -> void {
                        acc[0] = ::sycl::fma(1.F, 2.F, 3.F);
                    });
                }).wait();
            } catch (...) {
                continue;
            }

            result.emplace_back(type, dev.get_info<::sycl::info::device::name>(), p.get_info<::sycl::info::platform::name>());
        }
        return result;
    } catch (const ::sycl::exception& e) {
        return std::unexpected(std::format("[SYCL] [ERROR] {}", e.what()));
    } catch (...) {
        return std::unexpected(std::string("[SYCL] [ERROR] 未知错误!"));
    }

    auto Device::free(uint64_t queue_id) -> std::optional<std::string> {
        std::unique_lock _(queue_mutex);
        if (!queue_map.contains(queue_id)) {
            return std::format("[SYCL] [ERROR] invalid queue id: {}", queue_id);
        }

        queue_map.erase(queue_id);
        return std::nullopt;
    }
} // namespace stdpp::sycl
