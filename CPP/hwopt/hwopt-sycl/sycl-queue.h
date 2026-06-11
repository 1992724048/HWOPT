// 2026-06-11 12:52:13

#pragma once
#include <Windows.h>
#include <atomic>
#include <memory>
#include <sycl.hpp>
#include <usm.hpp>

extern std::unordered_map<int, std::atomic<std::shared_ptr<sycl::queue>>> queue_map;

namespace sycl {
    template<typename T>
    struct DeviceMemory final {
        size_t count;
        T* ptr;

        ~DeviceMemory() {
            dealloc();
        }

        static auto alloc(size_t count, const std::shared_ptr<queue>& queue_sp) -> DeviceMemory {
            DeviceMemory memory;
            memory.queue_ptr = queue_sp.get();
            memory.count = count;
            memory.ptr = static_cast<T*>(malloc_device<T>(count, *queue_sp));
            return memory;
        }

        auto copy_from(const T* src, const size_t count = 1) -> bool {
            if (count > this->count && !ptr && !src) {
                return false;
            }
            queue_ptr->memcpy(ptr, src, count * sizeof(T)).wait();
            return true;
        }

        auto copy_to(T* dest, const size_t count = 1) -> bool {
            if (count > this->count && !ptr && !dest) {
                return false;
            }
            queue_ptr->memcpy(dest, ptr, count * sizeof(T)).wait();
            return true;
        }

        auto clear() const -> void {
            if (!ptr) {
                return;
            }
            queue_ptr->memset(ptr, 0, count * sizeof(T)).wait();
        }

        auto set(int value) const -> void {
            if (!ptr) {
                return;
            }
            queue_ptr->memset(ptr, 0, count * sizeof(T)).wait();
        }

        auto dealloc() -> void {
            if (!ptr) {
                return;
            }
            sycl::free(ptr, *queue_ptr);
            ptr = nullptr;
        }

        auto operator->() -> T* {
            return ptr;
        }

        auto operator->() const -> const T* {
            return ptr;
        }
    private:
        DeviceMemory() = default;
        queue* queue_ptr;
    };

    template<typename T>
    struct SharedMemory final {
        size_t count;
        T* ptr;

        ~SharedMemory() {
            dealloc();
        }

        static auto alloc(size_t count, const std::shared_ptr<queue>& queue_sp) -> SharedMemory {
            SharedMemory memory;
            memory.queue_ptr = queue_sp.get();
            memory.count = count;
            memory.ptr = static_cast<T*>(malloc_shared<T>(count, *queue_sp));
            return memory;
        }

        auto clear() const -> void {
            if (!ptr) {
                return;
            }
            queue_ptr->memset(ptr, 0, count).wait();
        }

        auto dealloc() -> void {
            if (!ptr) {
                return;
            }
            sycl::free(ptr, *queue_ptr);
            ptr = nullptr;
        }

        auto operator->() -> T* {
            return ptr;
        }

        auto operator->() const -> const T* {
            return ptr;
        }
    private:
        SharedMemory() = default;
        queue* queue_ptr;
    };
}
