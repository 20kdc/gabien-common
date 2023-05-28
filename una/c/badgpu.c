/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * BadGPU Reference Implementation
 */

#include "badgpu_internal.h"

// Types

struct BADGPUObject {
    size_t refs;
    void (*destroy)(BADGPUObject);
};

typedef struct BADGPUInstancePriv {
    struct BADGPUObject obj;
    BADGPUWSICtx ctx;
} BADGPUInstancePriv;

typedef struct BADGPUTexturePriv {
    struct BADGPUObject obj;
} BADGPUTexturePriv;

typedef struct BADGPUDSBufferPriv {
    struct BADGPUObject obj;
} BADGPUDSBufferPriv;

// Object Management

static void badgpu_initObj(BADGPUObject obj, void (*destroy)(BADGPUObject)) {
    obj->refs = 1;
    obj->destroy = destroy;
}

BADGPU_EXPORT void badgpuRef(BADGPUObject obj) {
    obj->refs++;
}

BADGPU_EXPORT BADGPUBool badgpuUnref(BADGPUObject obj) {
    obj->refs--;
    if (!obj->refs) {
        obj->destroy(obj);
        return 1;
    }
    return 0;
}

// Instance Creation

static void destroyInstance(BADGPUObject obj) {
    BADGPUInstancePriv * bi = (void *) obj;
    badgpu_destroyWsiCtx(bi->ctx);
    free(obj);
}

BADGPU_EXPORT BADGPUInstance badgpuNewInstance(uint32_t flags, char ** error) {
    BADGPUInstancePriv * bi = malloc(sizeof(BADGPUInstancePriv));
    if (!bi) {
        *error = "Failed to allocate BADGPUInstance.";
        return 0;
    }
    memset(bi, 0, sizeof(BADGPUInstancePriv));
    badgpu_initObj((BADGPUObject) bi, destroyInstance);
    BADGPUWSICtx ctx = badgpu_newWsiCtx(error);
    if (!ctx) {
        free(bi);
        return 0;
    }
    return (BADGPUInstance) bi;
}

// Texture/2D Buffer Management

BADGPU_EXPORT BADGPUTexture badgpuNewTexture(BADGPUInstance instance,
    uint32_t flags, uint16_t width, uint16_t height, const uint8_t * data);

BADGPU_EXPORT BADGPUDSBuffer badgpuNewDSBuffer(BADGPUInstance instance,
    uint16_t width, uint16_t height);

BADGPU_EXPORT void badgpuGenerateMipmap(BADGPUTexture texture);

BADGPU_EXPORT void badgpuReadPixels(BADGPUTexture texture,
    uint16_t x, uint16_t y, uint16_t width, uint16_t height, uint8_t * data);

// Drawing Commands

BADGPU_EXPORT void badgpuDrawClear(
    BADGPU_SESSIONFLAGS
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
);

BADGPU_EXPORT void badgpuDrawGeom(
    BADGPU_SESSIONFLAGS
    uint32_t flags,
    // Vertex Loader
    const BADGPUVertex * vertex, BADGPUPrimitiveType pType,
    uint32_t iStart, uint32_t iCount, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * matrixA, const BADGPUMatrix * matrixB,
    // DepthRange
    float depthN, float depthF,
    // Viewport
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    // Fragment Shader
    BADGPUTexture texture,
    // PolygonOffset
    float poFactor, float poUnits,
    // Alpha Test
    float alphaTestMin,
    // Stencil Test
    BADGPUCompare stFunc, uint8_t stRef, uint8_t stMask,
    BADGPUStencilOp stSF, BADGPUStencilOp stDF, BADGPUStencilOp stDP,
    // Depth Test
    BADGPUCompare dtFunc,
    // Blending
    BADGPUBlendWeight bwRGBS, BADGPUBlendWeight bwRGBD, BADGPUBlendEquation beRGB,
    BADGPUBlendWeight bwAS, BADGPUBlendWeight bwAD, BADGPUBlendEquation beA
);

