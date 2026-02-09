// 遂沫 sycl-queue.h
// 2026-02-10 01:00:35

#pragma once
#include <sycl.hpp>

extern phmap::parallel_node_hash_map<int, std::atomic<std::shared_ptr<sycl::queue>>> queue_map;
