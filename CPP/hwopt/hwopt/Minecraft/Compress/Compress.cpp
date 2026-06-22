#include "Compress.hpp"
#include <algorithm>
#include <array>
#include <cstring>
#include <ipp/ipp.h>
#include <ipp/ippdc.h>
#include <mimalloc/mimalloc.h>
using namespace minecraft::compress;

namespace {
    thread_local struct {
        Ipp8u* bwt_buf = nullptr;
        int bwt_cap = 0;
        Ipp8u* s1 = nullptr;
        int s1_cap = 0;
        Ipp8u* s2 = nullptr;
        int s2_cap = 0;
        Ipp16u* s3 = nullptr;
        int s3_cap = 0;
        void* mtf_state = nullptr;
        int mtf_size = 0;
        void* rle_state = nullptr;
        int rle_size = 0;
        void* enc_huff = nullptr;
        int enc_huff_size = 0;
        bool ok = false;
    } g;

    auto ensure(const int n) -> void {
        const int need = std::max(n, 65536);
        if (g.ok && g.bwt_cap >= need) {
            return;
        }
        Ipp32u bs = 0;
        int ms = 0;
        int rs = 0;
        int hs = 0;
        ippsBWTFwdGetBufSize_SelectSort_8u(need, &bs, ippBWTAutoSort);
        ippsMTFGetSize_8u(&ms);
        ippsRLEGetSize_BZ2_8u(&rs);
        ippsEncodeHuffGetSize_BZ2_16u8u(need, &hs);
        mi_free(g.bwt_buf);
        g.bwt_buf = static_cast<Ipp8u*>(mi_malloc(bs));
        g.bwt_cap = need;
        mi_free(g.s1);
        g.s1 = static_cast<Ipp8u*>(mi_malloc(static_cast<size_t>((need * 2) + 4096)));
        g.s1_cap = (need * 2) + 4096;
        mi_free(g.s2);
        g.s2 = static_cast<Ipp8u*>(mi_malloc(static_cast<size_t>((need * 2) + 4096)));
        g.s2_cap = (need * 2) + 4096;
        mi_free(g.s3);
        g.s3 = static_cast<Ipp16u*>(mi_malloc(((need * 2) + 4096) * sizeof(Ipp16u)));
        g.s3_cap = (need * 2) + 4096;
        mi_free(g.mtf_state);
        g.mtf_state = mi_malloc(static_cast<size_t>(ms));
        g.mtf_size = ms;
        mi_free(g.rle_state);
        g.rle_state = mi_malloc(static_cast<size_t>(rs));
        g.rle_size = rs;
        mi_free(g.enc_huff);
        g.enc_huff = mi_malloc(static_cast<size_t>(hs));
        g.enc_huff_size = hs;
        ippsMTFInit_8u(static_cast<IppMTFState_8u*>(g.mtf_state));
        ippsEncodeRLEInit_BZ2_8u(static_cast<IppRLEState_BZ2*>(g.rle_state));
        g.ok = true;
    }
}

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
    ensure(input_len);

    output[0] = static_cast<uint8_t>(input_len >> 24);
    output[1] = static_cast<uint8_t>(input_len >> 16);
    output[2] = static_cast<uint8_t>(input_len >> 8);
    output[3] = static_cast<uint8_t>(input_len);

    Ipp32u prim_idx = 0;
    memcpy(g.s1, input, input_len);
    if (ippsBWTFwd_SelectSort_8u(input, g.s1, input_len, &prim_idx, g.bwt_buf, ippBWTAutoSort) != ippStsNoErr) {
        return -1;
    }

    output[4] = static_cast<uint8_t>(prim_idx >> 24);
    output[5] = static_cast<uint8_t>(prim_idx >> 16);
    output[6] = static_cast<uint8_t>(prim_idx >> 8);
    output[7] = static_cast<uint8_t>(prim_idx);
    int wp = 8;

    ippsMTFFwd_8u(g.s1, g.s2, input_len, static_cast<IppMTFState_8u*>(g.mtf_state));

    Ipp8u* rp = g.s2;
    int rs = input_len;
    Ipp8u* rq = g.s1;
    int rq_len = g.s1_cap;
    if (ippsEncodeRLE_BZ2_8u(&rp, &rs, rq, &rq_len, static_cast<IppRLEState_BZ2*>(g.rle_state)) != ippStsNoErr) {
        return -1;
    }
    int rle_out = rq_len;
    int fl_cap = g.s1_cap - rle_out;
    if (ippsEncodeRLEFlush_BZ2_8u(rq + rle_out, &fl_cap, static_cast<IppRLEState_BZ2*>(g.rle_state)) != ippStsNoErr) {
        return -1;
    }
    rle_out += fl_cap;

    Ipp8u* zp = rq;
    int zs = rle_out;
    Ipp16u* zq = g.s3;
    int zq_len = g.s3_cap;
    std::array<int, 258> freq{};
    if (ippsEncodeZ1Z2_BZ2_8u16u(&zp, &zs, zq, &zq_len, freq.data()) != ippStsNoErr) {
        return -1;
    }
    const int z_out = zq_len;

    if (ippsEncodeHuffInit_BZ2_16u8u(input_len, freq.data(), zq, z_out, static_cast<IppEncodeHuffState_BZ2*>(g.enc_huff)) != ippStsNoErr) {
        return -1;
    }

    std::array<Ipp32u, 2> code{};
    int c_bits = 0;
    int p_len = g.s3_cap;
    if (ippsPackHuffContext_BZ2_16u8u(code.data(), &c_bits, output + wp, &p_len, static_cast<IppEncodeHuffState_BZ2*>(g.enc_huff)) != ippStsNoErr) {
        return -1;
    }
    wp += p_len;

    Ipp16u* hp = zq;
    int hs2 = z_out;
    int e_len = output_len - wp;
    if (ippsEncodeHuff_BZ2_16u8u(code.data(), &c_bits, &hp, &hs2, output + wp, &e_len, static_cast<IppEncodeHuffState_BZ2*>(g.enc_huff)) != ippStsNoErr && hs2 > 0) {
        return -1;
    }

    const int remaining = output_len - wp - e_len;
    if (c_bits > 0) {
        const int last = (c_bits + 7) / 8;
        if (wp + last > output_len) {
            return -1;
        }
        for (int i = 0; i < last; i++) {
            output[wp + i] = static_cast<uint8_t>(code[0] >> (i * 8));
        }
        wp += last;
    } else {
        wp += remaining;
    }
    return wp;
}

auto Compress::decompress(const uint8_t* input, const int input_len, uint8_t* output, const int output_len) -> int {
    if (input_len < 8) {
        return -1;
    }
    const int expected = (static_cast<int>(input[0]) << 24) | (static_cast<int>(input[1]) << 16) | (static_cast<int>(input[2]) << 8) | static_cast<int>(input[3]);
    if (output_len < expected) {
        return -1;
    }
    const Ipp32u primIdx = (static_cast<Ipp32u>(input[4]) << 24) | (static_cast<Ipp32u>(input[5]) << 16) | (static_cast<Ipp32u>(input[6]) << 8) | static_cast<Ipp32u>(input[7]);
    ensure(expected);

    int dhs = 0;
    ippsDecodeHuffGetSize_BZ2_8u16u(expected, &dhs);
    auto* dh = static_cast<char*>(mi_malloc(static_cast<size_t>(dhs)));
    ippsDecodeHuffInit_BZ2_8u16u(expected, reinterpret_cast<IppDecodeHuffState_BZ2*>(dh));

    std::array<Ipp32u, 2> code{};
    int c_bits = 0;
    int pos = 8;
    auto* up = const_cast<uint8_t*>(input + pos);
    int up_len = input_len - pos;
    if (ippsUnpackHuffContext_BZ2_8u16u(code.data(), &c_bits, &up, &up_len, reinterpret_cast<IppDecodeHuffState_BZ2*>(dh)) != ippStsNoErr) {
        mi_free(dh);
        return -1;
    }
    pos += (input_len - pos) - up_len;

    auto* dp = const_cast<uint8_t*>(input + pos);
    int dp_len = input_len - pos;
    Ipp16u* dq = g.s3;
    int dq_len = g.s3_cap;
    if (ippsDecodeHuff_BZ2_8u16u(code.data(), &c_bits, &dp, &dp_len, dq, &dq_len, reinterpret_cast<IppDecodeHuffState_BZ2*>(dh)) != ippStsNoErr && dp_len > 0) {
        mi_free(dh);
        return -1;
    }
    mi_free(dh);
    const int huffOut = dq_len;

    Ipp16u* z1p = dq;
    int z1_s = huffOut;
    Ipp8u* z1_q = g.s2;
    int z1_q_len = g.s2_cap;
    if (ippsDecodeZ1Z2_BZ2_16u8u(&z1p, &z1_s, z1_q, &z1_q_len) != ippStsNoErr) {
        return -1;
    }
    const int z1Out = z1_q_len;

    auto* dr = mi_malloc(static_cast<size_t>(g.rle_size));
    ippsDecodeRLEStateInit_BZ2_8u(static_cast<IppRLEState_BZ2*>(dr));
    Ipp8u* rp2 = z1_q;
    auto rs2 = static_cast<Ipp32u>(z1Out);
    Ipp8u* rq2 = g.s1;
    auto rq2_len = static_cast<Ipp32u>(g.s1_cap);
    if (ippsDecodeRLEState_BZ2_8u(&rp2, &rs2, &rq2, &rq2_len, static_cast<IppRLEState_BZ2*>(dr)) != ippStsNoErr && rs2 > 0) {
        mi_free(dr);
        return -1;
    }
    Ipp32u fl = static_cast<Ipp32u>(g.s1_cap) - rq2_len;
    if (ippsDecodeRLEStateFlush_BZ2_8u(static_cast<IppRLEState_BZ2*>(dr), &rq2, &fl) != ippStsNoErr) {
        mi_free(dr);
        return -1;
    }
    mi_free(dr);
    const int rleOut = static_cast<int>(rq2_len + fl);

    ippsMTFInv_8u(g.s1, g.s2, rleOut, static_cast<IppMTFState_8u*>(g.mtf_state));

    Ipp32u bs = 0;
    ippsBWTInvGetSize_8u(expected, reinterpret_cast<int*>(&bs));
    auto* bb = static_cast<Ipp8u*>(mi_malloc(bs));
    const IppStatus st = ippsBWTInv_8u(g.s2, output, expected, static_cast<int>(primIdx), bb);
    mi_free(bb);
    return st == ippStsNoErr ? expected : -1;
}
