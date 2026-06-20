#pragma once

#include <cstddef>
#include <cstdint>
#include "sycl-plugin.h"

namespace sycl {
    class DLL_API MemoryOps {
    public:
        size_t count{};
        void* ptr{};

        ~MemoryOps() {
            dealloc();
        }

        auto dealloc() -> void {
            if (ptr != nullptr) {
                free(ptr, handle_id);
                ptr = nullptr;
                count = 0;
            }
        }

        static auto alloc_device(size_t count, size_t elem_size, uint64_t handle_id) -> void*;
        static auto alloc_shared(size_t count, size_t elem_size, uint64_t handle_id) -> void*;
        static auto alloc_host(size_t count, size_t elem_size, uint64_t handle_id) -> void*;
        static auto memcpy(void* dst, const void* src, size_t bytes, uint64_t handle_id) -> void;
        static auto memset(void* ptr, int val, size_t bytes, uint64_t handle_id) -> void;
        static auto free(void* ptr, uint64_t handle_id) -> void;
    protected:
        MemoryOps() = default;
        uint64_t handle_id{};
    };

    /*!
     * @brief 设备内存
     * @tparam T 类型
     * @note host 不能直接访问地址
     */
    template<typename T>
    struct DLL_API DeviceMemory final : MemoryOps {
        explicit DeviceMemory(size_t count, const Device::Handle& handle) {
            this->handle_id = handle.id;
            this->ptr = alloc_device(count, sizeof(T), this->handle_id);
            this->count = count;
        }

        static auto alloc(size_t count, const Device::Handle& handle) -> DeviceMemory {
            DeviceMemory m;
            m.handle_id = handle.id;
            m.ptr = alloc_device(count, sizeof(T), m.handle_id);
            m.count = count;
            return m;
        }

        auto copy_from(const T* src, size_t count = 1) -> bool {
            if (count > this->count || (ptr == nullptr) || (src == nullptr)) {
                return false;
            }
            memcpy(ptr, const_cast<T*>(src), count * sizeof(T), handle_id);
            return true;
        }

        auto copy_to(T* dest, size_t count = 1) -> bool {
            if (count > this->count || (ptr == nullptr) || (dest == nullptr)) {
                return false;
            }
            memcpy(dest, ptr, count * sizeof(T), handle_id);
            return true;
        }

        auto clear() const -> void {
            if (ptr) {
                memset(ptr, 0, count * sizeof(T), handle_id);
            }
        }

        auto operator->() -> T* {
            return static_cast<T*>(ptr);
        }

        auto operator->() const -> const T* {
            return static_cast<T*>(ptr);
        }
    };

    /*!
     * @brief 共享内存
     * @tparam T 类型
     * @note host 能直接访问地址
     */
    template<typename T>
    struct DLL_API SharedMemory final : MemoryOps {
        explicit SharedMemory(size_t count, const Device::Handle& handle) {
            this->handle_id = handle.id;
            this->ptr = alloc_shared(count, sizeof(T), this->handle_id);
            this->count = count;
        }

        static auto alloc(size_t count, const Device::Handle& handle) -> SharedMemory {
            SharedMemory m;
            m.handle_id = handle.id;
            m.ptr = alloc_shared(count, sizeof(T), m.handle_id);
            m.count = count;
            return m;
        }

        auto clear() const -> void {
            if (ptr) {
                memset(ptr, 0, count * sizeof(T), handle_id);
            }
        }

        auto operator->() -> T* {
            return static_cast<T*>(ptr);
        }

        auto operator->() const -> const T* {
            return static_cast<T*>(ptr);
        }

        auto operator[](size_t index) -> T& {
            return static_cast<T*>(ptr)[index];
        }

        auto operator[](size_t index) const -> const T& {
            return static_cast<const T*>(ptr)[index];
        }
    };

    /*!
     * @brief 主机内存
     * @tparam T 类型
     * @note host 能直接访问地址
     */
    template<typename T>
    struct DLL_API HostMemory final : MemoryOps {
        explicit HostMemory(size_t count, const Device::Handle& handle) {
            this->handle_id = handle.id;
            this->ptr = alloc_host(count, sizeof(T), this->handle_id);
            this->count = count;
        }

        static auto alloc(size_t count, const Device::Handle& handle) -> HostMemory {
            HostMemory m;
            m.handle_id = handle.id;
            m.ptr = alloc_host(count, sizeof(T), m.handle_id);
            m.count = count;
            return m;
        }

        auto clear() -> void {
            if (ptr) {
                memset(ptr, 0, count * sizeof(T));
            }
        }

        auto operator->() -> T* {
            return static_cast<T*>(ptr);
        }

        auto operator->() const -> const T* {
            return static_cast<T*>(ptr);
        }

        auto operator[](size_t index) -> T& {
            return static_cast<T*>(ptr)[index];
        }

        auto operator[](size_t index) const -> const T& {
            return static_cast<const T*>(ptr)[index];
        }
    };
} // namespace sycl
