#pragma once

#include "../JavaNative.hpp"

namespace minecraft::aabb {
    class AABB final : JavaNative<AABB> {
    public:
        double minX;
        double minY;
        double minZ;
        double maxX;
        double maxY;
        double maxZ;

        AABB();

        static auto add_methods() -> void;
    };
} // namespace minecraft::aabb
