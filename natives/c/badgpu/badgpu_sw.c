/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * The BadGPU Software Rasterizer... would go here, if it existed.
 */

#include "badgpu.h"
#include "badgpu_internal.h"
#include "badgpu_sw.h"

typedef struct BADGPUTextureSW {
    BADGPUTexturePriv base;
    int w, h;
    uint32_t data[];
} BADGPUTextureSW;
#define BG_TEXTURE_SW(x) ((BADGPUTextureSW *) (x))

typedef struct BADGPUDSBufferSW {
    BADGPUDSBufferPriv base;
    int w, h;
    badgpu_ds_t data[];
} BADGPUDSBufferSW;
#define BG_DSBUFFER_SW(x) ((BADGPUDSBufferSW *) (x))

static void destroySWInstance(BADGPUObject obj) {
    free(obj);
}
static void destroySWTexture(BADGPUObject obj) {
    BADGPUTextureSW * tex = BG_TEXTURE_SW(obj);
    badgpuUnref((BADGPUObject) tex->base.i);
    free(obj);
}
static void destroySWDSBuffer(BADGPUObject obj) {
    BADGPUDSBufferSW * tex = BG_DSBUFFER_SW(obj);
    badgpuUnref((BADGPUObject) tex->base.i);
    free(obj);
}

static const char * bswGetMetaInfo(struct BADGPUInstancePriv * instance, BADGPUMetaInfoType mi) {
    if (mi == BADGPUMetaInfoType_Vendor)
        return "BadGPU";
    if (mi == BADGPUMetaInfoType_Renderer)
        return "BadGPU Software Rasterizer";
    if (mi == BADGPUMetaInfoType_Version)
        return "Non-Functional";
    return NULL;
}

static BADGPUTexture bswNewTexture(struct BADGPUInstancePriv * instance, int16_t width, int16_t height, const void * data) {
    size_t datasize = ((size_t) width) * ((size_t) height) * sizeof(uint32_t);
    BADGPUTextureSW * tex = malloc(sizeof(BADGPUTextureSW) + datasize);
    if (!tex) {
        badgpuErr(instance, "badgpuNewTexture: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroySWTexture);

    tex->w = width;
    tex->h = height;
    tex->base.i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));

    if (data)
        memcpy(tex->data, data, datasize);

    return (BADGPUTexture) tex;
}

static BADGPUDSBuffer bswNewDSBuffer(struct BADGPUInstancePriv * instance, int16_t width, int16_t height) {
    size_t datasize = ((size_t) width) * ((size_t) height) * sizeof(badgpu_ds_t);
    BADGPUDSBufferSW * tex = malloc(sizeof(BADGPUDSBufferSW) + datasize);
    if (!tex) {
        badgpuErr(instance, "badgpuNewDSBuffer: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroySWDSBuffer);

    tex->w = width;
    tex->h = height;
    tex->base.i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));

    memset(tex->data, 0, datasize);

    return (BADGPUDSBuffer) tex;
}

static BADGPUBool bswGenerateMipmap(void * texture) {
    return 0;
}

static BADGPUBool bswReadPixelsARGBI32(void * texture, uint16_t x, uint16_t y, int16_t width, int16_t height, uint32_t * data) {
    BADGPUTextureSW * tex = BG_TEXTURE_SW(texture);
    int w = width;
    int h = height;
    int i;
    if (x + w > tex->w || y + h > tex->h)
        return badgpuErr(tex->base.i, "badgpuReadPixels: Read out of range");
    const uint32_t * texdata = tex->data;
    for (i = 0; i < height; i++) {
        memcpy(data, texdata, width * sizeof(uint32_t));
        data += width;
        texdata += width;
    }
    return 1;
}

// -- renderfuncs --

static BADGPUBool bswVSize(BADGPU_SESSIONFLAGS, int * w, int * h, badgpu_rect_t * region) {
    if (!sTexture) {
        if (sDSBuffer) {
            *w = BG_DSBUFFER_SW(sDSBuffer)->w;
            *h = BG_DSBUFFER_SW(sDSBuffer)->h;
            return 1;
        } else {
            return 0;
        }
    } else if (sDSBuffer) {
        if (BG_TEXTURE_SW(sTexture)->w != BG_DSBUFFER_SW(sDSBuffer)->w)
            return 0;
        if (BG_TEXTURE_SW(sTexture)->h != BG_DSBUFFER_SW(sDSBuffer)->h)
            return 0;
        *w = BG_TEXTURE_SW(sTexture)->w;
        *h = BG_TEXTURE_SW(sTexture)->h;
        return 1;
    } else {
        *w = BG_TEXTURE_SW(sTexture)->w;
        *h = BG_TEXTURE_SW(sTexture)->h;
        return 1;
    }
}

static BADGPUBool bswDrawClear(
    struct BADGPUInstancePriv * instance,
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
) {
    uint32_t pixel = f8topixel(cR, cG, cB, cA);
    badgpu_ds_t ds = { depth, stencil };

    int vpW, vpH;
    BADGPUTextureSW * rTex = BG_TEXTURE_SW(sTexture);
    BADGPUDSBufferSW * rDS = BG_DSBUFFER_SW(sDSBuffer);
    badgpu_rect_t region;
    if (!bswVSize(BADGPU_SESSIONFLAGS_PASSTHROUGH, &vpW, &vpH, &region))
        return 0;

    int x, y;

    if (rTex && (sFlags & BADGPUSessionFlags_MaskRGBA)) {
        uint32_t mask = sessionFlagsToARGBMask(sFlags);
        pixel &= mask;
        for (y = region.u; y < region.d; y++) {
            for (x = region.l; x < region.r; x++) {
                size_t p = x + (y * vpW);
                rTex->data[p] &= ~mask;
                rTex->data[p] |= pixel;
                p++;
            }
        }
    }
    if (rDS && (sFlags & BADGPUSessionFlags_StencilAll)) {
        for (y = region.u; y < region.d; y++) {
            for (x = region.l; x < region.r; x++) {
                size_t p = x + (y * vpW);
                if (sFlags & BADGPUSessionFlags_MaskDepth)
                    rDS->data[p].depth = ds.depth;
                rDS->data[p].stencil &= ~(sFlags & BADGPUSessionFlags_StencilAll);
                rDS->data[p].stencil |= ds.stencil & sFlags;
                p++;
            }
        }
    }
    return 1;
}

static void bswDrawPoint(
    struct BADGPUInstanceSWTNL * instance,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    float plSize
) {
    int vpW, vpH;
    BADGPUTextureSW * rTex = BG_TEXTURE_SW(ctx->sTexture);
    BADGPUDSBufferSW * rDS = BG_DSBUFFER_SW(ctx->sDSBuffer);
    badgpu_rect_t region;
    if (!bswVSize(ctx->sTexture, ctx->sDSBuffer, ctx->sFlags, ctx->sScX, ctx->sScY, ctx->sScWidth, ctx->sScHeight, &vpW, &vpH, &region))
        return;
    badgpu_swrop_t rop;
    badgpu_ropConfigure(&rop, ctx->sFlags, ctx->blendProgram);
    // would be nice if there was something actually here
    return;
}

static void bswDrawLine(
    struct BADGPUInstanceSWTNL * instance,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    BADGPURasterizerVertex b,
    float plSize
) {
    int vpW, vpH;
    BADGPUTextureSW * rTex = BG_TEXTURE_SW(ctx->sTexture);
    BADGPUDSBufferSW * rDS = BG_DSBUFFER_SW(ctx->sDSBuffer);
    badgpu_rect_t region;
    if (!bswVSize(ctx->sTexture, ctx->sDSBuffer, ctx->sFlags, ctx->sScX, ctx->sScY, ctx->sScWidth, ctx->sScHeight, &vpW, &vpH, &region))
        return;
    badgpu_swrop_t rop;
    badgpu_ropConfigure(&rop, ctx->sFlags, ctx->blendProgram);
    // would be nice if there was something actually here
    return;
}

static void bswDrawTriangle(
    struct BADGPUInstanceSWTNL * instance,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    BADGPURasterizerVertex b,
    BADGPURasterizerVertex c
) {
    int vpW, vpH;
    BADGPUTextureSW * rTex = BG_TEXTURE_SW(ctx->sTexture);
    BADGPUDSBufferSW * rDS = BG_DSBUFFER_SW(ctx->sDSBuffer);
    badgpu_rect_t region;
    if (!bswVSize(ctx->sTexture, ctx->sDSBuffer, ctx->sFlags, ctx->sScX, ctx->sScY, ctx->sScWidth, ctx->sScHeight, &vpW, &vpH, &region))
        return;
    badgpu_swrop_t rop;
    badgpu_ropConfigure(&rop, ctx->sFlags, ctx->blendProgram);
    // would be nice if there was something actually here
    return;
}

// -- the instance --

BADGPUInstance badgpu_newSoftwareInstance(BADGPUNewInstanceFlags flags, const char ** error) {
    BADGPUInstanceSWTNL * bi = malloc(sizeof(BADGPUInstanceSWTNL));
    if (!bi) {
        if (error)
            *error = "Failed to allocate BADGPUInstance.";
        return NULL;
    }
    memset(bi, 0, sizeof(BADGPUInstanceSWTNL));
    bi->base.backendCheck = (flags & BADGPUNewInstanceFlags_BackendCheck) != 0;
    bi->base.backendCheckAggressive = (flags & BADGPUNewInstanceFlags_BackendCheckAggressive) != 0;
    bi->base.canPrintf = (flags & BADGPUNewInstanceFlags_CanPrintf) != 0;
    bi->base.isBound = 1;
    badgpu_initObj((BADGPUObject) bi, destroySWInstance);
    // vtbl
    bi->base.getMetaInfo = bswGetMetaInfo;
    bi->base.texLoadFormat = BADGPUTextureLoadFormat_ARGBI32;
    bi->base.newTexture = bswNewTexture;
    bi->base.newDSBuffer = bswNewDSBuffer;
    bi->base.generateMipmap = bswGenerateMipmap;
    bi->base.readPixelsARGBI32 = bswReadPixelsARGBI32;
    bi->base.drawClear = bswDrawClear;
    bi->base.drawGeom = badgpu_swtnl_drawGeom;
    bi->drawPoint = bswDrawPoint;
    bi->drawLine = bswDrawLine;
    bi->drawTriangle = bswDrawTriangle;
    return (BADGPUInstance) bi;
}
