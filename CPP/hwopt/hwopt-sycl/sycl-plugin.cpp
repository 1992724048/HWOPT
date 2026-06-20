#include <windows.h>
#include <mimalloc/mimalloc.h>

#include <atomic>
#include <expected>
#include <format>
#include <memory>
#include <optional>
#include <shared_mutex>
#include <sstream>
#include <string>
#include <string_view>
#include <unordered_map>
#include <utility>
#include <vector>

#include "sycl-plugin.h"
#include "sycl-memory.h"
#include "sycl-state.h"
#include "stdpp/logger.h"

namespace {
    auto async_error_handler(const sycl::exception_list& elist) -> void {
        for (const auto& e : elist) {
            try {
                std::rethrow_exception(e);
            } catch (const sycl::exception& ex) {
                ELOG << "[SYCL] [ASYNC ERROR] " << ex.what();
            }
        }
        std::abort();
    }
} // namespace

using namespace sycl;

std::shared_mutex sycl::queue_mutex;
std::atomic<uint64_t> sycl::id_gen{0};
std::unordered_map<uint64_t, std::atomic<std::shared_ptr<queue>>> sycl::queue_map{};

Device::Handle::Handle(Handle&& other) noexcept : id(std::exchange(other.id, 0)) {}

auto Device::Handle::operator=(Handle&& other) noexcept -> Handle& {
    if (this != &other) {
        std::unique_lock _(queue_mutex);
        if (id != 0) {
            queue_map.erase(id);
        }
        id = std::exchange(other.id, 0);
    }
    return *this;
}

Device::Handle::~Handle() {
    if (id != 0) {
        std::unique_lock _(queue_mutex);
        queue_map.erase(id);
    }
}

static auto pick_device() -> std::optional<sycl::device> {
    struct Scored {
        sycl::device dev;
        int score;
    };
    std::vector<Scored> candidates;

    for (auto& d : sycl::device::get_devices()) {
        int score = 0;
        const auto backend = d.get_platform().get_backend();

        if (backend == sycl::backend::ext_oneapi_level_zero) {
            score += 100;
        } else if (backend == sycl::backend::ext_oneapi_cuda) {
            score += 90;
        } else if (backend == sycl::backend::ext_oneapi_hip) {
            score += 80;
        } else if (backend == sycl::backend::opencl) {
            score += 50;
        }

        if (d.is_gpu()) {
            score += 30;
        } else if (d.is_accelerator()) {
            score += 20;
        } else if (d.is_cpu()) {
            score += 10;
        }

        if (d.is_gpu() && d.has(sycl::aspect::ext_oneapi_is_integrated_gpu)) {
            score += 15;
        }

        try {
            sycl::queue q(d);
            q.submit([&](sycl::handler& h) -> void {
                h.single_task([=]() noexcept -> void {});
            }).wait();
        } catch (...) {
            continue;
        }

        candidates.push_back({.dev = d, .score = score});
    }

    if (candidates.empty()) {
        return std::nullopt;
    }

    std::ranges::sort(candidates,
                      [](auto& a, auto& b) -> auto {
                          return a.score > b.score;
                      });
    return candidates[0].dev;
}

static auto match_type(const sycl::device& dev, const DeviceType type) -> bool {
    switch (type) {
        case CPU:
            return dev.is_cpu();
        case GPU:
            return dev.is_gpu();
        case ACCELERATOR:
            return dev.is_accelerator();
        case OTHER:
            return true;
    }
    return false;
}

auto Device::switch_device(Handle& handle, const DeviceInfo& device_info) -> std::optional<std::string> try {
    std::unique_lock _(queue_mutex);
    if (!queue_map.contains(handle.id)) {
        return std::format("[SYCL] [ERROR] invalid queue id: {}\n", handle.id);
    }

    for (auto& dev : ::sycl::device::get_devices()) {
        if (!match_type(dev, device_info.type)) {
            continue;
        }
        if (dev.get_info<::sycl::info::device::name>().contains(device_info.name) && dev.get_platform().get_info<::sycl::info::platform::name>().contains(device_info.platform)) {
            queue_map[handle.id] = std::make_shared<::sycl::queue>(dev);
            return std::nullopt;
        }
    }

    return "[SYCL] [ERROR] 切换失败!";
} catch (const ::sycl::exception& e) {
    return std::format("[SYCL] [ERROR] {}\n", e.what());
}

auto Device::create_device(const DeviceInfo& device_info) -> std::expected<Handle, std::string> try {
    for (auto& dev : ::sycl::device::get_devices()) {
        if (!match_type(dev, device_info.type)) {
            continue;
        }
        if (dev.get_info<::sycl::info::device::name>().contains(device_info.name) && dev.get_platform().get_info<::sycl::info::platform::name>().contains(device_info.platform)) {
            std::unique_lock _(queue_mutex);
            uint64_t id;
            do {
                id = ++id_gen;
            } while (id == 0 || queue_map.contains(id));
            queue_map[id] = std::make_shared<::sycl::queue>(dev);
            return Handle(id);
        }
    }
    return std::unexpected(std::string("[SYCL] [ERROR] 无匹配设备"));
} catch (const ::sycl::exception& e) {
    return std::unexpected(std::format("[SYCL] [ERROR] {}", e.what()));
}

auto Device::create_device() -> std::expected<Handle, std::string> try {
    const auto dev_opt = pick_device();
    if (!dev_opt) {
        return std::unexpected(std::string("[SYCL] [ERROR] 无可用设备"));
    }
    auto& dev = *dev_opt;

    std::unique_lock _(queue_mutex);
    uint64_t id;
    do {
        id = ++id_gen;
    } while (id == 0 || queue_map.contains(id));
    queue_map[id] = std::make_shared<::sycl::queue>(dev);
    ILOG << std::format("[SYCL] 默认设备: {} ({}, {})",
                        dev.get_info<::sycl::info::device::name>(),
                        dev.get_platform().get_info<::sycl::info::platform::name>(),
                        [&]() -> std::string_view {
                            switch (dev.get_platform().get_backend()) {
                                case ::sycl::backend::ext_oneapi_level_zero:
                                    return "Level Zero";
                                case ::sycl::backend::ext_oneapi_cuda:
                                    return "CUDA";
                                case ::sycl::backend::ext_oneapi_hip:
                                    return "HIP";
                                case ::sycl::backend::opencl:
                                    return "OpenCL";
                                default:
                                    return "其他";
                            }
                        }());
    return Handle(id);
} catch (const ::sycl::exception& e) {
    return std::unexpected(std::format("[SYCL] [ERROR] {}", e.what()));
}

auto Device::enable_profiling(Handle& handle) -> std::optional<std::string> try {
    std::unique_lock _(queue_mutex);
    if (!queue_map.contains(handle.id)) {
        return std::format("[SYCL] [ERROR] invalid queue id: {}", handle.id);
    }

    const auto old_queue = queue_map[handle.id].load();
    auto new_queue = std::make_shared<::sycl::queue>(old_queue->get_device(), async_error_handler, ::sycl::property::queue::enable_profiling{});

    queue_map[handle.id] = std::move(new_queue);
    return std::nullopt;
} catch (const ::sycl::exception& e) {
    return std::format("[SYCL] [ERROR] enable_profiling failed: {}", e.what());
}

inline auto Device::get_device() -> std::expected<std::vector<DeviceInfo>, std::string> try {
    const auto all = ::sycl::device::get_devices();
    std::vector<DeviceInfo> result;
    for (const auto& dev : all) {
        auto p = dev.get_platform();
        DeviceType type;
        if (dev.is_cpu()) {
            type = CPU;
        } else if (dev.is_gpu()) {
            type = GPU;
        } else if (dev.is_accelerator()) {
            type = ACCELERATOR;
        } else {
            type = OTHER;
        }

        try {
            ::sycl::buffer<float> buf{::sycl::range(1)};
            ::sycl::queue q(dev);
            q.submit([&](::sycl::handler& handle) -> void {
                const auto acc = buf.get_access<::sycl::access::mode::write>(handle);
                handle.single_task([=]() noexcept -> void {
                    acc[0] = ::sycl::fma(1.F, 2.F, 3.F);
                });
            }).wait();
        } catch (...) {
            continue;
        }

        result.push_back({.type = type, .name = dev.get_info<::sycl::info::device::name>(), .platform = p.get_info<::sycl::info::platform::name>()});
    }
    return result;
} catch (const ::sycl::exception& e) {
    return std::unexpected(std::format("[SYCL] [ERROR] {}", e.what()));
} catch (...) {
    return std::unexpected(std::string("[SYCL] [ERROR] 未知错误!"));
}

auto Device::get_default_device_info() -> std::optional<DeviceInfo> {
    const auto dev_opt = pick_device();
    if (!dev_opt) {
        return std::nullopt;
    }
    const auto& dev = *dev_opt;

    DeviceType type;
    if (dev.is_cpu()) {
        type = CPU;
    } else if (dev.is_gpu()) {
        type = GPU;
    } else if (dev.is_accelerator()) {
        type = ACCELERATOR;
    } else {
        type = OTHER;
    }

    return DeviceInfo{.type = type, .name = dev.get_info<::sycl::info::device::name>(), .platform = dev.get_platform().get_info<::sycl::info::platform::name>()};
}
