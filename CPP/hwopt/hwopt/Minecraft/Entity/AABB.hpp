#pragma once

#include "../../JavaNative.hpp"

namespace minecraft::aabb {
    class HWOPT_API AABB final : JavaNative<AABB> {
    public:
        static constexpr int PARALLEL_THRESHOLD = 1536;

        double min_x;
        double min_y;
        double min_z;
        double max_x;
        double max_y;
        double max_z;

        AABB();

        static auto add_methods() -> void;

        static auto batch_collide_axis(int axis,
                                       double moving_min_x,
                                       double moving_min_y,
                                       double moving_min_z,
                                       double moving_max_x,
                                       double moving_max_y,
                                       double moving_max_z,
                                       double* stationary_boxes,
                                       int stationary_boxes_len,
                                       double initial_distance) -> double;

        static auto batch_find_collisions(const double* aabbs, int aabbs_len, int* output_a, int output_a_len, int* output_b, int output_b_len, int entity_count, int max_collisions) -> int;
    };
} // namespace minecraft::aabb
