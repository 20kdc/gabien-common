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

// Tokens

// Luckily, these are equal between their regular and suffixed versions.
#define GL_FRAMEBUFFER 0x8D40
#define GL_RENDERBUFFER 0x8D41
#define GL_DEPTH24_STENCIL8 0x88F0
#define GL_UNSIGNED_BYTE 0x1401
#define GL_COLOR_ATTACHMENT0 0x8CE0
#define GL_DEPTH_ATTACHMENT 0x8D00
#define GL_STENCIL_ATTACHMENT 0x8D20
#define GL_TEXTURE_2D 0x0DE1

#define GL_ALPHA 0x1906
#define GL_RGB 0x1907
#define GL_RGBA 0x1908
#define GL_LUMINANCE 0x1909
#define GL_LUMINANCE_ALPHA 0x190A

// Types

struct BADGPUObject {
    size_t refs;
    void (*destroy)(BADGPUObject);
};

typedef struct BADGPUInstancePriv {
    struct BADGPUObject obj;
    BADGPUWSICtx ctx;
    int debug;
    uint32_t fbo;
    int32_t (KHRABI *glGetError)();
    void (KHRABI *glEnable)(int32_t);
    void (KHRABI *glDisable)(int32_t);
    void (KHRABI *glEnableClientState)(int32_t);
    void (KHRABI *glDisableClientState)(int32_t);
    void (KHRABI *glGenTextures)(int32_t, uint32_t *);
    void (KHRABI *glDeleteTextures)(int32_t, uint32_t *);
    void (KHRABI *glStencilMask)(int32_t);
    void (KHRABI *glScissor)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *glReadPixels)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, void *);
    void (KHRABI *glBindTexture)(int32_t, uint32_t);
    void (KHRABI *glTexImage2D)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, const void *);
    // Desktop/Non-Desktop variable area
    void (KHRABI *glGenFramebuffers)(int32_t, uint32_t *);
    void (KHRABI *glDeleteFramebuffers)(int32_t, uint32_t *);
    void (KHRABI *glGenRenderbuffers)(int32_t, uint32_t *);
    void (KHRABI *glDeleteRenderbuffers)(int32_t, uint32_t *);
    void (KHRABI *glRenderbufferStorage)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *glBindFramebuffer)(int32_t, uint32_t);
    void (KHRABI *glFramebufferRenderbuffer)(int32_t, int32_t, int32_t, uint32_t);
    void (KHRABI *glFramebufferTexture2D)(int32_t, int32_t, int32_t, uint32_t, int32_t);
} BADGPUInstancePriv;
#define BG_INSTANCE(x) ((BADGPUInstancePriv *) (x))

typedef struct BADGPUTexturePriv {
    struct BADGPUObject obj;
    BADGPUInstancePriv * i;
    uint32_t tex;
} BADGPUTexturePriv;
#define BG_TEXTURE(x) ((BADGPUTexturePriv *) (x))

typedef struct BADGPUDSBufferPriv {
    struct BADGPUObject obj;
    BADGPUInstancePriv * i;
    uint32_t rbo;
} BADGPUDSBufferPriv;
#define BG_DSBUFFER(x) ((BADGPUDSBufferPriv *) (x))

// Object Management

static void badgpu_initObj(BADGPUObject obj, void (*destroy)(BADGPUObject)) {
    obj->refs = 1;
    obj->destroy = destroy;
}

BADGPU_EXPORT BADGPUObject badgpuRef(BADGPUObject obj) {
    obj->refs++;
    return obj;
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
    BADGPUInstancePriv * bi = BG_INSTANCE(obj);
    badgpu_wsiCtxMakeCurrent(bi->ctx);
    bi->glDeleteFramebuffers(1, &bi->fbo);
    badgpu_destroyWsiCtx(bi->ctx);
    free(obj);
}

static void badgpuChkInnards(BADGPUInstancePriv * bi, const char * location) {
    int err;
    while (err = bi->glGetError()) {
        printf("BADGPU: %s: GL error 0x%x\n", location, err);
    }
}

static inline void badgpuChk(BADGPUInstancePriv * instance, const char * location) {
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (bi->debug)
        badgpuChkInnards(bi, location);
}

BADGPU_EXPORT BADGPUInstance badgpuNewInstance(uint32_t flags, char ** error) {
    BADGPUInstancePriv * bi = malloc(sizeof(BADGPUInstancePriv));
    if (!bi) {
        *error = "Failed to allocate BADGPUInstance.";
        return 0;
    }
    memset(bi, 0, sizeof(BADGPUInstancePriv));
    bi->debug = (flags & BADGPUNewInstanceFlags_Debug) != 0;
    badgpu_initObj((BADGPUObject) bi, destroyInstance);
    int desktopExt;
    bi->ctx = badgpu_newWsiCtx(error, &desktopExt);
    if (!bi->ctx) {
        free(bi);
        return 0;
    }
    bi->glGetError = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGetError");
    bi->glEnable = badgpu_wsiCtxGetProcAddress(bi->ctx, "glEnable");
    bi->glDisable = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDisable");
    bi->glEnableClientState = badgpu_wsiCtxGetProcAddress(bi->ctx, "glEnableClientState");
    bi->glDisableClientState = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDisableClientState");
    bi->glGenTextures = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenTextures");
    bi->glDeleteTextures = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDeleteTextures");
    bi->glStencilMask = badgpu_wsiCtxGetProcAddress(bi->ctx, "glStencilMask");
    bi->glScissor = badgpu_wsiCtxGetProcAddress(bi->ctx, "glScissor");
    bi->glReadPixels = badgpu_wsiCtxGetProcAddress(bi->ctx, "glReadPixels");
    bi->glBindTexture = badgpu_wsiCtxGetProcAddress(bi->ctx, "glBindTexture");
    bi->glTexImage2D = badgpu_wsiCtxGetProcAddress(bi->ctx, "glTexImage2D");
    if (desktopExt) {
        bi->glGenFramebuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenFramebuffersEXT");
        bi->glDeleteFramebuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDeleteFramebuffersEXT");
        bi->glGenRenderbuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenRenderbuffersEXT");
        bi->glDeleteRenderbuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDeleteRenderbuffersEXT");
        bi->glRenderbufferStorage = badgpu_wsiCtxGetProcAddress(bi->ctx, "glRenderbufferStorageEXT");
        bi->glBindFramebuffer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glBindFramebufferEXT");
        bi->glFramebufferRenderbuffer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glFramebufferRenderbufferEXT");
        bi->glFramebufferTexture2D = badgpu_wsiCtxGetProcAddress(bi->ctx, "glFramebufferTexture2DEXT");
    } else {
        bi->glGenFramebuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenFramebuffersOES");
        bi->glDeleteFramebuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDeleteFramebuffersOES");
        bi->glGenRenderbuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenRenderbuffersOES");
        bi->glDeleteRenderbuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDeleteRenderbuffersOES");
        bi->glRenderbufferStorage = badgpu_wsiCtxGetProcAddress(bi->ctx, "glRenderbufferStorageOES");
        bi->glBindFramebuffer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glBindFramebufferOES");
        bi->glFramebufferRenderbuffer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glFramebufferRenderbufferOES");
        bi->glFramebufferTexture2D = badgpu_wsiCtxGetProcAddress(bi->ctx, "glFramebufferTexture2DOES");
    }
    bi->glGenFramebuffers(1, &bi->fbo);
    bi->glBindFramebuffer(GL_FRAMEBUFFER, bi->fbo);
    badgpuChk(bi, "badgpuNewInstance");
    return (BADGPUInstance) bi;
}

// FBM

static int fbSetup(BADGPUTexture sTexture, BADGPUDSBuffer sDSBuffer, BADGPUInstancePriv ** bi) {
    BADGPUTexturePriv * sTex = BG_TEXTURE(sTexture);
    BADGPUDSBufferPriv * sDS = BG_DSBUFFER(sDSBuffer);
    if (sTex)
        *bi = sTex->i;
    if (sDS)
        *bi = sDS->i;
    if (!*bi)
        return 1;
    badgpu_wsiCtxMakeCurrent((*bi)->ctx);
    (*bi)->glBindFramebuffer(GL_FRAMEBUFFER, (*bi)->fbo);
    badgpuChk(*bi, "fbSetup1");
    if (sTex) {
        (*bi)->glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, sTex->tex, 0);
    } else {
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, 0);
    }
    badgpuChk(*bi, "fbSetup2");
    if (sDS) {
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, sDS->rbo);
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, sDS->rbo);
    } else {
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, 0);
    }
    badgpuChk(*bi, "fbSetup3");
    return 0;
}

// Texture/2D Buffer Management

static void destroyTexture(BADGPUObject obj) {
    BADGPUTexturePriv * tex = BG_TEXTURE(obj);
    badgpu_wsiCtxMakeCurrent(tex->i->ctx);
    tex->i->glDeleteTextures(1, &tex->tex);
    badgpuChk(tex->i, "destroyTexture");
    badgpuUnref((BADGPUObject) tex->i);
    free(tex);
}

BADGPU_EXPORT BADGPUTexture badgpuNewTexture(BADGPUInstance instance,
    uint32_t flags, BADGPUTextureFormat format,
    uint16_t width, uint16_t height, const uint8_t * data) {
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    badgpu_wsiCtxMakeCurrent(bi->ctx);

    BADGPUTexturePriv * tex = malloc(sizeof(BADGPUTexturePriv));
    if (!tex)
        return 0;
    badgpu_initObj((BADGPUObject) tex, destroyTexture);

    tex->i = BG_INSTANCE(badgpuRef(instance));
    bi->glGenTextures(1, &tex->tex);

    int32_t ifmt = GL_LUMINANCE;
    switch (format) {
    case BADGPUTextureFormat_Alpha: ifmt = GL_ALPHA; break;
    case BADGPUTextureFormat_Luma: ifmt = GL_LUMINANCE; break;
    case BADGPUTextureFormat_LumaAlpha: ifmt = GL_LUMINANCE_ALPHA; break;
    case BADGPUTextureFormat_RGB: ifmt = GL_RGB; break;
    case BADGPUTextureFormat_RGBA: ifmt = GL_RGBA;
    }

    bi->glBindTexture(GL_TEXTURE_2D, tex->tex);
    bi->glTexImage2D(GL_TEXTURE_2D, 0, ifmt, width, height, 0, ifmt, GL_UNSIGNED_BYTE, data);

    badgpuChk(bi, "badgpuNewTexture");
    return (BADGPUTexture) tex;
}

static void destroyDSBuffer(BADGPUObject obj) {
    BADGPUDSBufferPriv * ds = BG_DSBUFFER(obj);
    badgpu_wsiCtxMakeCurrent(ds->i->ctx);
    ds->i->glDeleteRenderbuffers(1, &ds->rbo);
    badgpuChk(ds->i, "destroyDSBuffer");
    badgpuUnref((BADGPUObject) ds->i);
    free(ds);
}

BADGPU_EXPORT BADGPUDSBuffer badgpuNewDSBuffer(BADGPUInstance instance,
    uint16_t width, uint16_t height) {
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    badgpu_wsiCtxMakeCurrent(bi->ctx);

    BADGPUDSBufferPriv * ds = malloc(sizeof(BADGPUDSBufferPriv));
    if (!ds)
        return 0;
    badgpu_initObj((BADGPUObject) ds, destroyDSBuffer);

    ds->i = BG_INSTANCE(badgpuRef(instance));
    bi->glGenRenderbuffers(1, &ds->rbo);

    bi->glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);

    badgpuChk(bi, "badgpuNewDSBuffer");
    return (BADGPUDSBuffer) ds;
}

BADGPU_EXPORT void badgpuGenerateMipmap(BADGPUTexture texture) {
    BADGPUTexturePriv * tex = BG_TEXTURE(texture);
    badgpu_wsiCtxMakeCurrent(tex->i->ctx);
}

BADGPU_EXPORT void badgpuReadPixels(BADGPUTexture texture,
    uint16_t x, uint16_t y, uint16_t width, uint16_t height, uint8_t * data) {
    BADGPUInstancePriv * bi;
    if (fbSetup(texture, NULL, &bi))
        return;
    bi->glReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
    badgpuChk(bi, "badgpuReadPixels");
}

// Drawing Commands

static int drawingCmdSetup(
    BADGPU_SESSIONFLAGS,
    BADGPUInstancePriv ** bi
) {
    if (fbSetup(sTexture, sDSBuffer, bi))
        return 1;
    (*bi)->glStencilMask(sStencilMask);
    (*bi)->glScissor(sScX, sScY, sScWidth, sScHeight);
    return 0;
}

BADGPU_EXPORT void badgpuDrawClear(
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
) {
    BADGPUInstancePriv * bi;
    if (drawingCmdSetup(BADGPU_SESSIONFLAGS_PASSTHROUGH, &bi))
        return;
}

BADGPU_EXPORT void badgpuDrawGeom(
    BADGPU_SESSIONFLAGS,
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
) {
    BADGPUInstancePriv * bi;
    if (drawingCmdSetup(BADGPU_SESSIONFLAGS_PASSTHROUGH, &bi))
        return;
}

