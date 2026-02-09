// 遂沫 dllmain.cpp
// 2026-02-10 01:01:08

#include <windows.h>

#include <print>

#include <parallel_hashmap/phmap.h>

#include <atomic>
#include "sycl-plugin.h"
#include "sycl-queue.h"

auto DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) -> BOOL {
    return TRUE;
}

static std::atomic<uint64_t> id_gen{0};
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

    inline auto Device::switch_device(const int queue_id, const DeviceInfo& device_info) -> bool {
        if (!queue_map.contains(queue_id)) {
            std::println("[SYCL] [ERROR] invalid queue id: {}", queue_id);
            return false;
        }

        try {
            for (auto& dev : ::sycl::device::get_devices()) {
                if (!match_type(dev, std::get<0>(device_info))) {
                    continue;
                }

                if (dev.get_info<::sycl::info::device::name>().contains(std::get<1>(device_info)) && dev.get_platform().get_info<::sycl::info::platform::name>().contains(std::get<2>(device_info))) {
                    queue_map[queue_id] = std::make_shared<::sycl::queue>(dev);
                    return true;
                }
            }
        } catch (const ::sycl::exception& e) {
            std::println("[SYCL] [ERROR] {}", e.what());
        }

        return false;
    }

    auto Device::create_device(const DeviceInfo& device_info) -> int try {
        for (auto& dev : ::sycl::device::get_devices()) {
            if (!match_type(dev, std::get<0>(device_info))) {
                continue;
            }

            if (dev.get_info<::sycl::info::device::name>().contains(std::get<1>(device_info)) && dev.get_platform().get_info<::sycl::info::platform::name>().contains(std::get<2>(device_info))) {
                const auto id = ++id_gen;
                queue_map[id_gen++] = std::make_shared<::sycl::queue>(dev);
                return id;
            }
        }
        return -1;
    } catch (const ::sycl::exception& e) {
        std::println("[SYCL] [ERROR] {}", e.what());
        return -1;
    }

    auto Device::create_device() -> int try {
        const auto id = ++id_gen;
        queue_map[id_gen++] = std::make_shared<::sycl::queue>();
        return id;
    } catch (const ::sycl::exception& e) {
        std::println("[SYCL] [ERROR] {}", e.what());
        return -1;
    }

    auto Device::free(int queue_id) -> bool {
        if (!queue_map.contains(queue_id)) {
            std::println("[SYCL] [ERROR] invalid queue id: {}", queue_id);
            return false;
        }

        queue_map.erase(queue_id);
        return true;
    }

    auto Device::enable_profiling(int queue_id) -> bool try {
        if (!queue_map.contains(queue_id)) {
            std::println("[SYCL] [ERROR] invalid queue id: {}", queue_id);
            return false;
        }

        const auto old_queue = queue_map[queue_id].load();
        const auto dev = old_queue->get_device();

        auto new_queue = std::make_shared<::sycl::queue>(dev,
                                                         [](const ::sycl::exception_list& elist) {
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

        queue_map[queue_id] = std::move(new_queue);
        return true;
    } catch (const ::sycl::exception& e) {
        std::println("[SYCL] [ERROR] enable_profiling failed: {}", e.what());
        return false;
    }


    inline auto Device::get_device() -> std::expected<std::vector<DeviceInfo>, std::string> {
        try {
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

                result.emplace_back(type, dev.get_info<::sycl::info::device::name>(), p.get_info<::sycl::info::platform::name>());
            }
            return result;
        } catch (const ::sycl::exception& e) {
            std::println("[SYCL] [ERROR] {}", e.what());
            return std::unexpected(std::string(e.what()));
        } catch (...) {
            std::println("[SYCL] [ERROR] Unkonw error");
            return std::unexpected("Unkonw error");
        }
    }

    static auto elapsed_ms(const ::sycl::event& e) -> double try {
        const auto start = e.get_profiling_info<::sycl::info::event_profiling::command_start>();
        const auto end = e.get_profiling_info<::sycl::info::event_profiling::command_end>();
        return (end - start) * 1e-6;
    } catch (const ::sycl::exception& e2) {
        std::println("[SYCL] [ERROR] {}", e2.what());
        return 0.f;
    }

    auto Device::test_device(const int queue_id) -> bool try {
        constexpr int n = 10;
        ::sycl::buffer<float> buf{::sycl::range(n)};

        const auto& p = queue_map[queue_id].load();
        p->submit([&](::sycl::handler& h) {
            const auto acc = buf.get_access<::sycl::access::mode::write>(h);
            h.parallel_for(::sycl::range(n),
                           [=](const ::sycl::id<> i) {
                               float x = 1.0f;
                               float y = 2.0f;
                               float z = 3.0f;

                               for (size_t k = 0; k < 10; ++k) {
                                   x = ::sycl::fma(x, y, z);
                                   y = ::sycl::fma(x, y, z);
                                   z = ::sycl::fma(x, y, z);
                               }

                               acc[i] = x + y + z;
                           });
        });

        p->wait();
        return true;
    } catch (const ::sycl::exception& e2) {
        std::println("[SYCL] [ERROR] {}", e2.what());
        return false;
    }

    auto Device::test_compute(const int queue_id, const size_t n, const size_t iters) -> double {
        ::sycl::buffer<float> buf{::sycl::range(n)};

        ::sycl::event e = queue_map[queue_id].load()->submit([&](::sycl::handler& h) {
            const auto acc = buf.get_access<::sycl::access::mode::write>(h);
            h.parallel_for(::sycl::range(n),
                           [=](const ::sycl::id<> i) {
                               float x = 1.0f;
                               float y = 2.0f;
                               float z = 3.0f;

                               for (size_t k = 0; k < iters; ++k) {
                                   x = ::sycl::fma(x, y, z);
                                   y = ::sycl::fma(x, y, z);
                                   z = ::sycl::fma(x, y, z);
                               }

                               acc[i] = x + y + z;
                           });
        });

        e.wait();
        const double ms = elapsed_ms(e);

        const double flops = static_cast<double>(n) * iters * 6.0;
        return flops / (ms * 1e-3) / 1e9;
    }

    auto Device::test_bandwidth(const int queue_id, const size_t n) -> double {
        ::sycl::buffer<float> a{::sycl::range(n)};
        ::sycl::buffer<float> b{::sycl::range(n)};
        ::sycl::buffer<float> c{::sycl::range(n)};

        ::sycl::event e = queue_map[queue_id].load()->submit([&](::sycl::handler& h) {
            const auto A = a.get_access<::sycl::access::mode::read>(h);
            const auto B = b.get_access<::sycl::access::mode::read>(h);
            const auto C = c.get_access<::sycl::access::mode::write>(h);

            h.parallel_for(::sycl::range(n),
                           [=](const ::sycl::id<> i) {
                               C[i] = A[i] + 2.0f * B[i];
                           });
        });

        e.wait();
        const double ms = elapsed_ms(e);

        const double bytes = static_cast<double>(n) * 12.0;
        return bytes / (ms * 1e-3) / 1e9;
    }
}
