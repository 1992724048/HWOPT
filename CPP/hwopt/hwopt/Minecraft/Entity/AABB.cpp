#include "AABB.hpp"
#include <algorithm>
#include <cmath>
#include <vector>

#include <tbb/blocked_range.h>
#include <tbb/parallel_for.h>
#include <tbb/parallel_sort.h>

#include <atomic>
#include <omp.h>

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
            case 0: // X
                if (moving_max_y <= sMinY || moving_min_y >= sMaxY || moving_max_z <= sMinZ || moving_min_z >= sMaxZ) {
                    continue;
                }
                if (distance > 0.0) {
                    const double d = sMinX - moving_max_x;
                    if (d >= -1.0E-7) {
                        distance = (std::min)(distance, d);
                    }
                } else {
                    const double d = sMaxX - moving_min_x;
                    if (d <= 1.0E-7) {
                        distance = (std::max)(distance, d);
                    }
                }
                break;
            case 1: // Y
                if (moving_max_x <= sMinX || moving_min_x >= sMaxX || moving_max_z <= sMinZ || moving_min_z >= sMaxZ) {
                    continue;
                }
                if (distance > 0.0) {
                    const double d = sMinY - moving_max_y;
                    if (d >= -1.0E-7) {
                        distance = (std::min)(distance, d);
                    }
                } else {
                    const double d = sMaxY - moving_min_y;
                    if (d <= 1.0E-7) {
                        distance = (std::max)(distance, d);
                    }
                }
                break;
            case 2: // Z
                if (moving_max_x <= sMinX || moving_min_x >= sMaxX || moving_max_y <= sMinY || moving_min_y >= sMaxY) {
                    continue;
                }
                if (distance > 0.0) {
                    const double d = sMinZ - moving_max_z;
                    if (d >= -1.0E-7) {
                        distance = (std::min)(distance, d);
                    }
                } else {
                    const double d = sMaxZ - moving_min_z;
                    if (d <= 1.0E-7) {
                        distance = (std::max)(distance, d);
                    }
                }
                break;
            default: ;
        }

        if (std::abs(distance) < 1.0E-7) {
            return 0.0;
        }
    }
    return distance;
}

struct EntityRef {
    double min_x;
    double max_x;
    double min_y;
    double max_y;
    double min_z;
    double max_z;
    int id;
};

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

    std::vector<EntityRef> sorted(entity_count);
    #pragma omp simd
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

    tbb::parallel_sort(sorted.begin(),
                       sorted.end(),
                       [](const EntityRef& a, const EntityRef& b) -> bool {
                           return a.min_x < b.min_x;
                       });

    if (entity_count > PARALLEL_THRESHOLD) {
        std::atomic ac{0};
        parallel_for(tbb::blocked_range(0, entity_count),
                     [&](const tbb::blocked_range<int>& range) -> void {
                         for (int i = range.begin(); i < range.end(); i++) {
                             const auto& a = sorted[i];
                             for (int j = i + 1; j < entity_count; j++) {
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
                                 const int p = ac.fetch_add(1, std::memory_order_relaxed);
                                 if (p < max_collisions) {
                                     output_a[p] = a.id;
                                     output_b[p] = b.id;
                                 }
                             }
                         }
                     });
        const int result = ac.load(std::memory_order_relaxed);
        return result < max_collisions ? result : max_collisions;
    }

    int collision_count = 0;

    for (int i = 0; i < entity_count; i++) {
        const auto& a = sorted[i];

        for (int j = i + 1; j < entity_count; j++) {
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

            if (collision_count < max_collisions) {
                output_a[collision_count] = a.id;
                output_b[collision_count] = b.id;
                collision_count++;
            }
        }
    }

    return collision_count;
}
