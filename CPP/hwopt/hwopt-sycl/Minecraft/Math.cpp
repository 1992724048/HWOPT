#include "Math.hpp"
#include "sycl-state.h"

namespace minecraft::math::sycl {
    auto Math::batch_trilerp(const ::sycl::Device::Handle& device,
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
                             const ::sycl::HostMemory<double>& output) -> void {
        auto& q = *::sycl::queue_map[device.id].load();
        const auto w = static_cast<size_t>(cell_width);
        const auto h = static_cast<size_t>(cell_height);
        auto* out = static_cast<double*>(output.ptr);

        q.submit([&](::sycl::handler& cgh) -> void {
            cgh.parallel_for(::sycl::range(h, w, w),
                             [=](::sycl::id<3> id) -> void {
                                 const auto iy = id[0];
                                 const auto ix = id[1];
                                 const auto iz = id[2];

                                 const double dy = static_cast<double>(iy) / cell_height;
                                 const double ly = 1.0 - dy;
                                 const double hy = dy;
                                 const double v00 = n000 * ly + n010 * hy;
                                 const double v10 = n100 * ly + n110 * hy;
                                 const double v01 = n001 * ly + n011 * hy;
                                 const double v11 = n101 * ly + n111 * hy;

                                 const double dx = static_cast<double>(ix) / cell_width;
                                 const double lx = 1.0 - dx;
                                 const double hx = dx;
                                 const double z0 = v00 * lx + v10 * hx;
                                 const double z1 = v01 * lx + v11 * hx;

                                 const double dz = static_cast<double>(iz) / cell_width;
                                 out[iy * w * w + ix * w + iz] = z0 + dz * (z1 - z0);
                             });
        }).wait();
    }
} // namespace minecraft::math::sycl
