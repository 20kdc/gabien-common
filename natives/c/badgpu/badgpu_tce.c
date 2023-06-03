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

// This is intended to be optimized out
static const uint32_t ENDIAN_DETECTOR = 1;

static void cRGBA8888_RGB888(const uint8_t * fD, uint8_t * tD, size_t pixels) {
    while (pixels--) {
        tD[0] = fD[0];
        tD[1] = fD[1];
        tD[2] = fD[2];
        tD += 3;
        fD += 4;
    }
}
static void cRGB888_RGBA8888(const uint8_t * fD, uint8_t * tD, size_t pixels) {
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
static void cRGBA8888_ARGBI32(const uint8_t * fD, uint32_t * tD, size_t pixels) {
    while (pixels--) {
        uint8_t r = fD[0];
        uint8_t g = fD[1];
        uint8_t b = fD[2];
        uint8_t a = fD[3];
        *(tD++) = MKARGBI32(r, g, b, a);
        fD += 4;
    }
}
static void cRGB888_ARGBI32(const uint8_t * fD, uint32_t * tD, size_t pixels) {
    while (pixels--) {
        uint8_t r = fD[0];
        uint8_t g = fD[1];
        uint8_t b = fD[2];
        *(tD++) = MKARGBI32(r, g, b, 0xFF);
        fD += 3;
    }
}
static void cARGBI32_RGBA8888(const uint32_t * fD, uint8_t * tD, size_t pixels) {
    if (sizeof(void *) != 4) {
        // 64-bit "fast-path"
        // interestingly clang is perfectly willing to vectorize this
        uint64_t * fD64 = (void *) fD;
        uint64_t * tD64 = (void *) tD;
        while (pixels >= 2) {
            pixels -= 2;
            if (((uint8_t *) &(ENDIAN_DETECTOR))[0] == 1) {
                // LE
                //             IN 0xAARRGGBBAARRGGBB
                //            OUT 0xAABBGGRRAABBGGRR
                uint64_t ag = *fD64;
                uint64_t b = ag & 0x00FF000000FF0000UL;
                uint64_t r = ag & 0x000000FF000000FFUL;
                ag &=             0xFF00FF00FF00FF00UL;
                *tD64 = ag | (b >> 16) | (r << 16);
            } else {
                // BE
                //              IN 0xAARRGGBBAARRGGBB
                //             OUT 0xRRGGBBAARRGGBBAA
                uint64_t rgb = *fD64;
                uint64_t a = rgb & 0xFF000000FF000000UL;
                rgb &=             0x00FFFFFF00FFFFFFUL;
                *tD64 = (rgb << 8) | (a >> 24);
            }
            fD64++;
            tD64++;
        }
        fD = (void *) fD64;
        tD = (void *) tD64;
    }
    while (pixels--) {
        uint32_t argb = *(fD++);
        tD[0] = argb >> 16;
        tD[1] = argb >> 8;
        tD[2] = argb;
        tD[3] = argb >> 24;
        tD += 4;
    }
}
static void cARGBI32_RGB888(const uint32_t * fD, uint8_t * tD, size_t pixels) {
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
    size_t pixels = ((size_t) width) * (size_t) height;
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

BADGPU_EXPORT void badgpuPixelsConvertRGBA8888ToARGBI32InPlace(int16_t width,
    int16_t height, void * data) {
    if (width <= 0 || height <= 0)
        return;
    size_t pixels = ((size_t) width) * (size_t) height;
    // this all really has to be tested against clang
    //  because that's what Zig uses
    // despite gcc having better autovec for this stuff
    // clang can't autovec it so do what we can
    if (sizeof(void *) != 4) {
        // 64-bit "fast-path"
        // interestingly clang is perfectly willing to vectorize this
        uint64_t * data64 = data;
        while (pixels >= 2) {
            pixels -= 2;
            if (((uint8_t *) &(ENDIAN_DETECTOR))[0] == 1) {
                // LE
                //             IN 0xAABBGGRRAABBGGRR
                //            OUT 0xAARRGGBBAARRGGBB
                uint64_t ag = *data64;
                uint64_t b = ag & 0x00FF000000FF0000UL;
                uint64_t r = ag & 0x000000FF000000FFUL;
                ag &=             0xFF00FF00FF00FF00UL;
                *data64 = ag | (b >> 16) | (r << 16);
            } else {
                // BE
                //              IN 0xRRGGBBAARRGGBBAA
                //             OUT 0xAARRGGBBAARRGGBB
                uint64_t rgb = *data64;
                uint64_t a = rgb & 0x000000FF000000FFUL;
                rgb &=             0xFFFFFF00FFFFFF00UL;
                *data64 = (rgb >> 8) | (a << 24);
            }
            data64++;
        }
        data = data64;
    }
    uint8_t * data8 = data;
    // LE
    while (pixels--) {
        uint8_t tmpR = data8[0];
        uint8_t tmpG = data8[1];
        uint8_t tmpB = data8[2];
        uint8_t tmpA = data8[3];
        if (((uint8_t *) &(ENDIAN_DETECTOR))[0] == 1) {
            // LE
            data8[0] = tmpB;
            data8[1] = tmpG;
            data8[2] = tmpR;
            data8[3] = tmpA;
        } else {
            // BE
            data8[0] = tmpA;
            data8[1] = tmpR;
            data8[2] = tmpG;
            data8[3] = tmpB;
        }
        data8 += 4;
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

