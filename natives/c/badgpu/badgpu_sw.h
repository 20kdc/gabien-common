/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#ifndef BADGPU_SW_H_
#define BADGPU_SW_H_

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

static inline badgpu_rect_t badgpu_rect(int l, int u, int r, int d) {
    badgpu_rect_t res = {l, u, r, d};
    return res;
}

static inline void badgpu_rectClip(badgpu_rect_t * a, badgpu_rect_t b) {
    if (a->l < b.l)
        a->l = b.l;
    if (a->u < b.u)
        a->u = b.u;
    if (a->r > b.r)
        a->r = b.r;
    if (a->d > b.d)
        a->d = b.d;
}

static inline void badgpu_rectInclude(badgpu_rect_t * a, badgpu_rect_t b) {
    if (a->l > b.l)
        a->l = b.l;
    if (a->u > b.u)
        a->u = b.u;
    if (a->r < b.r)
        a->r = b.r;
    if (a->d < b.d)
        a->d = b.d;
}

static inline uint8_t f8tou8(float c) {
    int r = (int) ((c * 255) + 0.5);
    if (r < 0)
        return 0;
    if (r > 255)
        return 255;
    return r;
}
static inline float u8tof8(uint8_t c) {
    return c / 255.0f;
}
static inline uint32_t f8topixel(float r, float g, float b, float a) {
    uint32_t res = f8tou8(b);
    res |= f8tou8(g) << 8;
    res |= f8tou8(r) << 16;
    res |= f8tou8(a) << 24;
    return res;
}
static inline uint32_t sessionFlagsToARGBMask(uint32_t sFlags) {
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

typedef float (*badgpu_blendop_t)(float, float);
typedef float (*badgpu_blendweight_t)(float, float, float, float);

typedef struct {
    uint32_t sFlags;
    uint32_t rgbaMaskInv;
    // Blend Program
    badgpu_blendweight_t eqRGBbwS, eqRGBbwD;
    badgpu_blendop_t eqRGBbe;
    badgpu_blendweight_t eqAbwS, eqAbwD;
    badgpu_blendop_t eqAbe;
} badgpu_swrop_t;

void badgpu_ropConfigure(badgpu_swrop_t * opts, uint32_t sFlags, uint32_t blendProgram);

void badgpu_rop(uint32_t * dstRGB, badgpu_ds_t * dstDS, float sR, float sG, float sB, float sA, const badgpu_swrop_t * opts);

#endif
