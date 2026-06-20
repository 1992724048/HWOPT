#pragma once

#include "sycl-plugin.h"

namespace hwopt::global {
    inline sycl::Device::Handle handle{0};
} // namespace hwopt::global

DLL_API auto init_sycl_device() -> void;
