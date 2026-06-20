#pragma once
#include "sycl-memory.h"

namespace minecraft::math::sycl {
    class DLL_API Math final {
    public:
        static auto batch_trilerp(const ::sycl::Device::Handle& device,
                                  double n000,
                                  double n100,
                                  double n010,
                                  double n110,
                                  double n001,
                                  double n101,
                                  double n011,
                                  double n111,
                                  int cell_width,
                                  int cell_height,
                                  const ::sycl::HostMemory<double>& output) -> void;
    };
} // namespace minecraft::math::sycl
