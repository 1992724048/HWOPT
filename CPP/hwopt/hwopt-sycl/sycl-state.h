#pragma once

#include <atomic>
#include <memory>
#include <shared_mutex>
#include <sycl.hpp>
#include <unordered_map>

namespace sycl {
    extern std::shared_mutex queue_mutex;
    extern std::atomic<uint64_t> id_gen;
    extern std::unordered_map<uint64_t, std::atomic<std::shared_ptr<::sycl::queue>>> queue_map;
} // namespace sycl
