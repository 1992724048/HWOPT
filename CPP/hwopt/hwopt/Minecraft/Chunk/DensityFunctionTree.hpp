#pragma once
#include <vector>
#include "../Math.hpp"
#include "../../JavaNative.hpp"
#include "Noise/BlendedNoise.hpp"
#include "Noise/NormalNoise.hpp"

namespace minecraft::dftree {
    enum NodeType : int8_t {
        CONSTANT      = 0,
        ADD           = 1,
        MUL           = 2,
        MIN           = 3,
        MAX           = 4,
        TRANSFORM     = 5,
        RANGE_CHOICE  = 6,
        BLENDED_NOISE = 7,
        NOISE         = 8,
        Y_GRADIENT    = 9,
    };

    static constexpr int NODE_FLOATS = 5;

    class HWOPT_API DensityFunctionTree final : public JavaNative<DensityFunctionTree> {
    public:
        DensityFunctionTree();
        static auto add_methods() -> void;

        static auto evaluate_node(const double* nodes,
                                  int node_idx,
                                  noise::BlendedNoise* const* bn_ptrs,
                                  int bn_count,
                                  noise::NormalNoise* const* noise_ptrs,
                                  int noise_count,
                                  double x,
                                  double y,
                                  double z) -> double;

        static auto compute_densities_batch(const double* nodes,
                                            int nodes_len,
                                            long long* bn_ptrs,
                                            int bn_ptrs_len,
                                            long long* noise_ptrs,
                                            int noise_ptrs_len,
                                            int min_x,
                                            int min_y,
                                            int min_z,
                                            int size_x,
                                            int size_y,
                                            int size_z,
                                            double* output,
                                            int output_len) -> void;
    };
} // namespace minecraft::dftree
