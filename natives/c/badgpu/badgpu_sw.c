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

typedef struct {
    uint8_t r, g, b, a;
} badgpu_pixel_t;

typedef struct {
    float depth;
    uint8_t stencil;
} badgpu_ds_t;

typedef struct BADGPUTextureSW {
    BADGPUTexturePriv base;
    int w, h;
    badgpu_pixel_t data[];
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
    size_t datasize = ((size_t) width) * ((size_t) height) * sizeof(badgpu_pixel_t);
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

static BADGPUBool bswReadPixelsRGBA8888(void * texture, uint16_t x, uint16_t y, int16_t width, int16_t height, void * data) {
    BADGPUTextureSW * tex = BG_TEXTURE_SW(texture);
    int w = width;
    int h = height;
    if (x + w > tex->w || y + h > tex->h)
        return badgpuErr(tex->base.i, "badgpuReadPixels: Read out of range");
    const badgpu_pixel_t * texdata = tex->data;
    size_t stride = width * sizeof(badgpu_pixel_t);
    for (int i = 0; i < height; i++) {
        memcpy(data, texdata, stride);
        data += stride;
        texdata += width;
    }
    return 1;
}

// -- core maths --

static inline uint8_t f8tou8(float c) {
    int r = (int) ((c * 255) + 0.5);
    if (r < 0)
        return 0;
    if (r > 255)
        return 255;
    return r;
}

// -- renderfuncs --

static BADGPUBool bswDrawClear(
    struct BADGPUInstancePriv * instance,
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
) {
    badgpu_pixel_t pixel = { f8tou8(cR), f8tou8(cG), f8tou8(cB), f8tou8(cA) };
    badgpu_ds_t ds = { depth, stencil };
    // would be nice if there was something actually here
    return 1;
}

static BADGPUBool bswDrawGeom(
    struct BADGPUInstancePriv * instance,
    BADGPU_SESSIONFLAGS,
    uint32_t flags,
    // Vertex Loader
    int32_t vPosD, const float * vPos,
    const float * vCol,
    int32_t vTCD, const float * vTC,
    BADGPUPrimitiveType pType, float plSize,
    uint32_t iStart, uint32_t iCount, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * mvMatrix,
    // Viewport
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    // Fragment Shader
    BADGPUTexture texture, const BADGPUMatrix * matrixT,
    const float * clipPlane, BADGPUCompare atFunc, float atRef,
    // Stencil Test
    BADGPUCompare stFunc, uint8_t stRef, uint8_t stMask,
    BADGPUStencilOp stSF, BADGPUStencilOp stDF, BADGPUStencilOp stDP,
    // Depth Test / DepthRange / PolygonOffset
    BADGPUCompare dtFunc, float depthN, float depthF, float poFactor, float poUnits,
    // Blending
    uint32_t blendProgram
) {
    // would be nice if there was something actually here
    return 0;
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
    bi->base.texLoadFormat = BADGPUTextureLoadFormat_RGBA8888;
    bi->base.newTexture = bswNewTexture;
    bi->base.newDSBuffer = bswNewDSBuffer;
    bi->base.generateMipmap = bswGenerateMipmap;
    bi->base.readPixelsRGBA8888 = bswReadPixelsRGBA8888;
    bi->base.drawClear = bswDrawClear;
    bi->base.drawGeom = bswDrawGeom;
    return (BADGPUInstance) bi;
}
