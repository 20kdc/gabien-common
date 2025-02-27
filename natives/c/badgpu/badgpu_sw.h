/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#ifndef BADGPU_SW_H_
#define BADGPU_SW_H_

#include "badgpu.h"
#include "badgpu_internal.h"

typedef struct {
    float depth;
    uint8_t stencil;
} badgpu_ds_t;

typedef struct {
    int l;
    int u;
    int r;
    int d;
} badgpu_rect_t;

// core maths

BADGPU_INLINE badgpu_rect_t badgpu_rect(int l, int u, int r, int d) {
    badgpu_rect_t res = {l, u, r, d};
    return res;
}

BADGPU_INLINE void badgpu_rectClip(badgpu_rect_t * a, badgpu_rect_t b) {
    if (a->l < b.l)
        a->l = b.l;
    if (a->u < b.u)
        a->u = b.u;
    if (a->r > b.r)
        a->r = b.r;
    if (a->d > b.d)
        a->d = b.d;
}

BADGPU_INLINE void badgpu_rectInclude(badgpu_rect_t * a, badgpu_rect_t b) {
    if (a->l > b.l)
        a->l = b.l;
    if (a->u > b.u)
        a->u = b.u;
    if (a->r < b.r)
        a->r = b.r;
    if (a->d < b.d)
        a->d = b.d;
}

BADGPU_INLINE uint8_t f8tou8(float c) {
    int r = (int) ((c * 255) + 0.5);
    if (r < 0)
        return 0;
    if (r > 255)
        return 255;
    return r;
}
BADGPU_INLINE float u8tof8(uint8_t c) {
    return c / 255.0f;
}
BADGPU_INLINE uint32_t badgpu_sw_v42p(BADGPUSIMDVec4 a) {
    uint32_t res = f8tou8(a.b);
    res |= f8tou8(a.g) << 8;
    res |= f8tou8(a.r) << 16;
    res |= f8tou8(a.a) << 24;
    return res;
}
BADGPU_INLINE BADGPUSIMDVec4 badgpu_sw_p2v4(uint32_t pixel) {
    BADGPUSIMDVec4 vec = {
        .x = (float) ((pixel >> 16) & 0xFF),
        .y = (float) ((pixel >> 8) & 0xFF),
        .z = (float) ((pixel >> 0) & 0xFF),
        .w = (float) ((pixel >> 24) & 0xFF)
    };
    vec.v4 /= badgpu_vec4_1c(255.0f).v4;
    return vec;
}

BADGPU_INLINE uint32_t sessionFlagsToARGBMask(uint32_t sFlags) {
    uint32_t mask = 0;
    if (sFlags & BADGPUSessionFlags_MaskR)
        mask |= 0x00FF0000;
    if (sFlags & BADGPUSessionFlags_MaskG)
        mask |= 0x0000FF00;
    if (sFlags & BADGPUSessionFlags_MaskB)
        mask |= 0x000000FF;
    if (sFlags & BADGPUSessionFlags_MaskA)
        mask |= 0xFF000000;
    return mask;
}

// ROP

typedef float (*badgpu_blendop_t)(float, float) BADGPU_CONSTFN;
typedef float (*badgpu_blendweight_t)(float, float, float, float) BADGPU_CONSTFN;

struct badgpu_swrop;

// typedef BADGPUBool (*badgpu_rop_dsf_t)(const struct badgpu_swrop * opts, badgpu_ds_t * dstDS);
typedef void (*badgpu_rop_txf_t)(const struct badgpu_swrop * opts, uint32_t * dstRGB, BADGPUSIMDVec4 sRGBA);

typedef struct badgpu_swrop {
    // Core Impl.
    badgpu_rop_txf_t txFunc;
    // Flags & Such
    uint32_t flags;
    uint32_t sFlags;
    uint32_t rgbaMask;
    uint32_t rgbaMaskInv;
    // Blend Program
    badgpu_blendweight_t eqRGBbwS, eqRGBbwD;
    badgpu_blendop_t eqRGBbe;
    badgpu_blendweight_t eqAbwS, eqAbwD;
    badgpu_blendop_t eqAbe;
} badgpu_swrop_t;

void badgpu_ropConfigure(badgpu_swrop_t * opts, uint32_t flags, uint32_t sFlags, uint32_t blendProgram);

#endif
