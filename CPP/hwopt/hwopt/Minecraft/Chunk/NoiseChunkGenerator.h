#pragma once
#include "../../JavaNative.h"

namespace minecraft {
    class NoiseChunkGenerator : JavaNative<NoiseChunkGenerator> {
    public:
        NoiseChunkGenerator();

        static auto add_methods() -> void;
    private:
        static void get_interpolated_state(short* array, int array_size, int x_size, int y_size, int z_size);
    };
}
