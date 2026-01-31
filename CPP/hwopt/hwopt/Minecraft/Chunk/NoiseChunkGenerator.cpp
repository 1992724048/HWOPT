#include "NoiseChunkGenerator.h"

#include <stdexcept>
#include <FastNoiseLite/FastNoiseLite.h>

minecraft::NoiseChunkGenerator::NoiseChunkGenerator() {
    JavaNative::touch();
}

static FastNoiseLite noise;

auto minecraft::NoiseChunkGenerator::add_methods() -> void {
    register_method<get_interpolated_state>("NoiseChunkGenerator::get_interpolated_state");
    noise.SetNoiseType(FastNoiseLite::NoiseType_OpenSimplex2);
    noise.SetFrequency(0.01f);
    noise.SetFractalType(FastNoiseLite::FractalType_FBm);
    noise.SetFractalOctaves(4);
    noise.SetFractalLacunarity(2.0f);
    noise.SetFractalGain(0.5f);
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

                const float wx = static_cast<float>(x);
                const float wz = static_cast<float>(z);

                const float n = noise.GetNoise(wx, wz);
                const int height = static_cast<int>(base_height + n * amplitude);

                if (y <= height) {
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
