#include "Math.hpp"
#include <array>
#include <windows.h>
using namespace minecraft::math;

Math::Math() {
    JavaNative::touch();
}

auto Math::add_methods() -> void {
    "Math::lerp3"_jf.reg<lerp3>();
    "Math::ray_intersects_aabb"_jf.reg<ray_intersects_aabb_wrapper>();
    "Math::atan2"_jf.reg<atan2>();
    "Math::clamped_lerp"_jf.reg<clamped_lerp>();
    "Math::batch_trilerp"_jf.reg<batch_trilerp>();
}

auto Math::smoothstep(const double x) -> double {
    return ((((x * 6.0) - 15.0) * x) + 10.0) * x * x * x;
}

auto Math::smoothstep_derivative(const double x) -> double {
    const double t = x * (x - 1.0);
    return 30.0 * t * t;
}

auto Math::lerp(const double alpha1, const double p0, const double p1) -> double {
    return p0 + (alpha1 * (p1 - p0));
}

auto Math::lerp2(const double alpha1, const double alpha2, const double x00, const double x10, const double x01, const double x11) -> double {
    return lerp(alpha2, lerp(alpha1, x00, x10), lerp(alpha1, x01, x11));
}

auto Math::lerp3(const double alpha1,
                 const double alpha2,
                 const double alpha3,
                 const double x000,
                 const double x100,
                 const double x010,
                 const double x110,
                 const double x001,
                 const double x101,
                 const double x011,
                 const double x111) -> double {
    return lerp(alpha3, lerp2(alpha1, alpha2, x000, x100, x010, x110), lerp2(alpha1, alpha2, x001, x101, x011, x111));
}

auto Math::atan2(const double y, const double x) -> double {
    return std::atan2(y, x);
}

auto Math::ray_intersects_aabb(const glm::vec3 ray_start, const glm::vec3 ray_dir, const aabb::AABB& aabb) -> bool {
    const double centerX = (aabb.min_x + aabb.max_x) * static_cast<double>(0.5F);
    const double boxExtentX = (aabb.max_x - aabb.min_x) * static_cast<double>(0.5F);
    const double diffX = ray_start.x - centerX;
    if (std::abs(diffX) > boxExtentX && diffX * ray_dir.x >= static_cast<double>(0.0F)) {
        return false;
    }
    const double centerY = (aabb.min_y + aabb.max_y) * static_cast<double>(0.5F);
    const double boxExtentY = (aabb.max_y - aabb.min_y) * static_cast<double>(0.5F);
    const double diffY = ray_start.y - centerY;
    if (std::abs(diffY) > boxExtentY && diffY * ray_dir.y >= static_cast<double>(0.0F)) {
        return false;
    }
    const double centerZ = (aabb.min_z + aabb.max_z) * static_cast<double>(0.5F);
    const double boxExtentZ = (aabb.max_z - aabb.min_z) * static_cast<double>(0.5F);
    const double diffZ = ray_start.z - centerZ;
    if (std::abs(diffZ) > boxExtentZ && diffZ * ray_dir.z >= static_cast<double>(0.0F)) {
        return false;
    }
    const double andrewWooDiffX = std::abs(ray_dir.x);
    const double andrewWooDiffY = std::abs(ray_dir.y);
    const double andrewWooDiffZ = std::abs(ray_dir.z);
    double f = (ray_dir.y * diffZ) - (ray_dir.z * diffY);
    if (std::abs(f) > (boxExtentY * andrewWooDiffZ) + (boxExtentZ * andrewWooDiffY)) {
        return false;
    }
    f = (ray_dir.z * diffX) - (ray_dir.x * diffZ);
    if (std::abs(f) > (boxExtentX * andrewWooDiffZ) + (boxExtentZ * andrewWooDiffX)) {
        return false;
    }
    f = (ray_dir.x * diffY) - (ray_dir.y * diffX);
    return std::abs(f) < (boxExtentX * andrewWooDiffY) + (boxExtentY * andrewWooDiffX);
}

auto Math::clamped_lerp(const double factor, const double min, const double max) -> double {
    if (factor < static_cast<double>(0.0F)) {
        return min;
    }
    return factor > static_cast<double>(1.0F) ? max : lerp(factor, min, max);
}

auto Math::batch_trilerp(const double n000,
                         const double n100,
                         const double n010,
                         const double n110,
                         const double n001,
                         const double n101,
                         const double n011,
                         const double n111,
                         const int cell_width,
                         const int cell_height,
                         double* __restrict output,
                         const int output_len) -> void {
    int idx = 0;
    std::array<double, 16> x_fracs{};
    std::array<double, 16> z_fracs{};

    [[assume(cell_width > 0 && cell_width <= 16)]];
    [[assume(cell_height > 0 && cell_height <= 16)]];

    #pragma omp simd
    #pragma unroll
    #pragma loop_count max(16)
    for (int i = 0; i < cell_width; ++i) {
        const double f = static_cast<double>(i) / static_cast<double>(cell_width);
        x_fracs[i] = f;
        z_fracs[i] = f;
    }

    #pragma omp simd
    #pragma unroll
    #pragma loop_count max(16)
    for (int iy = cell_height - 1; iy >= 0; --iy) {
        const double dy = static_cast<double>(iy) / static_cast<double>(cell_height);
        const double ly = 1.0 - dy;
        const double hy = dy;
        const double v00 = (n000 * ly) + (n010 * hy);
        const double v10 = (n100 * ly) + (n110 * hy);
        const double v01 = (n001 * ly) + (n011 * hy);
        const double v11 = (n101 * ly) + (n111 * hy);

        #pragma omp simd
        #pragma unroll

        for (int ix = 0; ix < cell_width; ++ix) {
            const double dx = x_fracs[ix];
            const double lx = 1.0 - dx;
            const double hx = dx;
            const double z0 = (v00 * lx) + (v10 * hx);
            const double z1 = (v01 * lx) + (v11 * hx);

            #pragma omp simd
            #pragma unroll
            #pragma loop_count max(16)
            for (int iz = 0; iz < cell_width; ++iz) {
                output[idx++] = z0 + (z_fracs[iz] * (z1 - z0));
            }
        }
    }
}

auto Math::ray_intersects_aabb_wrapper(const double sx,
                                       const double sy,
                                       const double sz,
                                       const double dx,
                                       const double dy,
                                       const double dz,
                                       const double minX,
                                       const double minY,
                                       const double minZ,
                                       const double maxX,
                                       const double maxY,
                                       const double maxZ) -> bool {
    aabb::AABB box;
    box.min_x = minX;
    box.min_y = minY;
    box.min_z = minZ;
    box.max_x = maxX;
    box.max_y = maxY;
    box.max_z = maxZ;
    return ray_intersects_aabb(glm::vec3(sx, sy, sz), glm::vec3(dx, dy, dz), box);
}
