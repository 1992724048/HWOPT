#pragma once
#include <cstdint>
#include <ipp/ippdc.h>
#include "../../JavaNative.hpp"

namespace minecraft::compress {
    class HWOPT_API Compress final : JavaNative<Compress> {
    public:
        Compress();
        static auto add_methods() -> void;

        static auto compress(const uint8_t* input, int input_len, uint8_t* output, int output_len) -> int;
        static auto decompress(const uint8_t* input, int input_len, uint8_t* output, int output_len) -> int;
    };
} // namespace minecraft::compress
