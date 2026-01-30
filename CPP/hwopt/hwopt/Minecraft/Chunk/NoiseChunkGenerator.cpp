#include "NoiseChunkGenerator.h"

#include <stdexcept>

minecraft::NoiseChunkGenerator::NoiseChunkGenerator() {
    JavaNative::touch();
}

auto minecraft::NoiseChunkGenerator::add_methods() -> void {
    register_method<get_interpolated_state>("NoiseChunkGenerator::get_interpolated_state");
}

auto minecraft::NoiseChunkGenerator::get_interpolated_state(short* array, const int array_size, const int x_size, const int y_size, const int z_size) -> void try {
    if (!array) {
        return;
    }

    if (array_size < x_size * y_size * z_size) {
        return;
    }

    const int strideXZ = x_size;
    const int strideY = x_size * z_size;

    for (int y = 0; y < y_size; ++y) {
        for (int z = 0; z < z_size; ++z) {
            for (int x = 0; x < x_size; ++x) {
                const int idx = x + strideXZ * (z + z_size * y);

                if (y - 65 > 80) {
                    array[idx] = 20;
                } else {
                    array[idx] = 411;
                }
            }
        }
    }
} catch (const std::exception& exception) {
    ELOG << "[" << GetCurrentThreadId() << "] " << exception.what();
} catch (const stdpp::exception::NativeException& exception) {
    ELOG << "[" << GetCurrentThreadId() << "] " << std::hex << exception.code() << " " << stdpp::encode::gbk_to_utf8(exception.what());
}
