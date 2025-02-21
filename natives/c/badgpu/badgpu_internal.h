/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#ifndef BADGPU_INTERNAL_H_
#define BADGPU_INTERNAL_H_

#include <stddef.h>

#ifdef WIN32
#include <windows.h>
#endif

#include "badgpu.h"

// WSI stuff

#ifdef WIN32
#define KHRABI __stdcall
#else
#define KHRABI
#endif

#ifndef WIN32
// Because Zig can get picky about the standard library, let's define these...
void * malloc(size_t sz);
void free(void * mem);
void * memset(void * mem, int c, size_t len);
void * memcpy(void * dst, const void * src, size_t len);
void * dlopen(const char * fn, int flags);
void * dlsym(void * mod, const char * symbol);
int dlclose(void * mod);
#ifndef ANDROID
int printf(const char * fmt, ...);
#else
#include <stdarg.h>
int __android_log_vprint(int prio, const char * tag, const char * fmt, va_list ap);
static inline int printf(const char * fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    int res = __android_log_vprint(3, "BadGPU", fmt, ap);
    va_end(ap);
    return res;
}
#endif
int strcmp(const char * a, const char * b);
char * strstr(const char * h, const char * n);
#else
#include <stdio.h>
#endif

// Separate declaration of these so they don't end up in API.
// Keep in sync with badgpu.h!

#define BADGPU_SESSIONFLAGS \
    BADGPUTexture sTexture, BADGPUDSBuffer sDSBuffer, \
    uint32_t sFlags, \
    int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight

#define BADGPU_SESSIONFLAGS_PASSTHROUGH \
    sTexture, sDSBuffer, \
    sFlags, \
    sScX, sScY, sScWidth, sScHeight

// DL

// Given a NULL-terminated location list, and an environment variable, loads a native library.
// Returns NULL on error.
typedef struct BADGPUDynLib * BADGPUDynLib;
BADGPUDynLib badgpu_dlOpen(const char ** locations, const char * env);
void * badgpu_dlSym(BADGPUDynLib lib, const char * sym);
void * badgpu_dlSym2(BADGPUDynLib lib, const char * sym1, const char * sym2);
void * badgpu_dlSym4(BADGPUDynLib lib, const char * sym1, const char * sym2, const char * sym3, const char * sym4);
void badgpu_dlClose(BADGPUDynLib lib);

BADGPUBool badgpu_getEnvFlag(const char * flag);

// WSI

// Creates a new WSICtx.
BADGPUWSIContext badgpu_newWsiCtxPlatform(const char ** error, BADGPUBool logDetailed);
// Determines if the platform context creation is EGL.
BADGPUBool badgpu_newWsiCtxPlatformIsEGL();
// Creates a new WSICtx.
BADGPUWSIContext badgpu_newWsiCtxEGL(const char ** error, BADGPUBool logDetailed);

// Creates a new software renderer instance.
BADGPUInstance badgpu_newSoftwareInstance(BADGPUNewInstanceFlags flags, const char ** error);

// OBJ

// Object Management

struct BADGPUObject {
    size_t refs;
    void (*destroy)(BADGPUObject);
};

static inline void badgpu_initObj(BADGPUObject obj, void (*destroy)(BADGPUObject)) {
    obj->refs = 1;
    obj->destroy = destroy;
}

// IUTIL

// Finds the vertex count for the given indices array.
uint32_t badgpu_findVertexCount(uint32_t iStart, uint32_t iCount, const uint16_t * indices);

#define BADGPU_BP_RGBS(blendProgram) (((blendProgram) >> 24) & 077)
#define BADGPU_BP_RGBD(blendProgram) (((blendProgram) >> 18) & 077)
#define BADGPU_BP_AS(blendProgram)   (((blendProgram) >>  9) & 077)
#define BADGPU_BP_AD(blendProgram)   (((blendProgram) >>  3) & 077)
#define BADGPU_BP_RGBE(blendProgram) (((blendProgram) >> 15) & 07)
#define BADGPU_BP_AE(blendProgram)   ((blendProgram) & 07)

// Instance Base

typedef struct BADGPURasterizerContext {
    BADGPUTexture sTexture;
    BADGPUDSBuffer sDSBuffer;
    uint32_t sFlags;
    // Scissor
    int32_t sScX, sScY, sScWidth, sScHeight;
    uint32_t flags;
    // Viewport
    int32_t vX, vY, vW, vH;
    // Fragment Shader
    BADGPUTexture texture;
    const float * clipPlane;
    BADGPUCompare atFunc;
    float atRef;
    // Stencil Test
    BADGPUCompare stFunc;
    uint8_t stRef, stMask;
    BADGPUStencilOp stSF, stDF, stDP;
    // Depth Test / DepthRange / PolygonOffset
    BADGPUCompare dtFunc;
    float depthN, depthF, poFactor, poUnits;
    // Blending
    uint32_t blendProgram;
} BADGPURasterizerContext;

typedef struct BADGPURasterizerVertex {
    // Position in clip coordinates (ES11p27)
    BADGPUVector p;
    // Colour.
    BADGPUVector c;
    // Texture coordinate.
    float u, v;
} BADGPURasterizerVertex;

struct BADGPUInstancePriv;

typedef BADGPUBool (*badgpu_drawGeom_t)(
    struct BADGPUInstancePriv *,
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
);

typedef void (*badgpu_drawPoint_t)(
    struct BADGPUInstancePriv *,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    float plSize
);
typedef void (*badgpu_drawLine_t)(
    struct BADGPUInstancePriv *,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    BADGPURasterizerVertex b,
    float plSize
);
typedef void (*badgpu_drawTriangle_t)(
    struct BADGPUInstancePriv *,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    BADGPURasterizerVertex b,
    BADGPURasterizerVertex c
);

typedef struct BADGPUInstancePriv {
    struct BADGPUObject obj;
    int isBound;
    int backendCheck;
    int backendCheckAggressive;
    int canPrintf;
    BADGPUWSIContext ctx;
    // vtbl
    BADGPUBool (*bind)(struct BADGPUInstancePriv *); // optional (defaults to NOP)
    void (*unbind)(struct BADGPUInstancePriv *); // optional (defaults to NOP)
    void (*flush)(struct BADGPUInstancePriv *); // optional (defaults to NOP)
    void (*finish)(struct BADGPUInstancePriv *); // optional (defaults to NOP)
    BADGPUBool (*resetGLState)(struct BADGPUInstancePriv *); // optional (defaults to fail)
    BADGPUTexture (*newTextureFromGL)(struct BADGPUInstancePriv *, uint32_t glTex); // optional (defaults to fail)
    // --
    const char * (*getMetaInfo)(struct BADGPUInstancePriv *, BADGPUMetaInfoType);
    BADGPUTextureLoadFormat texLoadFormat;
    BADGPUTexture (*newTexture)(struct BADGPUInstancePriv *, int16_t width, int16_t height, const void * data);
    BADGPUDSBuffer (*newDSBuffer)(struct BADGPUInstancePriv * instance, int16_t width, int16_t height);
    BADGPUBool (*generateMipmap)(void *);
    // one of these two must be implemented
    BADGPUBool (*readPixelsRGBA8888)(void *, uint16_t x, uint16_t y, int16_t width, int16_t height, void * data);
    BADGPUBool (*readPixelsARGBI32)(void *, uint16_t x, uint16_t y, int16_t width, int16_t height, uint32_t * data);
    BADGPUBool (*drawClear)(
        struct BADGPUInstancePriv *,
        BADGPU_SESSIONFLAGS,
        float cR, float cG, float cB, float cA, float depth, uint8_t stencil
    );
    // Ok, so some explanation of what's going on here:
    // We need to be able to test SW components (doesn't work) in the HW backend (known-good).
    // SW and HW use different pipeline orientations, and doing this testing flips the orientation on its head.
    // SW: drawGeomFrontend (VS/PA) -> drawPLTFrontend (clipper) -> drawPLTBackend (rasterizer).
    // HW: drawGeomFrontend.
    // HW w/ SWTnL: drawGeomFrontend -> drawPLTFrontend -> drawGeomBackend.
    // HW w/ SWTnL & SWClip: drawGeomFrontend -> drawPLTFrontend -> drawPLTBackend -> drawGeomBackend.
    badgpu_drawGeom_t drawGeomFrontend, drawGeomBackend;
    // If the true backend has no implementation it should use badgpu_swtnl_harness*.
    badgpu_drawPoint_t drawPointFrontend, drawPointBackend;
    badgpu_drawLine_t drawLineFrontend, drawLineBackend;
    badgpu_drawTriangle_t drawTriangleFrontend, drawTriangleBackend;
} BADGPUInstancePriv;
#define BG_INSTANCE(x) ((BADGPUInstancePriv *) (x))

typedef struct BADGPUTexturePriv {
    struct BADGPUObject obj;
    BADGPUInstancePriv * i;
    // Value returned from badgpuGetGLTexture
    uint32_t glTex;
    int autoDel;
} BADGPUTexturePriv;
#define BG_TEXTURE(x) ((BADGPUTexturePriv *) (x))

typedef struct BADGPUDSBufferPriv {
    struct BADGPUObject obj;
    BADGPUInstancePriv * i;
} BADGPUDSBufferPriv;
#define BG_DSBUFFER(x) ((BADGPUDSBufferPriv *) (x))

static inline BADGPUBool badgpuErr(BADGPUInstancePriv * instance, const char * location) {
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (bi->canPrintf)
        printf("BADGPU: %s\n", location);
    return 0;
}

// Checks that the instance is bound.
static inline BADGPUInstancePriv * badgpuBChk(BADGPUInstance bi, const char * location) {
    BADGPUInstancePriv * bip = BG_INSTANCE(bi);
    if (!bip)
        return 0;
    if (!bip->isBound) {
        if (bip->canPrintf)
            printf("BADGPU: %s: Instance not bound\n", location);
        return 0;
    }
    return bip;
}

// Vector maths

static inline float badgpu_lerp(float a, float b, float v) {
    return (a * (1 - v)) + (b * v);
}

static inline BADGPUVector badgpu_vecLerp(BADGPUVector a, BADGPUVector b, float v) {
    BADGPUVector res = {
        badgpu_lerp(a.x, b.x, v),
        badgpu_lerp(a.y, b.y, v),
        badgpu_lerp(a.z, b.z, v),
        badgpu_lerp(a.w, b.w, v)
    };
    return res;
}

static inline BADGPUVector badgpu_vectorByMatrix(BADGPUVector v, const BADGPUMatrix * matrix) {
    BADGPUVector out = {
        (matrix->x.x * v.x) + (matrix->y.x * v.y) + (matrix->z.x * v.z) + (matrix->w.x * v.w),
        (matrix->x.y * v.x) + (matrix->y.y * v.y) + (matrix->z.y * v.z) + (matrix->w.y * v.w),
        (matrix->x.z * v.x) + (matrix->y.z * v.y) + (matrix->z.z * v.z) + (matrix->w.z * v.w),
        (matrix->x.w * v.x) + (matrix->y.w * v.y) + (matrix->z.w * v.z) + (matrix->w.w * v.w)
    };
    return out;
}


// Instance w/ Software TnL

static inline BADGPURasterizerVertex badgpu_rvtxLerp(BADGPURasterizerVertex a, BADGPURasterizerVertex b, float v) {
    BADGPURasterizerVertex res = {
        .p = badgpu_vecLerp(a.p, b.p, v),
        .c = badgpu_vecLerp(a.c, b.c, v),
        .u = badgpu_lerp(a.u, b.u, v),
        .v = badgpu_lerp(a.v, b.v, v)
    };
    return res;
}

void badgpu_swtnl_transform(
    uint32_t flags,
    // Vertex Loader
    int32_t vPosD, const float * vPos,
    const float * vCol,
    int32_t vTCD, const float * vTC,
    uint32_t iStart, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * mvMatrix,
    // Fragment Shader
    const BADGPUMatrix * matrixT,
    BADGPURasterizerVertex * out
);

BADGPUBool badgpu_swtnl_drawGeom(
    struct BADGPUInstancePriv *,
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
);

// emulation harness

void badgpu_swtnl_harnessDrawPoint(struct BADGPUInstancePriv *, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, float plSize);
void badgpu_swtnl_harnessDrawLine(struct BADGPUInstancePriv *, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, float plSize);
void badgpu_swtnl_harnessDrawTriangle(struct BADGPUInstancePriv *, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, BADGPURasterizerVertex c);

// clipper

void badgpu_swclip_drawPoint(struct BADGPUInstancePriv *, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, float plSize);
void badgpu_swclip_drawLine(struct BADGPUInstancePriv *, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, float plSize);
void badgpu_swclip_drawTriangle(struct BADGPUInstancePriv *, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, BADGPURasterizerVertex c);

#endif

