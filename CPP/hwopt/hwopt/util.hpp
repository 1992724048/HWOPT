#pragma once

#include <utility>
#include <mimalloc/mimalloc.h>

namespace hwopt::util {
    template<typename T, typename... Args>
    auto mi_new(Args&&... args) -> T* {
        void* mem = mi_malloc(sizeof(T));
        if (!mem) {
            return nullptr;
        }
        return ::new(mem) T(std::forward<Args>(args)...);
    }

    template<typename T>
    auto mi_delete(const T* ptr) -> void {
        if (!ptr) {
            return;
        }
        const_cast<T*>(ptr)->~T();
        mi_free(const_cast<void*>(static_cast<const void*>(ptr)));
    }
} // namespace hwopt::util
