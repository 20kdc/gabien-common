/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu.h"
#include "badgpu_sw.h"

static float ropEqAdd(float a, float b) {
    return a + b;
}

static float ropEqSub(float a, float b) {
    return a - b;
}

static float ropEqSubRev(float a, float b) {
    return b - a;
}

static float ropBWZero(float s, float d, float sA, float dA) {
    return 0;
}

static float ropBWOne(float s, float d, float sA, float dA) {
    return 1;
}

static float ropBWSrcAlphaSaturate(float s, float d, float sA, float dA) {
    float invdA = 1 - dA;
    return invdA < sA ? invdA : sA;
}

static float ropBWDst(float s, float d, float sA, float dA) {
    return d;
}

static float ropBWInvertDst(float s, float d, float sA, float dA) {
    return 1 - d;
}

static float ropBWDstA(float s, float d, float sA, float dA) {
    return dA;
}

static float ropBWInvertDstA(float s, float d, float sA, float dA) {
    return 1 - dA;
}

static float ropBWSrc(float s, float d, float sA, float dA) {
    return s;
}

static float ropBWInvertSrc(float s, float d, float sA, float dA) {
    return 1 - s;
}

static float ropBWSrcA(float s, float d, float sA, float dA) {
    return sA;
}

static float ropBWInvertSrcA(float s, float d, float sA, float dA) {
    return 1 - sA;
}

static badgpu_blendop_t convertBlendOp(BADGPUBlendOp op) {
    if (op == BADGPUBlendOp_ReverseSub)
        return ropEqSubRev;
    if (op == BADGPUBlendOp_Sub)
        return ropEqSub;
    return ropEqAdd;
}

static badgpu_blendweight_t convertBlendWeight(BADGPUBlendWeight w, int isA) {
    if (w == BADGPUBlendWeight_Zero)
        return ropBWZero;
    if (w == BADGPUBlendWeight_SrcAlphaSaturate)
        return isA ? ropBWOne : ropBWSrcAlphaSaturate;
    if (w == BADGPUBlendWeight_Dst)
        return ropBWDst;
    if (w == BADGPUBlendWeight_InvertDst)
        return ropBWInvertDst;
    if (w == BADGPUBlendWeight_DstA)
        return ropBWDstA;
    if (w == BADGPUBlendWeight_InvertDstA)
        return ropBWInvertDstA;
    if (w == BADGPUBlendWeight_Src)
        return ropBWSrc;
    if (w == BADGPUBlendWeight_InvertSrc)
        return ropBWInvertSrc;
    if (w == BADGPUBlendWeight_SrcA)
        return ropBWSrcA;
    if (w == BADGPUBlendWeight_InvertSrcA)
        return ropBWInvertSrcA;
    if (w == BADGPUBlendWeight_Zero)
        return ropBWZero;
    return ropBWOne;
}

static void badgpu_rop_txfBlend(const badgpu_swrop_t * opts, uint32_t * dstRGB, float sR, float sG, float sB, float sA) {
    uint32_t pixDst = *dstRGB;
    uint32_t orin = 0;
    float dA = u8tof8(pixDst >> 24);
    if (opts->sFlags & BADGPUSessionFlags_MaskR) {
        float dR = u8tof8(pixDst >> 16);
        orin |= f8tou8(opts->eqRGBbe(opts->eqRGBbwS(sR, dR, sA, dA) * sR, opts->eqRGBbwD(sR, dR, sA, dA) * dR)) << 16;
    }
    if (opts->sFlags & BADGPUSessionFlags_MaskG) {
        float dG = u8tof8(pixDst >> 8);
        orin |= f8tou8(opts->eqRGBbe(opts->eqRGBbwS(sG, dG, sA, dA) * sG, opts->eqRGBbwD(sG, dG, sA, dA) * dG)) << 8;
    }
    if (opts->sFlags & BADGPUSessionFlags_MaskB) {
        float dB = u8tof8(pixDst);
        orin |= f8tou8(opts->eqRGBbe(opts->eqRGBbwS(sB, dB, sA, dA) * sB, opts->eqRGBbwD(sB, dB, sA, dA) * dB));
    }
    if (opts->sFlags & BADGPUSessionFlags_MaskA)
        orin |= f8tou8(opts->eqAbe(opts->eqAbwS(sA, dA, sA, dA) * sA, opts->eqAbwD(sA, dA, sA, dA) * dA)) << 24;
    pixDst &= opts->rgbaMaskInv;
    pixDst |= orin;
    *dstRGB = pixDst;
}

static void badgpu_rop_txfNoBlend(const badgpu_swrop_t * opts, uint32_t * dstRGB, float sR, float sG, float sB, float sA) {
    uint32_t pixDst = *dstRGB;
    uint32_t orin = 0;
    if (opts->sFlags & BADGPUSessionFlags_MaskR)
        orin |= f8tou8(sR) << 16;
    if (opts->sFlags & BADGPUSessionFlags_MaskG)
        orin |= f8tou8(sG) << 8;
    if (opts->sFlags & BADGPUSessionFlags_MaskB)
        orin |= f8tou8(sB);
    if (opts->sFlags & BADGPUSessionFlags_MaskA)
        orin |= f8tou8(sA) << 24;
    pixDst &= opts->rgbaMaskInv;
    pixDst |= orin;
    *dstRGB = pixDst;
}

void badgpu_ropConfigure(badgpu_swrop_t * opts, uint32_t flags, uint32_t sFlags, uint32_t blendProgram) {
    opts->txFunc = flags & BADGPUDrawFlags_Blend ? badgpu_rop_txfBlend : badgpu_rop_txfNoBlend;
    opts->flags = flags;
    opts->sFlags = sFlags;
    opts->rgbaMaskInv = ~sessionFlagsToARGBMask(sFlags);
    opts->eqRGBbwS = convertBlendWeight(BADGPU_BP_RGBS(blendProgram), 0);
    opts->eqRGBbwD = convertBlendWeight(BADGPU_BP_RGBD(blendProgram), 0);
    opts->eqAbwS = convertBlendWeight(BADGPU_BP_AS(blendProgram), 1);
    opts->eqAbwD = convertBlendWeight(BADGPU_BP_AD(blendProgram), 1);
    opts->eqRGBbe = convertBlendOp(BADGPU_BP_RGBE(blendProgram));
    opts->eqAbe = convertBlendOp(BADGPU_BP_AE(blendProgram));
}
