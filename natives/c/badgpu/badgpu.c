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
#define GL_COLOR_ATTACHMENT0 0x8CE0
#define GL_DEPTH_ATTACHMENT 0x8D00
#define GL_STENCIL_ATTACHMENT 0x8D20
#define GL_TEXTURE_2D 0x0DE1

#define GL_UNSIGNED_BYTE 0x1401
#define GL_UNSIGNED_SHORT 0x1403
#define GL_FLOAT 0x1406

#define GL_VERTEX_ARRAY 0x8074
#define GL_COLOR_ARRAY 0x8076
#define GL_TEXTURE_COORD_ARRAY 0x8078

#define GL_ALPHA 0x1906
#define GL_RGB 0x1907
#define GL_RGBA 0x1908
#define GL_LUMINANCE 0x1909
#define GL_LUMINANCE_ALPHA 0x190A

#define GL_ALPHA_TEST 0x0BC0
#define GL_STENCIL_TEST 0x0B90
#define GL_DEPTH_TEST 0x0B71
#define GL_SCISSOR_TEST 0x0C11
#define GL_CULL_FACE 0x0B44

#define GL_FRONT 0x0404
#define GL_BACK 0x0405

#define GL_DEPTH_BUFFER_BIT 0x00000100
#define GL_STENCIL_BUFFER_BIT 0x00000400
#define GL_COLOR_BUFFER_BIT 0x00004000

#define GL_NEAREST 0x2600
#define GL_LINEAR 0x2601
#define GL_NEAREST_MIPMAP_NEAREST 0x2700
#define GL_LINEAR_MIPMAP_LINEAR 0x2703
#define GL_REPEAT 0x2901
#define GL_CLAMP_TO_EDGE 0x812F
#define GL_TEXTURE_MAG_FILTER 0x2800
#define GL_TEXTURE_MIN_FILTER 0x2801
#define GL_TEXTURE_WRAP_S 0x2802
#define GL_TEXTURE_WRAP_T 0x2803

#define GL_MODELVIEW 0x1700
#define GL_PROJECTION 0x1701
#define GL_TEXTURE 0x1702

#define GL_POLYGON_OFFSET_FILL 0x8037

#define GL_BLEND 0x0BE2

#define GL_CW 0x0900
#define GL_CCW 0x0901

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
    void (KHRABI *glColorMask)(unsigned char, unsigned char, unsigned char, unsigned char);
    void (KHRABI *glDepthMask)(unsigned char);
    void (KHRABI *glScissor)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *glViewport)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *glClearColor)(float, float, float, float);
    void (KHRABI *glClearDepthf)(float);
    void (KHRABI *glDepthRangef)(float, float);
    void (KHRABI *glPolygonOffset)(float, float);
    void (KHRABI *glPointSize)(float);
    void (KHRABI *glLineWidth)(float);
    void (KHRABI *glClearStencil)(int32_t);
    void (KHRABI *glClear)(int32_t);
    void (KHRABI *glReadPixels)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, void *);
    void (KHRABI *glBindTexture)(int32_t, uint32_t);
    void (KHRABI *glTexImage2D)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, const void *);
    void (KHRABI *glTexParameteri)(int32_t, int32_t, int32_t);
    void (KHRABI *glDrawArrays)(int32_t, int32_t, int32_t);
    void (KHRABI *glDrawElements)(int32_t, int32_t, int32_t, const void *);
    void (KHRABI *glVertexPointer)(int32_t, int32_t, int32_t, const void *);
    void (KHRABI *glColorPointer)(int32_t, int32_t, int32_t, const void *);
    void (KHRABI *glTexCoordPointer)(int32_t, int32_t, int32_t, const void *);
    void (KHRABI *glMatrixMode)(int32_t);
    void (KHRABI *glLoadMatrixf)(const float *);
    void (KHRABI *glLoadIdentity)();
    void (KHRABI *glAlphaFunc)(int32_t, float);
    void (KHRABI *glFrontFace)(int32_t);
    void (KHRABI *glCullFace)(int32_t);
    void (KHRABI *glDepthFunc)(int32_t);
    void (KHRABI *glStencilFunc)(int32_t, int32_t, int32_t);
    void (KHRABI *glStencilOp)(int32_t, int32_t, int32_t);
    void (KHRABI *glBlendFuncSeparate)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *glBlendEquationSeparate)(int32_t, int32_t);
    // Desktop/Non-Desktop variable area
    void (KHRABI *glGenFramebuffers)(int32_t, uint32_t *);
    void (KHRABI *glDeleteFramebuffers)(int32_t, uint32_t *);
    void (KHRABI *glGenRenderbuffers)(int32_t, uint32_t *);
    void (KHRABI *glDeleteRenderbuffers)(int32_t, uint32_t *);
    void (KHRABI *glRenderbufferStorage)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *glBindFramebuffer)(int32_t, uint32_t);
    void (KHRABI *glFramebufferRenderbuffer)(int32_t, int32_t, int32_t, uint32_t);
    void (KHRABI *glFramebufferTexture2D)(int32_t, int32_t, int32_t, uint32_t, int32_t);
    void (KHRABI *glGenerateMipmap)(int32_t);
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
    bi->glColorMask = badgpu_wsiCtxGetProcAddress(bi->ctx, "glColorMask");
    bi->glDepthMask = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDepthMask");
    bi->glScissor = badgpu_wsiCtxGetProcAddress(bi->ctx, "glScissor");
    bi->glViewport = badgpu_wsiCtxGetProcAddress(bi->ctx, "glViewport");
    bi->glClearColor = badgpu_wsiCtxGetProcAddress(bi->ctx, "glClearColor");
    bi->glClearDepthf = badgpu_wsiCtxGetProcAddress(bi->ctx, "glClearDepthf");
    bi->glDepthRangef = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDepthRangef");
    bi->glPolygonOffset = badgpu_wsiCtxGetProcAddress(bi->ctx, "glPolygonOffset");
    bi->glPointSize = badgpu_wsiCtxGetProcAddress(bi->ctx, "glPointSize");
    bi->glLineWidth = badgpu_wsiCtxGetProcAddress(bi->ctx, "glLineWidth");
    bi->glClearStencil = badgpu_wsiCtxGetProcAddress(bi->ctx, "glClearStencil");
    bi->glClear = badgpu_wsiCtxGetProcAddress(bi->ctx, "glClear");
    bi->glReadPixels = badgpu_wsiCtxGetProcAddress(bi->ctx, "glReadPixels");
    bi->glBindTexture = badgpu_wsiCtxGetProcAddress(bi->ctx, "glBindTexture");
    bi->glTexImage2D = badgpu_wsiCtxGetProcAddress(bi->ctx, "glTexImage2D");
    bi->glTexParameteri = badgpu_wsiCtxGetProcAddress(bi->ctx, "glTexParameteri");
    bi->glDrawArrays = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDrawArrays");
    bi->glDrawElements = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDrawElements");
    bi->glVertexPointer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glVertexPointer");
    bi->glColorPointer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glColorPointer");
    bi->glTexCoordPointer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glTexCoordPointer");
    bi->glMatrixMode = badgpu_wsiCtxGetProcAddress(bi->ctx, "glMatrixMode");
    bi->glLoadMatrixf = badgpu_wsiCtxGetProcAddress(bi->ctx, "glLoadMatrixf");
    bi->glLoadIdentity = badgpu_wsiCtxGetProcAddress(bi->ctx, "glLoadIdentity");
    bi->glAlphaFunc = badgpu_wsiCtxGetProcAddress(bi->ctx, "glAlphaFunc");
    bi->glFrontFace = badgpu_wsiCtxGetProcAddress(bi->ctx, "glFrontFace");
    bi->glCullFace = badgpu_wsiCtxGetProcAddress(bi->ctx, "glCullFace");
    bi->glDepthFunc = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDepthFunc");
    bi->glStencilFunc = badgpu_wsiCtxGetProcAddress(bi->ctx, "glStencilFunc");
    bi->glStencilOp = badgpu_wsiCtxGetProcAddress(bi->ctx, "glStencilOp");
    bi->glBlendFuncSeparate = badgpu_wsiCtxGetProcAddress(bi->ctx, "glBlendFuncSeparate");
    bi->glBlendEquationSeparate = badgpu_wsiCtxGetProcAddress(bi->ctx, "glBlendEquationSeparate");
    if (desktopExt) {
        bi->glGenFramebuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenFramebuffersEXT");
        bi->glDeleteFramebuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDeleteFramebuffersEXT");
        bi->glGenRenderbuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenRenderbuffersEXT");
        bi->glDeleteRenderbuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDeleteRenderbuffersEXT");
        bi->glRenderbufferStorage = badgpu_wsiCtxGetProcAddress(bi->ctx, "glRenderbufferStorageEXT");
        bi->glBindFramebuffer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glBindFramebufferEXT");
        bi->glFramebufferRenderbuffer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glFramebufferRenderbufferEXT");
        bi->glFramebufferTexture2D = badgpu_wsiCtxGetProcAddress(bi->ctx, "glFramebufferTexture2DEXT");
        bi->glGenerateMipmap = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenerateMipmapEXT");
    } else {
        bi->glGenFramebuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenFramebuffersOES");
        bi->glDeleteFramebuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDeleteFramebuffersOES");
        bi->glGenRenderbuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenRenderbuffersOES");
        bi->glDeleteRenderbuffers = badgpu_wsiCtxGetProcAddress(bi->ctx, "glDeleteRenderbuffersOES");
        bi->glRenderbufferStorage = badgpu_wsiCtxGetProcAddress(bi->ctx, "glRenderbufferStorageOES");
        bi->glBindFramebuffer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glBindFramebufferOES");
        bi->glFramebufferRenderbuffer = badgpu_wsiCtxGetProcAddress(bi->ctx, "glFramebufferRenderbufferOES");
        bi->glFramebufferTexture2D = badgpu_wsiCtxGetProcAddress(bi->ctx, "glFramebufferTexture2DOES");
        bi->glGenerateMipmap = badgpu_wsiCtxGetProcAddress(bi->ctx, "glGenerateMipmapOES");
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
    // badgpuChk(*bi, "fbSetup1");
    if (sTex) {
        (*bi)->glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, sTex->tex, 0);
    } else {
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, 0);
    }
    // badgpuChk(*bi, "fbSetup2");
    if (sDS) {
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, sDS->rbo);
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, sDS->rbo);
    } else {
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, 0);
    }
    badgpuChk(*bi, "fbSetup");
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

    bi->glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, flags & BADGPUTextureFlags_WrapS ? GL_REPEAT : GL_CLAMP_TO_EDGE);
    bi->glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, flags & BADGPUTextureFlags_WrapT ? GL_REPEAT : GL_CLAMP_TO_EDGE);

    int32_t minFilter = ((flags & BADGPUTextureFlags_Mipmap) ?
        ((flags & BADGPUTextureFlags_MinLinear) ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST_MIPMAP_NEAREST)
        :
        ((flags & BADGPUTextureFlags_MinLinear) ? GL_LINEAR : GL_NEAREST)
    );
    bi->glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
    bi->glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, flags & BADGPUTextureFlags_MagLinear ? GL_LINEAR : GL_NEAREST);

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
    tex->i->glBindTexture(GL_TEXTURE_2D, tex->tex);
    tex->i->glGenerateMipmap(GL_TEXTURE_2D);
    badgpuChk(tex->i, "badgpuGenerateMipmap");
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
    (*bi)->glStencilMask(sFlags & BADGPUSessionFlags_StencilAll);
    (*bi)->glColorMask(
        sFlags & BADGPUSessionFlags_MaskR ? 1 : 0,
        sFlags & BADGPUSessionFlags_MaskG ? 1 : 0,
        sFlags & BADGPUSessionFlags_MaskB ? 1 : 0,
        sFlags & BADGPUSessionFlags_MaskA ? 1 : 0
    );
    (*bi)->glDepthMask(sFlags & BADGPUSessionFlags_MaskDepth ? 1 : 0);
    if (sFlags & BADGPUSessionFlags_Scissor) {
        (*bi)->glEnable(GL_SCISSOR_TEST);
        (*bi)->glScissor(sScX, sScY, sScWidth, sScHeight);
    } else {
        (*bi)->glDisable(GL_SCISSOR_TEST);
    }
    return 0;
}

BADGPU_EXPORT void badgpuDrawClear(
    BADGPU_SESSIONFLAGS,
    uint8_t cR, uint8_t cG, uint8_t cB, uint8_t cA, float depth, uint8_t stencil
) {
    BADGPUInstancePriv * bi;
    if (drawingCmdSetup(BADGPU_SESSIONFLAGS_PASSTHROUGH, &bi))
        return;
    int32_t cFlags = 0;
    if (sFlags & BADGPUSessionFlags_MaskRGBA) {
        bi->glClearColor(cR / 255.0f, cG / 255.0f, cB / 255.0f, cA / 255.0f);
        cFlags |= GL_COLOR_BUFFER_BIT;
    }
    if (sFlags & BADGPUSessionFlags_MaskDepth) {
        bi->glClearDepthf(depth);
        cFlags |= GL_DEPTH_BUFFER_BIT;
    }
    if (sFlags & BADGPUSessionFlags_StencilAll) {
        bi->glClearStencil(stencil);
        cFlags |= GL_STENCIL_BUFFER_BIT;
    }
    if (cFlags)
        bi->glClear(cFlags);
}

BADGPU_EXPORT void badgpuDrawGeom(
    BADGPU_SESSIONFLAGS,
    uint32_t flags,
    // Vertex Loader
    const BADGPUVertex * vertex, BADGPUPrimitiveType pType, float plSize,
    uint32_t iStart, uint32_t iCount, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * matrixA, const BADGPUMatrix * matrixB,
    // DepthRange
    float depthN, float depthF,
    // Viewport
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    // Fragment Shader
    BADGPUTexture texture, const BADGPUMatrix * matrixT,
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

    // Vertex Shader
    bi->glMatrixMode(GL_PROJECTION);
    if (!matrixA) bi->glLoadIdentity(); else bi->glLoadMatrixf((void *) matrixA);
    bi->glMatrixMode(GL_MODELVIEW);
    if (!matrixB) bi->glLoadIdentity(); else bi->glLoadMatrixf((void *) matrixB);

    // DepthRange/Viewport
    bi->glDepthRangef(depthN, depthF);
    bi->glViewport(vX, vY, vW, vH);

    // Fragment Shader
    if (texture) {
        bi->glEnable(GL_TEXTURE_2D);
        bi->glBindTexture(GL_TEXTURE_2D, BG_TEXTURE(texture)->tex);
        bi->glMatrixMode(GL_TEXTURE);
        if (!matrixT) bi->glLoadIdentity(); else bi->glLoadMatrixf((void *) matrixT);
    } else {
        bi->glDisable(GL_TEXTURE_2D);
    }

    // PolygonOffset
    bi->glEnable(GL_POLYGON_OFFSET_FILL);
    bi->glPolygonOffset(poFactor, poUnits);

    // Alpha Test
    bi->glEnable(GL_ALPHA_TEST);
    bi->glAlphaFunc(flags & BADGPUDrawFlags_AlphaTestInvert ? BADGPUCompare_Less : BADGPUCompare_GEqual, alphaTestMin);

    // Stencil Test
    if (flags & BADGPUDrawFlags_StencilTest) {
        bi->glEnable(GL_STENCIL_TEST);
        // no conversion as values deliberately match
        bi->glStencilFunc(stFunc, stRef, stMask);
        bi->glStencilOp(stSF, stDF, stDP);
    } else {
        bi->glDisable(GL_STENCIL_TEST);
    }

    // Depth Test
    if (flags & BADGPUDrawFlags_DepthTest) {
        bi->glEnable(GL_DEPTH_TEST);
        // no conversion as values deliberately match
        bi->glDepthFunc(dtFunc);
    } else {
        bi->glDisable(GL_DEPTH_TEST);
    }

    // Misc. Flags Stuff
    bi->glFrontFace(flags & BADGPUDrawFlags_FrontFaceCW ? GL_CW : GL_CCW);

    if (flags & BADGPUDrawFlags_CullFace) {
        bi->glEnable(GL_CULL_FACE);
        bi->glCullFace(flags & BADGPUDrawFlags_CullFaceFront ? GL_FRONT : GL_BACK);
    } else {
        bi->glDisable(GL_CULL_FACE);
    }

    // Blending
    if (flags & BADGPUDrawFlags_Blend) {
        bi->glEnable(GL_BLEND);
        // no conversion as values deliberately match
        bi->glBlendFuncSeparate(bwRGBS, bwRGBD, bwAS, bwAD);
        bi->glBlendEquationSeparate(beRGB, beA);
    } else {
        bi->glDisable(GL_BLEND);
    }

    if (pType == BADGPUPrimitiveType_Points) {
        bi->glPointSize(plSize);
    } else if (pType == BADGPUPrimitiveType_Lines) {
        bi->glLineWidth(plSize);
    }

    bi->glEnableClientState(GL_VERTEX_ARRAY);
    bi->glVertexPointer(4, GL_FLOAT, sizeof(BADGPUVertex), &vertex->x);
    bi->glEnableClientState(GL_COLOR_ARRAY);
    bi->glColorPointer(4, GL_FLOAT, sizeof(BADGPUVertex), &vertex->cR);
    bi->glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    bi->glTexCoordPointer(4, GL_FLOAT, sizeof(BADGPUVertex), &vertex->tS);

    if (indices) {
        bi->glDrawElements(pType, iCount, GL_UNSIGNED_SHORT, indices + iStart);
    } else {
        bi->glDrawArrays(pType, iStart, iCount);
    }
    badgpuChk(bi, "badgpuDrawGeom");
}

BADGPU_EXPORT void badgpuDrawGeomNoDS(
    BADGPUTexture sTexture,
    uint32_t sFlags,
    int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight,
    uint32_t flags,
    // Vertex Loader
    const BADGPUVertex * vertex, BADGPUPrimitiveType pType, float plSize,
    uint32_t iStart, uint32_t iCount, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * matrixA, const BADGPUMatrix * matrixB,
    // Viewport
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    // Fragment Shader
    BADGPUTexture texture, const BADGPUMatrix * matrixT,
    // Alpha Test
    float alphaTestMin,
    // Blending
    BADGPUBlendWeight bwRGBS, BADGPUBlendWeight bwRGBD, BADGPUBlendEquation beRGB,
    BADGPUBlendWeight bwAS, BADGPUBlendWeight bwAD, BADGPUBlendEquation beA
) {
    badgpuDrawGeom(
    sTexture, NULL,
    sFlags,
    sScX, sScY, sScWidth, sScHeight,
    flags,
    vertex, pType, plSize,
    iStart, iCount, indices,
    matrixA, matrixB,
    0, 0,
    vX, vY, vW, vH,
    texture, matrixT,
    0, 0,
    alphaTestMin,
    BADGPUCompare_Always, 0, 0,
    BADGPUStencilOp_Keep, BADGPUStencilOp_Keep, BADGPUStencilOp_Keep,
    BADGPUCompare_Always,
    bwRGBS, bwRGBD, beRGB,
    bwAS, bwAD, beA
    );
}

