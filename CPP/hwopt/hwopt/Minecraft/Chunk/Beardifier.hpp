#pragma once
#include "../../JavaNative.hpp"

namespace minecraft::math {
    class HWOPT_API Beardifier final : JavaNative<Beardifier> {
    public:
        Beardifier();

        static auto add_methods() -> void;
        static auto batch_compute(int cell_start_block_x,
                                  int cell_start_block_y,
                                  int cell_start_block_z,
                                  int cell_width,
                                  int cell_height,
                                  int* pieces_box,
                                  int pieces_box_len,
                                  int* pieces_meta,
                                  int pieces_meta_len,
                                  int* junctions_data,
                                  int junctions_data_len,
                                  int affected_min_x,
                                  int affected_min_y,
                                  int affected_min_z,
                                  int affected_max_x,
                                  int affected_max_y,
                                  int affected_max_z,
                                  double* output,
                                  int output_len) -> void;
    };
} // namespace minecraft::math
