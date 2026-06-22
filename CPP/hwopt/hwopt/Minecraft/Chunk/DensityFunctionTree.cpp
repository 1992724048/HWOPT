#include "DensityFunctionTree.hpp"

#include <tbb/tbb.h>

using namespace minecraft::dftree;

DensityFunctionTree::DensityFunctionTree() {
    JavaNative::touch();
}

auto DensityFunctionTree::add_methods() -> void {
    "DensityFunctionTree::compute_densities_batch"_jf.reg<compute_densities_batch>();
}

auto DensityFunctionTree::evaluate_node(const double* nodes,
                                        const int node_idx,
                                        noise::BlendedNoise* const* bn_ptrs,
                                        const int bn_count,
                                        noise::NormalNoise* const* noise_ptrs,
                                        const int noise_count,
                                        const double x,
                                        const double y,
                                        const double z) -> double {
    const double* n = nodes + node_idx * NODE_FLOATS;
    const int type = static_cast<int>(n[0]);
    const int c0 = static_cast<int>(n[1]);
    const int c1 = static_cast<int>(n[2]);

    switch (type) {
        case CONSTANT:
            return n[3];
        case ADD:
            return evaluate_node(nodes, c0, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z) + evaluate_node(nodes, c1, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z);
        case MUL:
            return evaluate_node(nodes, c0, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z) * evaluate_node(nodes, c1, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z);
        case MIN:
            return std::min(evaluate_node(nodes, c0, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z), evaluate_node(nodes, c1, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z));
        case MAX:
            return std::max(evaluate_node(nodes, c0, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z), evaluate_node(nodes, c1, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z));
        case TRANSFORM:
            return (evaluate_node(nodes, c0, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z) * n[3]) + n[4];
        case RANGE_CHOICE: {
            const double input = evaluate_node(nodes, c0, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z);
            const int c2 = static_cast<int>(n[4]);
            if (input < n[3] || input > n[4]) {
                return evaluate_node(nodes, c2, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z);
            }
            return evaluate_node(nodes, c1, bn_ptrs, bn_count, noise_ptrs, noise_count, x, y, z);
        }
        case BLENDED_NOISE: {
            const int id = static_cast<int>(n[3]);
            if (id >= 0 && id < bn_count && (bn_ptrs[id] != nullptr)) {
                return bn_ptrs[id]->compute(x, y, z);
            }
            return 0.0;
        }
        case NOISE: {
            const int id = static_cast<int>(n[3]);
            if (id >= 0 && id < noise_count && (noise_ptrs[id] != nullptr)) {
                return noise_ptrs[id]->get_value(x * n[4], y * n[4], z * n[4]);
            }
            return 0.0;
        }
        case Y_GRADIENT: {
            const double minY = n[1];
            const double maxY = n[2];
            const double minV = n[3];
            const double maxV = n[4];
            if (y <= minY) {
                return minV;
            }
            if (y >= maxY) {
                return maxV;
            }
            const double t = (y - minY) / (maxY - minY);
            return minV + (t * (maxV - minV));
        }
        default:
            return 0.0;
    }
}

auto DensityFunctionTree::compute_densities_batch(const double* nodes,
                                                  int nodes_len,
                                                  long long* bn_ptrs,
                                                  const int bn_ptrs_len,
                                                  long long* noise_ptrs,
                                                  const int noise_ptrs_len,
                                                  const int min_x,
                                                  const int min_y,
                                                  const int min_z,
                                                  const int size_x,
                                                  const int size_y,
                                                  const int size_z,
                                                  double* output,
                                                  const int output_len) -> void {
    const int total = size_x * size_y * size_z;
    if (output_len < total) {
        return;
    }

    tbb::parallel_for(0,
                      total,
                      [&](const int i) -> void {
                          const int x = min_x + (i % size_x);
                          const int z = min_z + ((i / size_x) % size_z);
                          const int y = min_y + (i / (size_x * size_z));
                          output[i] = evaluate_node(nodes,
                                                    0,
                                                    reinterpret_cast<noise::BlendedNoise**>(bn_ptrs),
                                                    bn_ptrs_len,
                                                    reinterpret_cast<noise::NormalNoise**>(noise_ptrs),
                                                    noise_ptrs_len,
                                                    x,
                                                    y,
                                                    z);
                      });
}
