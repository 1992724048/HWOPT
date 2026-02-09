// 遂沫 NoiseChunkGenerator.cpp
// 2026-02-08 22:45:49

#include "NoiseChunkGenerator.h"

#include <stdexcept>
#include <FastNoiseLite/FastNoiseLite.h>

minecraft::NoiseChunkGenerator::NoiseChunkGenerator() {
    JavaNative::touch();
}

static FastNoiseLite noise;

auto minecraft::NoiseChunkGenerator::add_methods() -> void {
    "NoiseChunkGenerator::get_interpolated_state"_jf.reg<get_interpolated_state>();
}

auto minecraft::NoiseChunkGenerator::get_interpolated_state(short* array, const int array_size, const int x_size, const int y_size, const int z_size) -> void try {
    if (!array) {
        return;
    }
    if (array_size < x_size * y_size * z_size) {
        return;
    }

    const int stride_xz = x_size;
    for (int y = 0; y < y_size; ++y) {
        for (int z = 0; z < z_size; ++z) {
            for (int x = 0; x < x_size; ++x) {
                constexpr int base_height = 64;
                constexpr float amplitude = 20.0f;
                const int idx = x + stride_xz * (z + z_size * y);

                const auto wx = static_cast<float>(x);
                const auto wz = static_cast<float>(z);

                const float n = noise.GetNoise(wx, wz);
                if (y <= static_cast<int>(base_height + n * amplitude)) {
                    array[idx] = 411;
                } else {
                    array[idx] = 20;
                }
            }
        }
    }
} catch (const std::exception& exception) {
    ELOG << "[" << GetCurrentThreadId() << "] " << exception.what();
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << "[" << GetCurrentThreadId() << "] " << std::hex << exception.code() << " " << stdpp::encode::gbk_to_utf8(exception.what());
}
