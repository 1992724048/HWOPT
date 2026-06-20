#include "Beardifier.hpp"
#include <algorithm>
#include <array>
#include <cmath>

using namespace minecraft::math;

static constexpr int BEARD_KERNEL_RADIUS = 12;
static constexpr int BEARD_KERNEL_SIZE = 24;
static std::array<float, BEARD_KERNEL_SIZE * BEARD_KERNEL_SIZE * BEARD_KERNEL_SIZE> beard_kernel{};

static auto init_kernel() -> bool {
    for (int zi = 0; zi < BEARD_KERNEL_SIZE; ++zi) {
        for (int xi = 0; xi < BEARD_KERNEL_SIZE; ++xi) {
            for (int yi = 0; yi < BEARD_KERNEL_SIZE; ++yi) {
                const int dx = xi - BEARD_KERNEL_RADIUS;
                const int dy = yi - BEARD_KERNEL_RADIUS;
                const int dz = zi - BEARD_KERNEL_RADIUS;
                const double distSqr = static_cast<double>(dx * dx + dz * dz) + (static_cast<double>(dy) + 0.5) * (static_cast<double>(dy) + 0.5);
                beard_kernel[(zi * BEARD_KERNEL_SIZE * BEARD_KERNEL_SIZE) + xi * BEARD_KERNEL_SIZE + yi] = static_cast<float>(std::exp(-distSqr / 16.0));
            }
        }
    }
    return true;
}

static const bool KERNEL_INIT = init_kernel();

static auto is_in_kernel_range(const int xi) -> bool {
    return xi >= 0 && xi < BEARD_KERNEL_SIZE;
}

static auto get_beard_contribution(const int dx, const int dy, const int dz, const int yToGround) -> double {
    const int xi = dx + BEARD_KERNEL_RADIUS;
    const int yi = dy + BEARD_KERNEL_RADIUS;
    const int zi = dz + BEARD_KERNEL_RADIUS;
    if (!is_in_kernel_range(xi) || !is_in_kernel_range(yi) || !is_in_kernel_range(zi)) {
        return 0.0;
    }
    const double dyWithOffset = static_cast<double>(yToGround) + 0.5;
    const double distanceSqr = static_cast<double>(dx * dx + dz * dz) + dyWithOffset * dyWithOffset;
    const double invSqrt = 1.0 / std::sqrt(distanceSqr / 2.0);
    const double value = -dyWithOffset * invSqrt / 2.0;
    return value * static_cast<double>(beard_kernel[zi * BEARD_KERNEL_SIZE * BEARD_KERNEL_SIZE + xi * BEARD_KERNEL_SIZE + yi]);
}

static auto get_bury_contribution(const double dx, const double dy, const double dz) -> double {
    const double length = std::sqrt(dx * dx + dy * dy + dz * dz);
    if (length <= 0.0) {
        return 1.0;
    }
    if (length >= 6.0) {
        return 0.0;
    }
    return 1.0 - (length / 6.0);
}

Beardifier::Beardifier() {
    JavaNative::touch();
}

auto Beardifier::add_methods() -> void {
    "Beardifier::batch_compute"_jf.reg<batch_compute>();
}

auto Beardifier::batch_compute(const int cell_start_block_x,
                               const int cell_start_block_y,
                               const int cell_start_block_z,
                               const int cell_width,
                               const int cell_height,
                               int* pieces_box,
                               const int pieces_box_len,
                               int* pieces_meta,
                               const int pieces_meta_len,
                               int* junctions_data,
                               const int junctions_data_len,
                               const int affected_min_x,
                               const int affected_min_y,
                               const int affected_min_z,
                               const int affected_max_x,
                               const int affected_max_y,
                               const int affected_max_z,
                               double* output,
                               const int output_len) -> void {
    const int pieceCount = pieces_box_len / 6;
    const int junctionCount = junctions_data_len / 4;

    int idx = 0;
    for (int iy = cell_height - 1; iy >= 0; --iy) {
        const int blockY = cell_start_block_y + iy;
        for (int ix = 0; ix < cell_width; ++ix) {
            const int blockX = cell_start_block_x + ix;
            for (int iz = 0; iz < cell_width; ++iz) {
                const int blockZ = cell_start_block_z + iz;

                double noise_value = 0.0;

                // Check affected box
                if (blockX >= affected_min_x && blockX <= affected_max_x && blockY >= affected_min_y && blockY <= affected_max_y && blockZ >= affected_min_z && blockZ <= affected_max_z) {
                    // Piece contributions
                    for (int p = 0; p < pieceCount; ++p) {
                        const int* box = pieces_box + p * 6;
                        const int* meta = pieces_meta + p * 2;
                        const int minX = box[0];
                        const int minY = box[1];
                        const int minZ = box[2];
                        const int maxX = box[3];
                        const int maxY = box[4];
                        const int maxZ = box[5];
                        const int terrainAdjustment = meta[0];
                        const int groundLevelDelta = meta[1];

                        const int dx = std::max({0, minX - blockX, blockX - maxX});
                        const int dz = std::max({0, minZ - blockZ, blockZ - maxZ});
                        const int groundY = minY + groundLevelDelta;
                        const int dyToGround = blockY - groundY;

                        int dy;
                        switch (terrainAdjustment) {
                            case 0:
                                dy = 0;
                                break; // NONE
                            case 1: // BURY
                            case 2:
                                dy = dyToGround;
                                break; // BEARD_THIN
                            case 3:
                                dy = std::max({0, groundY - blockY, blockY - maxY});
                                break; // BEARD_BOX
                            case 4:
                                dy = std::max({0, minY - blockY, blockY - maxY});
                                break; // ENCAPSULATE
                            default:
                                dy = 0;
                                break;
                        }

                        double contribution;
                        switch (terrainAdjustment) {
                            case 0:
                                contribution = 0.0;
                                break; // NONE
                            case 1:
                                contribution = get_bury_contribution(dx, static_cast<double>(dy) * 0.5, dz);
                                break; // BURY
                            case 2:
                            case 3:
                                contribution = get_beard_contribution(dx, dy, dz, dyToGround) * 0.8;
                                break;
                            case 4:
                                contribution = get_bury_contribution(static_cast<double>(dx) * 0.5, static_cast<double>(dy) * 0.5, static_cast<double>(dz) * 0.5) * 0.8;
                                break;
                            default:
                                contribution = 0.0;
                                break;
                        }
                        noise_value += contribution;
                    }

                    // Junction contributions
                    for (int j = 0; j < junctionCount; ++j) {
                        const int* jd = junctions_data + j * 4;
                        const int dx = blockX - jd[0];
                        const int dy = blockY - jd[3];
                        const int dz = blockZ - jd[2];
                        noise_value += get_beard_contribution(dx, dy, dz, dy) * 0.4;
                    }
                }

                output[idx++] = noise_value;
            }
        }
    }
}
