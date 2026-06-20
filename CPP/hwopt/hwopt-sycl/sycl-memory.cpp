#include "sycl-memory.h"
#include <shared_mutex>
#include <sycl.hpp>
#include <usm.hpp>
#include "sycl-state.h"

static auto resolve_queue(const uint64_t handle_id) -> sycl::queue* {
    std::shared_lock _(sycl::queue_mutex);
    const auto it = sycl::queue_map.find(handle_id);
    return it != sycl::queue_map.end() ? it->second.load().get() : nullptr;
}

auto sycl::MemoryOps::alloc_device(const size_t count, const size_t elem_size, const uint64_t handle_id) -> void* {
    const auto* q = resolve_queue(handle_id);
    return (q != nullptr) ? malloc_device(count * elem_size, *q) : nullptr;
}

auto sycl::MemoryOps::alloc_shared(const size_t count, const size_t elem_size, const uint64_t handle_id) -> void* {
    const auto* q = resolve_queue(handle_id);
    return (q != nullptr) ? malloc_shared(count * elem_size, *q) : nullptr;
}

auto sycl::MemoryOps::alloc_host(const size_t count, const size_t elem_size, const uint64_t handle_id) -> void* {
    const auto* q = resolve_queue(handle_id);
    return (q != nullptr) ? malloc_host(count * elem_size, *q) : nullptr;
}

auto sycl::MemoryOps::memcpy(void* dst, const void* src, const size_t bytes, const uint64_t handle_id) -> void {
    if (auto* q = resolve_queue(handle_id)) {
        q->memcpy(dst, src, bytes).wait();
    }
}

auto sycl::MemoryOps::memset(void* ptr, const int val, const size_t bytes, const uint64_t handle_id) -> void {
    auto* q = resolve_queue(handle_id);
    if (q != nullptr) {
        q->memset(ptr, val, bytes).wait();
    }
}

auto sycl::MemoryOps::free(void* ptr, const uint64_t handle_id) -> void {
    const auto* q = resolve_queue(handle_id);
    if (q != nullptr) {
        ::sycl::free(ptr, *q);
    }
}
