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

#include "badgpu.h"
#include "badgpu_internal.h"

// -- endianness --

// This is intended to be optimized out
static const uint32_t ENDIAN_DETECTOR = 1;

#define BADGPU_LITTLE_ENDIAN() (((uint8_t *) &(ENDIAN_DETECTOR))[0] == 1)

// -- converter core --

#define BADGPU_TCE_HEAD(fType, tType) \
BADGPU_INLINE void c ## fType ## _ ## tType (const BADGPU_TCE_pixel_ ## fType * __restrict__ fD, BADGPU_TCE_pixel_ ## tType * __restrict__ tD, size_t pixels) { \
    while (pixels--) {
#define BADGPU_TCE_TAIL \
    } \
}

#define BADGPU_TCE_MEMC(fType, size) \
BADGPU_INLINE void c ## fType ## _ ## fType(const void * fD, void * tD, size_t pixels) { \
    memcpy(tD, fD, pixels * size); \
}

// -- PMA --

#define BADGPU_TCE_ALPHA_S2P \
        r = (r * (uint32_t) a) / 255; \
        g = (g * (uint32_t) a) / 255; \
        b = (b * (uint32_t) a) / 255;

// Rounding up here keeps roundtrips clean when possible
// The conversion to premul rounds down to avoid creating additives.
// So conversely the conversion from premul must round up.
#define BADGPU_TCE_ALPHA_P2S \
        if (a != 0) { \
            r = ((r * (uint32_t) 255) + (a - (uint32_t) 1)) / a; \
            g = ((g * (uint32_t) 255) + (a - (uint32_t) 1)) / a; \
            b = ((b * (uint32_t) 255) + (a - (uint32_t) 1)) / a; \
        }

// -- format load and save --

// units used in load/save routines
typedef uint8_t  BADGPU_TCE_pixel_RGBA8888;
typedef uint8_t  BADGPU_TCE_pixel_RGB888;
typedef uint32_t BADGPU_TCE_pixel_ARGBI32;
typedef uint8_t  BADGPU_TCE_pixel_RGBA8888_SA;
typedef uint32_t BADGPU_TCE_pixel_ARGBI32_SA;

#define MKARGBI32(r, g, b, a) ((((uint32_t) r) << 16) | (((uint32_t) g) << 8) | ((uint32_t) b) | (((uint32_t) a) << 24))

#define BADGPU_TCE_LOAD_RGB888 uint8_t r = fD[0], g = fD[1], b = fD[2], a = 255; fD += 3;
#define BADGPU_TCE_LOAD_RGBA8888 uint8_t r = fD[0], g = fD[1], b = fD[2], a = fD[3]; fD += 4;
#define BADGPU_TCE_LOAD_ARGBI32 \
    uint32_t fV = *(fD++); \
    uint8_t r = fV >> 16, g = fV >> 8, b = fV, a = fV >> 24;

#define BADGPU_TCE_SAVE_RGB888 tD[0] = r; tD[1] = g; tD[2] = b; tD += 3;
#define BADGPU_TCE_SAVE_RGBA8888 tD[0] = r; tD[1] = g; tD[2] = b; tD[3] = a; tD += 4;
#define BADGPU_TCE_SAVE_ARGBI32 *(tD++) = MKARGBI32(r, g, b, a);

// -- converters --

BADGPU_TCE_HEAD(RGBA8888, RGB888       ) BADGPU_TCE_LOAD_RGBA8888                      BADGPU_TCE_SAVE_RGB888   BADGPU_TCE_TAIL
BADGPU_TCE_MEMC(RGBA8888, 4)
BADGPU_TCE_HEAD(RGBA8888, ARGBI32      ) BADGPU_TCE_LOAD_RGBA8888                      BADGPU_TCE_SAVE_ARGBI32  BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(RGBA8888, RGBA8888_SA  ) BADGPU_TCE_LOAD_RGBA8888 BADGPU_TCE_ALPHA_P2S BADGPU_TCE_SAVE_RGBA8888 BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(RGBA8888, ARGBI32_SA   ) BADGPU_TCE_LOAD_RGBA8888 BADGPU_TCE_ALPHA_P2S BADGPU_TCE_SAVE_ARGBI32  BADGPU_TCE_TAIL

// this is special : no alpha so the SA format means nothing as a source
BADGPU_TCE_HEAD(RGB888, RGBA8888       ) BADGPU_TCE_LOAD_RGB888                        BADGPU_TCE_SAVE_RGBA8888 BADGPU_TCE_TAIL
BADGPU_TCE_MEMC(RGB888, 3)
BADGPU_TCE_HEAD(RGB888, ARGBI32        ) BADGPU_TCE_LOAD_RGB888                        BADGPU_TCE_SAVE_ARGBI32  BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(RGB888, RGBA8888_SA    ) BADGPU_TCE_LOAD_RGB888                        BADGPU_TCE_SAVE_RGBA8888 BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(RGB888, ARGBI32_SA     ) BADGPU_TCE_LOAD_RGB888                        BADGPU_TCE_SAVE_ARGBI32  BADGPU_TCE_TAIL

BADGPU_TCE_HEAD(ARGBI32, RGBA8888      ) BADGPU_TCE_LOAD_ARGBI32                       BADGPU_TCE_SAVE_RGBA8888 BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(ARGBI32, RGB888        ) BADGPU_TCE_LOAD_ARGBI32                       BADGPU_TCE_SAVE_RGB888   BADGPU_TCE_TAIL
BADGPU_TCE_MEMC(ARGBI32, 4)
BADGPU_TCE_HEAD(ARGBI32, RGBA8888_SA   ) BADGPU_TCE_LOAD_ARGBI32  BADGPU_TCE_ALPHA_P2S BADGPU_TCE_SAVE_RGBA8888 BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(ARGBI32, ARGBI32_SA    ) BADGPU_TCE_LOAD_ARGBI32  BADGPU_TCE_ALPHA_P2S BADGPU_TCE_SAVE_ARGBI32  BADGPU_TCE_TAIL

BADGPU_TCE_HEAD(RGBA8888_SA, RGB888    ) BADGPU_TCE_LOAD_RGBA8888 BADGPU_TCE_ALPHA_S2P BADGPU_TCE_SAVE_RGB888   BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(RGBA8888_SA, ARGBI32   ) BADGPU_TCE_LOAD_RGBA8888 BADGPU_TCE_ALPHA_S2P BADGPU_TCE_SAVE_ARGBI32  BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(RGBA8888_SA, RGBA8888  ) BADGPU_TCE_LOAD_RGBA8888 BADGPU_TCE_ALPHA_S2P BADGPU_TCE_SAVE_RGBA8888 BADGPU_TCE_TAIL
BADGPU_TCE_MEMC(RGBA8888_SA, 4)
BADGPU_TCE_HEAD(RGBA8888_SA, ARGBI32_SA) BADGPU_TCE_LOAD_RGBA8888                      BADGPU_TCE_SAVE_ARGBI32  BADGPU_TCE_TAIL

BADGPU_TCE_HEAD(ARGBI32_SA, RGB888     ) BADGPU_TCE_LOAD_ARGBI32  BADGPU_TCE_ALPHA_S2P BADGPU_TCE_SAVE_RGB888   BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(ARGBI32_SA, ARGBI32    ) BADGPU_TCE_LOAD_ARGBI32  BADGPU_TCE_ALPHA_S2P BADGPU_TCE_SAVE_ARGBI32  BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(ARGBI32_SA, RGBA8888   ) BADGPU_TCE_LOAD_ARGBI32  BADGPU_TCE_ALPHA_S2P BADGPU_TCE_SAVE_RGBA8888 BADGPU_TCE_TAIL
BADGPU_TCE_HEAD(ARGBI32_SA, RGBA8888_SA) BADGPU_TCE_LOAD_ARGBI32                       BADGPU_TCE_SAVE_RGBA8888 BADGPU_TCE_TAIL
BADGPU_TCE_MEMC(ARGBI32_SA, 4)

// -- converter dispatch --

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
#define BADGPU_TCE_TOFORMATSUBBLOCK(fromFormat) \
        switch (tF) { \
        case BADGPUTextureLoadFormat_RGBA8888: c ## fromFormat ## _RGBA8888(fD, tD, pixels); return; \
        case BADGPUTextureLoadFormat_RGB888: c ## fromFormat ## _RGB888(fD, tD, pixels); return; \
        case BADGPUTextureLoadFormat_ARGBI32: c ## fromFormat ## _ARGBI32(fD, tD, pixels); return; \
        case BADGPUTextureLoadFormat_RGBA8888_SA: c ## fromFormat ## _RGBA8888_SA(fD, tD, pixels); return; \
        case BADGPUTextureLoadFormat_ARGBI32_SA: c ## fromFormat ## _ARGBI32_SA(fD, tD, pixels); return; \
        default: return; \
        }
    switch (fF) {
    case BADGPUTextureLoadFormat_RGBA8888: BADGPU_TCE_TOFORMATSUBBLOCK(RGBA8888); break;
    case BADGPUTextureLoadFormat_RGB888: BADGPU_TCE_TOFORMATSUBBLOCK(RGB888); break;
    case BADGPUTextureLoadFormat_ARGBI32: BADGPU_TCE_TOFORMATSUBBLOCK(ARGBI32); break;
    case BADGPUTextureLoadFormat_RGBA8888_SA: BADGPU_TCE_TOFORMATSUBBLOCK(RGBA8888_SA); break;
    case BADGPUTextureLoadFormat_ARGBI32_SA: BADGPU_TCE_TOFORMATSUBBLOCK(ARGBI32_SA); break;
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
            if (BADGPU_LITTLE_ENDIAN()) {
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
        if (BADGPU_LITTLE_ENDIAN()) {
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

BADGPU_EXPORT void badgpuPixelsConvertARGBI32StraightToPremultipliedInPlace(
    int16_t width, int16_t height, uint32_t * data) {
    if (width <= 0 || height <= 0)
        return;
    size_t pixels = ((size_t) width) * (size_t) height;
    while (pixels--) {
        uint32_t pixel = *data;
        uint32_t r = (pixel >> 16) & 0xFF;
        uint32_t g = (pixel >> 8) & 0xFF;
        uint32_t b = pixel & 0xFF;
        uint32_t a = (pixel >> 24) & 0xFF;
        BADGPU_TCE_ALPHA_S2P
        *(data++) = MKARGBI32(r, g, b, a);
    }
}

BADGPU_EXPORT void badgpuPixelsConvertARGBI32PremultipliedToStraightInPlace(
    int16_t width, int16_t height, uint32_t * data) {
    if (width <= 0 || height <= 0)
        return;
    size_t pixels = ((size_t) width) * (size_t) height;
    while (pixels--) {
        uint32_t pixel = *data;
        uint32_t r = (pixel >> 16) & 0xFF;
        uint32_t g = (pixel >> 8) & 0xFF;
        uint32_t b = pixel & 0xFF;
        uint32_t a = (pixel >> 24) & 0xFF;
        BADGPU_TCE_ALPHA_P2S
        *(data++) = MKARGBI32(r, g, b, a);
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
    case BADGPUTextureLoadFormat_RGBA8888_SA:
        return pixels * 4;
    case BADGPUTextureLoadFormat_ARGBI32_SA:
        return pixels * 4;
    default: return 0;
    }
}

