#include "AABB.hpp"
#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <vector>

#include <omp.h>
#include <tbb/parallel_sort.h>

#include "mimalloc/mimalloc.h"

#include "stdpp/util.h"

using namespace minecraft::aabb;

AABB::AABB() {
    JavaNative::touch();
}

auto AABB::add_methods() -> void {
    "AABB::batch_collide_axis"_jf.reg<batch_collide_axis>();
    "AABB::batch_find_collisions"_jf.reg<batch_find_collisions>();
}

auto AABB::batch_collide_axis(const int axis,
                              const double moving_min_x,
                              const double moving_min_y,
                              const double moving_min_z,
                              const double moving_max_x,
                              const double moving_max_y,
                              const double moving_max_z,
                              double* stationary_boxes,
                              const int stationary_boxes_len,
                              const double initial_distance) -> double {
    if (std::abs(initial_distance) < 1.0E-7) {
        return 0.0;
    }

    const int count = stationary_boxes_len / 6;
    double distance = initial_distance;

    for (int i = 0; i < count; i++) {
        const double* b = stationary_boxes + (i * 6);
        const double sMinX = b[0];
        const double sMinY = b[1];
        const double sMinZ = b[2];
        const double sMaxX = b[3];
        const double sMaxY = b[4];
        const double sMaxZ = b[5];

        switch (axis) {
            case 0:
                if (moving_max_y <= sMinY || moving_min_y >= sMaxY || moving_max_z <= sMinZ || moving_min_z >= sMaxZ) {
                    continue;
                }
                if (distance > 0.0) {
                    const double d = sMinX - moving_max_x;
                    if (d >= -1.0E-7 && d < distance) {
                        distance = d;
                    }
                } else {
                    const double d = sMaxX - moving_min_x;
                    if (d <= 1.0E-7 && d > distance) {
                        distance = d;
                    }
                }
                break;
            case 1:
                if (moving_max_x <= sMinX || moving_min_x >= sMaxX || moving_max_z <= sMinZ || moving_min_z >= sMaxZ) {
                    continue;
                }
                if (distance > 0.0) {
                    const double d = sMinY - moving_max_y;
                    if (d >= -1.0E-7 && d < distance) {
                        distance = d;
                    }
                } else {
                    const double d = sMaxY - moving_min_y;
                    if (d <= 1.0E-7 && d > distance) {
                        distance = d;
                    }
                }
                break;
            default:
                if (moving_max_x <= sMinX || moving_min_x >= sMaxX || moving_max_y <= sMinY || moving_min_y >= sMaxY) {
                    continue;
                }
                if (distance > 0.0) {
                    const double d = sMinZ - moving_max_z;
                    if (d >= -1.0E-7 && d < distance) {
                        distance = d;
                    }
                } else {
                    const double d = sMaxZ - moving_min_z;
                    if (d <= 1.0E-7 && d > distance) {
                        distance = d;
                    }
                }
                break;
        }
        if (std::abs(distance) < 1.0E-7) {
            return 0.0;
        }
    }
    return distance;
}

struct EntityRef {
    double min_x, max_x, min_y, max_y, min_z, max_z;
    int id;
};

namespace {
    auto sweep_seq(const int n, const int max_collisions, const EntityRef* sorted, int* out_a, int* out_b) -> int {
        int total = 0;
        for (int i = 0; i < n && total < max_collisions; i++) {
            const auto& a = sorted[i];
            for (int j = i + 1; j < n; j++) {
                const auto& b = sorted[j];
                if (b.min_x > a.max_x) {
                    break;
                }
                if (a.max_y <= b.min_y || a.min_y >= b.max_y) {
                    continue;
                }
                if (a.max_z <= b.min_z || a.min_z >= b.max_z) {
                    continue;
                }
                out_a[total] = a.id;
                out_b[total] = b.id;
                if (++total >= max_collisions) {
                    return total;
                }
            }
        }
        return total;
    }
} // namespace

auto AABB::batch_find_collisions(const double* aabbs,
                                 const int aabbs_len,
                                 int* output_a,
                                 const int output_a_len,
                                 int* output_b,
                                 const int output_b_len,
                                 const int entity_count,
                                 const int max_collisions) -> int {
    if (entity_count < 2) {
        return 0;
    }

    std::vector<EntityRef, mi_stl_allocator<EntityRef>> sorted(entity_count);
    for (int i = 0; i < entity_count; i++) {
        const double* b = aabbs + (i * 6);
        sorted[i].min_x = b[0];
        sorted[i].max_x = b[3];
        sorted[i].min_y = b[1];
        sorted[i].max_y = b[4];
        sorted[i].min_z = b[2];
        sorted[i].max_z = b[5];
        sorted[i].id = i;
    }

    if (entity_count > 500) {
        tbb::parallel_sort(sorted.begin(),
                           sorted.end(),
                           [](const EntityRef& a, const EntityRef& b) -> bool {
                               return a.min_x < b.min_x;
                           });
    } else {
        std::ranges::sort(sorted,
                          [](const EntityRef& a, const EntityRef& b) -> bool {
                              return a.min_x < b.min_x;
                          });
    }
    return sweep_seq(entity_count, max_collisions, sorted.data(), output_a, output_b);
}
