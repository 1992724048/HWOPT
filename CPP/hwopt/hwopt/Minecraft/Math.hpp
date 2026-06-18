#pragma once
#include "AABB.hpp"
#include "../JavaNative.hpp"
#include "glm/vec3.hpp"

namespace minecraft::math {
    class Math final : JavaNative<Math> {
    public:
        Math();
        static auto add_methods() -> void;

        static auto smoothstep(double x) -> double;

        static auto smoothstep_derivative(double x) -> double;

        static auto lerp(double alpha1, double p0, double p1) -> double;

        static auto lerp2(double alpha1, double alpha2, double x00, double x10, double x01, double x11) -> double;

        static auto lerp3(double alpha1, double alpha2, double alpha3, double x000, double x100, double x010, double x110, double x001, double x101, double x011, double x111) -> double;

        static auto atan2(double y, double x) -> double;

        static auto ray_intersects_aabb(glm::vec3 ray_start, glm::vec3 ray_dir, const aabb::AABB& aabb) -> bool;

        static auto clamped_lerp(double factor, double min, double max) -> double;
    private:
        static auto ray_intersects_aabb_wrapper(double sx, double sy, double sz, double dx, double dy, double dz, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) -> bool;
    };
} // namespace minecraft::math
