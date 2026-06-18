#include "Math.hpp"

using namespace minecraft::math;

Math::Math() {
    JavaNative::touch();
}

auto Math::add_methods() -> void {
    "Math::lerp3"_jf.reg<lerp3>();
    "Math::ray_intersects_aabb"_jf.reg<ray_intersects_aabb_wrapper>();
    "Math::atan2"_jf.reg<atan2>();
    "Math::clamped_lerp"_jf.reg<clamped_lerp>();
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
    const double centerX = (aabb.minX + aabb.maxX) * static_cast<double>(0.5F);
    const double boxExtentX = (aabb.maxX - aabb.minX) * static_cast<double>(0.5F);
    const double diffX = ray_start.x - centerX;
    if (std::abs(diffX) > boxExtentX && diffX * ray_dir.x >= static_cast<double>(0.0F)) {
        return false;
    }
    const double centerY = (aabb.minY + aabb.maxY) * static_cast<double>(0.5F);
    const double boxExtentY = (aabb.maxY - aabb.minY) * static_cast<double>(0.5F);
    const double diffY = ray_start.y - centerY;
    if (std::abs(diffY) > boxExtentY && diffY * ray_dir.y >= static_cast<double>(0.0F)) {
        return false;
    }
    const double centerZ = (aabb.minZ + aabb.maxZ) * static_cast<double>(0.5F);
    const double boxExtentZ = (aabb.maxZ - aabb.minZ) * static_cast<double>(0.5F);
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
    box.minX = minX;
    box.minY = minY;
    box.minZ = minZ;
    box.maxX = maxX;
    box.maxY = maxY;
    box.maxZ = maxZ;
    return ray_intersects_aabb(glm::vec3(sx, sy, sz), glm::vec3(dx, dy, dz), box);
}
