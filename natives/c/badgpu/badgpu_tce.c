/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * BadGPU Reference Implementation
 * Texture Conversion Engine
 */

#include "badgpu_internal.h"

static void cRGBA8888_RGB888(const uint8_t * fD, uint8_t * tD, uint32_t pixels) {
    while (pixels--) {
        tD[0] = fD[0];
        tD[1] = fD[1];
        tD[2] = fD[2];
        tD += 3;
        fD += 4;
    }
}
static void cRGB888_RGBA8888(const uint8_t * fD, uint8_t * tD, uint32_t pixels) {
    while (pixels--) {
        tD[0] = fD[0];
        tD[1] = fD[1];
        tD[2] = fD[2];
        tD[3] = 0xFF;
        tD += 4;
        fD += 3;
    }
}

#define MKARGBI32(r, g, b, a) ((((uint32_t) r) << 16) | (((uint32_t) g) << 8) | ((uint32_t) b) | (((uint32_t) a) << 24))
static void cRGBA8888_ARGBI32(const uint8_t * fD, uint32_t * tD, uint32_t pixels) {
    while (pixels--) {
        uint8_t r = fD[0];
        uint8_t g = fD[1];
        uint8_t b = fD[2];
        uint8_t a = fD[3];
        *(tD++) = MKARGBI32(r, g, b, a);
        fD += 4;
    }
}
static void cRGB888_ARGBI32(const uint8_t * fD, uint32_t * tD, uint32_t pixels) {
    while (pixels--) {
        uint8_t r = fD[0];
        uint8_t g = fD[1];
        uint8_t b = fD[2];
        *(tD++) = MKARGBI32(r, g, b, 0xFF);
        fD += 3;
    }
}
static void cARGBI32_RGBA8888(const uint32_t * fD, uint8_t * tD, uint32_t pixels) {
    while (pixels--) {
        uint32_t argb = *(fD++);
        tD[0] = argb >> 16;
        tD[1] = argb >> 8;
        tD[2] = argb;
        tD[3] = argb >> 24;
        tD += 4;
    }
}
static void cARGBI32_RGB888(const uint32_t * fD, uint8_t * tD, uint32_t pixels) {
    while (pixels--) {
        uint32_t argb = *(fD++);
        tD[0] = argb >> 16;
        tD[1] = argb >> 8;
        tD[2] = argb;
        tD += 3;
    }
}

BADGPU_EXPORT void badgpuPixelsConvert(BADGPUTextureLoadFormat fF,
    BADGPUTextureLoadFormat tF, int16_t width, int16_t height, const void * fD,
    void * tD) {
    if (width <= 0 || height <= 0)
        return;
    if (fF == tF) {
        memcpy(tD, fD, badgpuPixelsSize(tF, width, height));
        return;
    }
    uint32_t pixels = ((uint32_t) width) * (uint32_t) height;
    switch (fF) {
    case BADGPUTextureLoadFormat_RGBA8888:
        switch (tF) {
        case BADGPUTextureLoadFormat_RGB888:
            cRGBA8888_RGB888(fD, tD, pixels);
            return;
        case BADGPUTextureLoadFormat_ARGBI32:
            cRGBA8888_ARGBI32(fD, tD, pixels);
            return;
        default: return;
        }
        break;
    case BADGPUTextureLoadFormat_RGB888:
        switch (tF) {
        case BADGPUTextureLoadFormat_RGBA8888:
            cRGB888_RGBA8888(fD, tD, pixels);
            return;
        case BADGPUTextureLoadFormat_ARGBI32:
            cRGB888_ARGBI32(fD, tD, pixels);
            return;
        default: return;
        }
        break;
    case BADGPUTextureLoadFormat_ARGBI32:
        switch (tF) {
        case BADGPUTextureLoadFormat_RGBA8888:
            cARGBI32_RGBA8888(fD, tD, pixels);
            return;
        case BADGPUTextureLoadFormat_RGB888:
            cARGBI32_RGB888(fD, tD, pixels);
            return;
        default: return;
        }
        break;
    default: return;
    }
}

BADGPU_EXPORT uint32_t badgpuPixelsSize(BADGPUTextureLoadFormat format,
    int16_t width, int16_t height) {
    if (width <= 0 || height <= 0)
        return 0;
    uint32_t pixels = ((uint32_t) width) * (uint32_t) height;
    switch (format) {
    case BADGPUTextureLoadFormat_RGBA8888:
        return pixels * 4;
    case BADGPUTextureLoadFormat_RGB888:
        return pixels * 3;
    case BADGPUTextureLoadFormat_ARGBI32:
        return pixels * 4;
    default: return 0;
    }
}

