/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu.h"
#include "badgpu_internal.h"
#include "badgpu_sw.h"

static float BADGPU_CONSTFN ropEqAdd(float a, float b) {
    return a + b;
}

static float BADGPU_CONSTFN ropEqSub(float a, float b) {
    return a - b;
}

static float BADGPU_CONSTFN ropEqSubRev(float a, float b) {
    return b - a;
}

static float BADGPU_CONSTFN ropBWZero(float s, float d, float sA, float dA) {
    return 0;
}

static float BADGPU_CONSTFN ropBWOne(float s, float d, float sA, float dA) {
    return 1;
}

static float BADGPU_CONSTFN ropBWSrcAlphaSaturate(float s, float d, float sA, float dA) {
    float invdA = 1 - dA;
    return invdA < sA ? invdA : sA;
}

static float BADGPU_CONSTFN ropBWDst(float s, float d, float sA, float dA) {
    return d;
}

static float BADGPU_CONSTFN ropBWInvertDst(float s, float d, float sA, float dA) {
    return 1 - d;
}

static float BADGPU_CONSTFN ropBWDstA(float s, float d, float sA, float dA) {
    return dA;
}

static float BADGPU_CONSTFN ropBWInvertDstA(float s, float d, float sA, float dA) {
    return 1 - dA;
}

static float BADGPU_CONSTFN ropBWSrc(float s, float d, float sA, float dA) {
    return s;
}

static float BADGPU_CONSTFN ropBWInvertSrc(float s, float d, float sA, float dA) {
    return 1 - s;
}

static float BADGPU_CONSTFN ropBWSrcA(float s, float d, float sA, float dA) {
    return sA;
}

static float BADGPU_CONSTFN ropBWInvertSrcA(float s, float d, float sA, float dA) {
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

static void badgpu_rop_txfBlend(const badgpu_swrop_t * __restrict__ opts, uint32_t * __restrict__ dstRGB, BADGPUSIMDVec4 sRGBA) {
    uint32_t pixDst = *dstRGB;
    uint32_t orin = 0;
    float dA = u8tof8(pixDst >> 24);
    if (opts->sFlags & BADGPUSessionFlags_MaskR) {
        float dR = u8tof8(pixDst >> 16);
        orin |= f8tou8(opts->eqRGBbe(opts->eqRGBbwS(sRGBA.x, dR, sRGBA.w, dA) * sRGBA.x, opts->eqRGBbwD(sRGBA.x, dR, sRGBA.w, dA) * dR)) << 16;
    }
    if (opts->sFlags & BADGPUSessionFlags_MaskG) {
        float dG = u8tof8(pixDst >> 8);
        orin |= f8tou8(opts->eqRGBbe(opts->eqRGBbwS(sRGBA.y, dG, sRGBA.w, dA) * sRGBA.y, opts->eqRGBbwD(sRGBA.y, dG, sRGBA.w, dA) * dG)) << 8;
    }
    if (opts->sFlags & BADGPUSessionFlags_MaskB) {
        float dB = u8tof8(pixDst);
        orin |= f8tou8(opts->eqRGBbe(opts->eqRGBbwS(sRGBA.z, dB, sRGBA.w, dA) * sRGBA.z, opts->eqRGBbwD(sRGBA.z, dB, sRGBA.w, dA) * dB));
    }
    if (opts->sFlags & BADGPUSessionFlags_MaskA)
        orin |= f8tou8(opts->eqAbe(opts->eqAbwS(sRGBA.w, dA, sRGBA.w, dA) * sRGBA.w, opts->eqAbwD(sRGBA.w, dA, sRGBA.w, dA) * dA)) << 24;
    pixDst &= opts->rgbaMaskInv;
    pixDst |= orin;
    *dstRGB = pixDst;
}

static void badgpu_rop_txfNormalBlend(const badgpu_swrop_t * __restrict__ opts, uint32_t * __restrict__ dstRGB, BADGPUSIMDVec4 sRGBA) {
    // One, InvertSrcA, Add
    uint32_t pixDst = *dstRGB;
    BADGPUSIMDVec4 vDst = badgpu_sw_p2v4(pixDst);
    float invSA = 1 - sRGBA.w;
    vDst.v4 = (vDst.v4 * badgpu_vec4_1c(invSA).v4) + sRGBA.v4;
    uint32_t orin = badgpu_sw_v42p(vDst);
    orin &= opts->rgbaMask;
    pixDst &= opts->rgbaMaskInv;
    pixDst |= orin;
    *dstRGB = pixDst;
}

static void badgpu_rop_txfNoBlend(const badgpu_swrop_t * __restrict__ opts, uint32_t * __restrict__ dstRGB, BADGPUSIMDVec4 sRGBA) {
    uint32_t pixDst = *dstRGB;
    uint32_t orin = badgpu_sw_v42p(sRGBA);
    orin &= opts->rgbaMask;
    pixDst &= opts->rgbaMaskInv;
    pixDst |= orin;
    *dstRGB = pixDst;
}

void badgpu_ropConfigure(badgpu_swrop_t * opts, uint32_t flags, uint32_t sFlags, uint32_t blendProgram) {
    opts->txFunc = flags & BADGPUDrawFlags_Blend ? badgpu_rop_txfBlend : badgpu_rop_txfNoBlend;
    opts->flags = flags;
    opts->sFlags = sFlags;
    opts->rgbaMask = sessionFlagsToARGBMask(sFlags);
    opts->rgbaMaskInv = ~opts->rgbaMask;
    opts->eqRGBbwS = convertBlendWeight(BADGPU_BP_RGBS(blendProgram), 0);
    opts->eqRGBbwD = convertBlendWeight(BADGPU_BP_RGBD(blendProgram), 0);
    opts->eqAbwS = convertBlendWeight(BADGPU_BP_AS(blendProgram), 1);
    opts->eqAbwD = convertBlendWeight(BADGPU_BP_AD(blendProgram), 1);
    opts->eqRGBbe = convertBlendOp(BADGPU_BP_RGBE(blendProgram));
    opts->eqAbe = convertBlendOp(BADGPU_BP_AE(blendProgram));
    if (flags & BADGPUDrawFlags_Blend) {
        // hardcoded blending programs
        if (opts->eqRGBbwS == ropBWOne && opts->eqRGBbwD == ropBWInvertSrcA && opts->eqRGBbe == ropEqAdd && opts->eqAbwS == ropBWOne && opts->eqAbwD == ropBWInvertSrcA && opts->eqAbe == ropEqAdd) {
            opts->txFunc = badgpu_rop_txfNormalBlend;
        }
    }
}
