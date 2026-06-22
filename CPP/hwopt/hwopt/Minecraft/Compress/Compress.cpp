#include "Compress.hpp"
#include <zstd.h>
#include <cstring>

#include "compress/clevels.h"
using namespace minecraft::compress;

Compress::Compress() {
    JavaNative::touch();
}

auto Compress::add_methods() -> void {
    "Compress::compress"_jf.reg<&compress>();
    "Compress::decompress"_jf.reg<&decompress>();
}

auto Compress::compress(const uint8_t* input, const int input_len, uint8_t* output, const int output_len) -> int {
    if (input_len <= 0 || output_len < input_len + 8) {
        return -1;
    }
    const size_t dst_len = ZSTD_compress(output, static_cast<size_t>(output_len), input, static_cast<size_t>(input_len), ZSTD_MAX_CLEVEL);
    if (ZSTD_isError(dst_len) != 0U) {
        return -1;
    }
    return static_cast<int>(dst_len);
}

auto Compress::decompress(const uint8_t* input, const int input_len, uint8_t* output, const int output_len) -> int {
    if (input_len <= 0 || output_len <= 0) {
        return -1;
    }
    const size_t dst_len = ZSTD_decompress(output, static_cast<size_t>(output_len), input, static_cast<size_t>(input_len));
    if (ZSTD_isError(dst_len) != 0U) {
        return -1;
    }
    return static_cast<int>(dst_len);
}
