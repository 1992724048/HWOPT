// 遂沫 util.h
// 2026-02-09 01:08:17

#pragma once
#include <exception>
#include <utility>
#include <mimalloc/mimalloc.h>

namespace stdpp::util {
    template<typename T, typename... Args>
    static auto mi_new(Args&&... args) -> T* {
        void* ptr = mi_malloc(sizeof(T));
        if (ptr == nullptr) {
            throw std::bad_alloc();
        }

        try {
            return new(ptr) T(std::forward<Args>(args)...);
        } catch (...) {
            mi_free(ptr);
            throw;
        }
    }

    template<typename T>
    static auto mi_delete(const T* ptr) noexcept -> void {
        if (!ptr) {
            return;
        }

        auto* p = const_cast<T*>(ptr);
        p->~T();
        ::mi_free(p);
    }


    template<typename T>
    struct MiDeleter {
        auto operator()(T* ptr) const noexcept -> void {
            mi_delete(ptr);
        }
    };

    template<typename T, typename... Args>
    auto mi_make_unique(Args&&... args) -> std::unique_ptr<T, MiDeleter<T>> {
        return std::unique_ptr<T, MiDeleter<T>>{mi_new<T>(std::forward<Args>(args)...)};
    }

    template<typename T, typename... Args>
    auto mi_make_shared(Args&&... args) -> std::shared_ptr<T> {
        T* ptr = mi_new<T>(std::forward<Args>(args)...);

        try {
            return std::shared_ptr<T>(ptr, MiDeleter<T>{});
        } catch (...) {
            mi_delete(ptr);
            throw;
        }
    }
} // namespace stdpp::util
