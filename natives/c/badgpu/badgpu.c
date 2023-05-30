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

#define GL_TEXTURE0 0x84C0

// Types

struct BADGPUObject {
    size_t refs;
    void (*destroy)(BADGPUObject);
};

typedef struct BADGPUInstancePriv {
    struct BADGPUObject obj;
    BADGPUWSICtx ctx;
    int backendCheck;
    int backendCheckAggressive;
    int canPrintf;
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
    void (KHRABI *glColor4f)(float, float, float, float);
    void (KHRABI *glMultiTexCoord4f)(int32_t, float, float, float, float);
    const char * (KHRABI *glGetString)(int32_t);
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
    if (!obj)
        return 0;

    obj->refs++;
    return obj;
}

BADGPU_EXPORT BADGPUBool badgpuUnref(BADGPUObject obj) {
    if (!obj)
        return 0;

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

static BADGPUBool badgpuChkInnards(BADGPUInstancePriv * bi, const char * location) {
    BADGPUBool ok = 1;
    while (1) {
        int err = bi->glGetError();
        if (!err)
            break;
        if (bi->canPrintf)
            printf("BADGPU: %s: GL error 0x%x\n", location, err);
        ok = 0;
    }
    return ok;
}

static inline BADGPUBool badgpuChk(BADGPUInstancePriv * instance, const char * location, BADGPUBool failureIsAggressive) {
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (bi->backendCheck)
        return badgpuChkInnards(bi, location) || (failureIsAggressive && !bi->backendCheckAggressive);
    return 1;
}

static inline BADGPUBool badgpuErr(BADGPUInstancePriv * instance, const char * location) {
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (bi->canPrintf)
        printf("BADGPU: %s\n", location);
    return 0;
}

BADGPU_EXPORT BADGPUInstance badgpuNewInstance(uint32_t flags, const char ** error) {
    BADGPUInstancePriv * bi = malloc(sizeof(BADGPUInstancePriv));
    if (!bi) {
        if (error)
            *error = "Failed to allocate BADGPUInstance.";
        return NULL;
    }
    memset(bi, 0, sizeof(BADGPUInstancePriv));
    bi->backendCheck = (flags & BADGPUNewInstanceFlags_BackendCheck) != 0;
    bi->backendCheckAggressive = (flags & BADGPUNewInstanceFlags_BackendCheckAggressive) != 0;
    bi->canPrintf = (flags & BADGPUNewInstanceFlags_CanPrintf) != 0;
    badgpu_initObj((BADGPUObject) bi, destroyInstance);
    int desktopExt;
    bi->ctx = badgpu_newWsiCtx(error, &desktopExt);
    if (!bi->ctx) {
        free(bi);
        // error provided by badgpu_newWsiCtx
        return NULL;
    }
#define CHKGLFN(fn) \
if (!(bi->fn)) { \
    badgpu_destroyWsiCtx(bi->ctx); \
    if (error) \
        *error = "Failed to bind function: " #fn; \
    free(bi); \
    return NULL; \
}
#define BINDGLFN(fn) bi->fn = badgpu_wsiCtxGetProcAddress(bi->ctx, #fn); \
CHKGLFN(fn)
#define BINDGLFN2(fn, ext) bi->fn = badgpu_wsiCtxGetProcAddress(bi->ctx, #fn #ext); \
CHKGLFN(fn)
    BINDGLFN(glGetError);
    BINDGLFN(glEnable);
    BINDGLFN(glDisable);
    BINDGLFN(glEnableClientState);
    BINDGLFN(glDisableClientState);
    BINDGLFN(glGenTextures);
    BINDGLFN(glDeleteTextures);
    BINDGLFN(glStencilMask);
    BINDGLFN(glColorMask);
    BINDGLFN(glDepthMask);
    BINDGLFN(glScissor);
    BINDGLFN(glViewport);
    BINDGLFN(glClearColor);
    BINDGLFN(glClearDepthf);
    BINDGLFN(glDepthRangef);
    BINDGLFN(glPolygonOffset);
    BINDGLFN(glPointSize);
    BINDGLFN(glLineWidth);
    BINDGLFN(glClearStencil);
    BINDGLFN(glClear);
    BINDGLFN(glReadPixels);
    BINDGLFN(glBindTexture);
    BINDGLFN(glTexImage2D);
    BINDGLFN(glTexParameteri);
    BINDGLFN(glDrawArrays);
    BINDGLFN(glDrawElements);
    BINDGLFN(glVertexPointer);
    BINDGLFN(glColorPointer);
    BINDGLFN(glTexCoordPointer);
    BINDGLFN(glMatrixMode);
    BINDGLFN(glLoadMatrixf);
    BINDGLFN(glLoadIdentity);
    BINDGLFN(glAlphaFunc);
    BINDGLFN(glFrontFace);
    BINDGLFN(glCullFace);
    BINDGLFN(glDepthFunc);
    BINDGLFN(glStencilFunc);
    BINDGLFN(glStencilOp);
    BINDGLFN(glBlendFuncSeparate);
    BINDGLFN(glBlendEquationSeparate);
    BINDGLFN(glColor4f);
    BINDGLFN(glMultiTexCoord4f);
    BINDGLFN(glGetString);
    if (desktopExt) {
        BINDGLFN2(glGenFramebuffers, EXT);
        BINDGLFN2(glDeleteFramebuffers, EXT);
        BINDGLFN2(glGenRenderbuffers, EXT);
        BINDGLFN2(glDeleteRenderbuffers, EXT);
        BINDGLFN2(glRenderbufferStorage, EXT);
        BINDGLFN2(glBindFramebuffer, EXT);
        BINDGLFN2(glFramebufferRenderbuffer, EXT);
        BINDGLFN2(glFramebufferTexture2D, EXT);
        BINDGLFN2(glGenerateMipmap, EXT);
    } else {
        BINDGLFN2(glGenFramebuffers, OES);
        BINDGLFN2(glDeleteFramebuffers, OES);
        BINDGLFN2(glGenRenderbuffers, OES);
        BINDGLFN2(glDeleteRenderbuffers, OES);
        BINDGLFN2(glRenderbufferStorage, OES);
        BINDGLFN2(glBindFramebuffer, OES);
        BINDGLFN2(glFramebufferRenderbuffer, OES);
        BINDGLFN2(glFramebufferTexture2D, OES);
        BINDGLFN2(glGenerateMipmap, OES);
    }
    bi->glGenFramebuffers(1, &bi->fbo);
    bi->glBindFramebuffer(GL_FRAMEBUFFER, bi->fbo);
    if (!badgpuChk(bi, "badgpuNewInstance", 1)) {
        badgpuUnref((BADGPUInstance) bi);
        if (error)
            *error = "Initial GL resource setup returned an error.";
        return NULL;
    }
    return (BADGPUInstance) bi;
}

BADGPU_EXPORT const char * badgpuGetMetaInfo(BADGPUInstance instance,
    BADGPUMetaInfoType mi) {
    if (!instance)
        return NULL;
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    badgpu_wsiCtxMakeCurrent(bi->ctx);
    return bi->glGetString(mi);
}

// FBM

static inline BADGPUBool fbSetup(BADGPUTexture sTexture, BADGPUDSBuffer sDSBuffer, BADGPUInstancePriv ** bi) {
    BADGPUTexturePriv * sTex = BG_TEXTURE(sTexture);
    BADGPUDSBufferPriv * sDS = BG_DSBUFFER(sDSBuffer);
    if (sTex)
        *bi = sTex->i;
    if (sDS)
        *bi = sDS->i;
    // Sadly, can't report this if it happens
    if (!*bi)
        return 0;
    badgpu_wsiCtxMakeCurrent((*bi)->ctx);
    (*bi)->glBindFramebuffer(GL_FRAMEBUFFER, (*bi)->fbo);
    // badgpuChk(*bi, "fbSetup1", 0);
    if (sTex) {
        (*bi)->glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, sTex->tex, 0);
    } else {
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, 0);
    }
    // badgpuChk(*bi, "fbSetup2", 0);
    if (sDS) {
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, sDS->rbo);
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, sDS->rbo);
    } else {
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
        (*bi)->glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, 0);
    }
    return badgpuChk(*bi, "fbSetup", 1);
}

// Texture/2D Buffer Management

static void destroyTexture(BADGPUObject obj) {
    BADGPUTexturePriv * tex = BG_TEXTURE(obj);
    badgpu_wsiCtxMakeCurrent(tex->i->ctx);
    tex->i->glDeleteTextures(1, &tex->tex);
    badgpuChk(tex->i, "destroyTexture", 0);
    badgpuUnref((BADGPUObject) tex->i);
    free(tex);
}

BADGPU_EXPORT BADGPUTexture badgpuNewTexture(BADGPUInstance instance,
    uint32_t flags, BADGPUTextureFormat format,
    uint16_t width, uint16_t height, const uint8_t * data) {
    if (!instance)
        return NULL;

    // Continue.
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);

    // Do this conversion first, it's relevant to memory safety.
    int32_t ifmt = 0;
    int32_t efmt = 0;
    switch (format) {
    case BADGPUTextureFormat_Alpha: ifmt = GL_RGBA; efmt = GL_ALPHA; break;
    case BADGPUTextureFormat_Luma: ifmt = GL_RGB; efmt = GL_LUMINANCE; break;
    case BADGPUTextureFormat_LumaAlpha: ifmt = GL_RGBA; efmt = GL_LUMINANCE_ALPHA; break;
    case BADGPUTextureFormat_RGB: ifmt = GL_RGB; efmt = GL_RGB; break;
    case BADGPUTextureFormat_RGBA: ifmt = GL_RGBA; efmt = GL_RGBA; break;
    default:
        badgpuErr(bi, "badgpuNewTexture: Invalid BADGPUTextureFormat.");
        return NULL;
    }

    BADGPUTexturePriv * tex = malloc(sizeof(BADGPUTexturePriv));
    if (!tex) {
        badgpuErr(bi, "badgpuNewTexture: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroyTexture);

    badgpu_wsiCtxMakeCurrent(bi->ctx);

    tex->i = BG_INSTANCE(badgpuRef(instance));
    bi->glGenTextures(1, &tex->tex);

    bi->glBindTexture(GL_TEXTURE_2D, tex->tex);
    bi->glTexImage2D(GL_TEXTURE_2D, 0, ifmt, width, height, 0, efmt, GL_UNSIGNED_BYTE, data);

    if (!badgpuChk(bi, "badgpuNewTexture", 1)) {
        badgpuUnref((BADGPUTexture) tex);
        return NULL;
    }
    return (BADGPUTexture) tex;
}

static void destroyDSBuffer(BADGPUObject obj) {
    BADGPUDSBufferPriv * ds = BG_DSBUFFER(obj);
    badgpu_wsiCtxMakeCurrent(ds->i->ctx);
    ds->i->glDeleteRenderbuffers(1, &ds->rbo);
    badgpuChk(ds->i, "destroyDSBuffer", 0);
    badgpuUnref((BADGPUObject) ds->i);
    free(ds);
}

BADGPU_EXPORT BADGPUDSBuffer badgpuNewDSBuffer(BADGPUInstance instance,
    uint16_t width, uint16_t height) {
    if (!instance)
        return NULL;

    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    badgpu_wsiCtxMakeCurrent(bi->ctx);

    BADGPUDSBufferPriv * ds = malloc(sizeof(BADGPUDSBufferPriv));
    if (!ds) {
        badgpuErr(bi, "badgpuNewDSBuffer: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) ds, destroyDSBuffer);

    ds->i = BG_INSTANCE(badgpuRef(instance));
    bi->glGenRenderbuffers(1, &ds->rbo);

    bi->glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);

    if (!badgpuChk(bi, "badgpuNewDSBuffer", 1)) {
        badgpuUnref((BADGPUDSBuffer) ds);
        return NULL;
    }
    return (BADGPUDSBuffer) ds;
}

BADGPU_EXPORT BADGPUBool badgpuGenerateMipmap(BADGPUTexture texture) {
    if (!texture)
        return 0;
    BADGPUTexturePriv * tex = BG_TEXTURE(texture);
    badgpu_wsiCtxMakeCurrent(tex->i->ctx);
    tex->i->glBindTexture(GL_TEXTURE_2D, tex->tex);
    tex->i->glGenerateMipmap(GL_TEXTURE_2D);
    return badgpuChk(tex->i, "badgpuGenerateMipmap", 0);
}

BADGPU_EXPORT BADGPUBool badgpuReadPixels(BADGPUTexture texture,
    uint16_t x, uint16_t y, uint16_t width, uint16_t height, uint8_t * data) {
    BADGPUInstancePriv * bi;
    if (width == 0 || height == 0)
        return 1;
    if (!fbSetup(texture, NULL, &bi))
        return 0;
    if (!data)
        return badgpuErr(bi, "badgpuReadPixels: data == NULL for non-zero area");
    bi->glReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
    return badgpuChk(bi, "badgpuReadPixels", 0);
}

// Drawing Commands

static inline BADGPUBool drawingCmdSetup(
    BADGPU_SESSIONFLAGS,
    BADGPUInstancePriv ** bi
) {
    if (!fbSetup(sTexture, sDSBuffer, bi))
        return 0;
    (*bi)->glColorMask(
        sFlags & BADGPUSessionFlags_MaskR ? 1 : 0,
        sFlags & BADGPUSessionFlags_MaskG ? 1 : 0,
        sFlags & BADGPUSessionFlags_MaskB ? 1 : 0,
        sFlags & BADGPUSessionFlags_MaskA ? 1 : 0
    );
    // OPT: If we don't have a DSBuffer we don't need to setup the mask for it.
    if (sDSBuffer) {
        (*bi)->glStencilMask(sFlags & BADGPUSessionFlags_StencilAll);
        (*bi)->glDepthMask(sFlags & BADGPUSessionFlags_MaskDepth ? 1 : 0);
    }
    if (sFlags & BADGPUSessionFlags_Scissor) {
        (*bi)->glEnable(GL_SCISSOR_TEST);
        (*bi)->glScissor(sScX, sScY, sScWidth, sScHeight);
    } else {
        (*bi)->glDisable(GL_SCISSOR_TEST);
    }
    return 1;
}

BADGPU_EXPORT BADGPUBool badgpuDrawClear(
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
) {
    BADGPUInstancePriv * bi;
    if (!drawingCmdSetup(BADGPU_SESSIONFLAGS_PASSTHROUGH, &bi))
        return 0;
    int32_t cFlags = 0;
    if (sFlags & BADGPUSessionFlags_MaskRGBA) {
        bi->glClearColor(cR, cG, cB, cA);
        cFlags |= GL_COLOR_BUFFER_BIT;
    }
    // OPT: If we don't have a DSBuffer we don't need to setup the clear for it.
    if (sDSBuffer) {
        if (sFlags & BADGPUSessionFlags_MaskDepth) {
            bi->glClearDepthf(depth);
            cFlags |= GL_DEPTH_BUFFER_BIT;
        }
        if (sFlags & BADGPUSessionFlags_StencilAll) {
            bi->glClearStencil(stencil);
            cFlags |= GL_STENCIL_BUFFER_BIT;
        }
    }
    if (cFlags)
        bi->glClear(cFlags);
    return badgpuChk(bi, "badgpuDrawClear", 0);
}

BADGPU_EXPORT BADGPUBool badgpuDrawGeom(
    BADGPU_SESSIONFLAGS,
    uint32_t flags,
    // Vertex Loader
    const BADGPUVector * vPos, const BADGPUVector * vCol, const BADGPUVector * vTC,
    BADGPUPrimitiveType pType, float plSize,
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
    if (!drawingCmdSetup(BADGPU_SESSIONFLAGS_PASSTHROUGH, &bi))
        return 0;

    if (!vPos)
        return badgpuErr(bi, "badgpuDrawGeom: vPos is NULL");

    // Vertex Shader
    bi->glMatrixMode(GL_PROJECTION);
    if (!matrixA) bi->glLoadIdentity(); else bi->glLoadMatrixf((void *) matrixA);
    bi->glMatrixMode(GL_MODELVIEW);
    if (!matrixB) bi->glLoadIdentity(); else bi->glLoadMatrixf((void *) matrixB);

    // DepthRange/Viewport
    bi->glViewport(vX, vY, vW, vH);

    // Fragment Shader
    if (texture) {
        bi->glEnable(GL_TEXTURE_2D);
        bi->glBindTexture(GL_TEXTURE_2D, BG_TEXTURE(texture)->tex);
        bi->glMatrixMode(GL_TEXTURE);
        if (!matrixT) bi->glLoadIdentity(); else bi->glLoadMatrixf((void *) matrixT);

        bi->glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, flags & BADGPUDrawFlags_WrapS ? GL_REPEAT : GL_CLAMP_TO_EDGE);
        bi->glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, flags & BADGPUDrawFlags_WrapT ? GL_REPEAT : GL_CLAMP_TO_EDGE);

        int32_t minFilter = ((flags & BADGPUDrawFlags_Mipmap) ?
            ((flags & BADGPUDrawFlags_MinLinear) ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST_MIPMAP_NEAREST)
            :
            ((flags & BADGPUDrawFlags_MinLinear) ? GL_LINEAR : GL_NEAREST)
        );
        bi->glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
        bi->glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, flags & BADGPUDrawFlags_MagLinear ? GL_LINEAR : GL_NEAREST);
    } else {
        bi->glDisable(GL_TEXTURE_2D);
    }

    // Alpha Test
    bi->glEnable(GL_ALPHA_TEST);
    bi->glAlphaFunc(flags & BADGPUDrawFlags_AlphaTestInvert ? BADGPUCompare_Less : BADGPUCompare_GEqual, alphaTestMin);

    // OPT: Depth and stencil test are force-disabled by GL if we have no d/s.
    // (ES1.1 4.1.5 Stencil Test, 4.1.6 Depth Buffer Test, last paragraph of
    //  both)
    // That in mind, skip anything we dare to.
    if (sDSBuffer) {
        bi->glDepthRangef(depthN, depthF);

        // PolygonOffset
        bi->glEnable(GL_POLYGON_OFFSET_FILL);
        bi->glPolygonOffset(poFactor, poUnits);

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
    bi->glVertexPointer(4, GL_FLOAT, sizeof(BADGPUVector), &vPos->x);
    if (vCol) {
        if (flags & BADGPUDrawFlags_FreezeColour) {
            bi->glDisableClientState(GL_COLOR_ARRAY);
            bi->glColor4f(vCol->x, vCol->y, vCol->z, vCol->w);
        } else {
            bi->glEnableClientState(GL_COLOR_ARRAY);
            bi->glColorPointer(4, GL_FLOAT, sizeof(BADGPUVector), &vCol->x);
        }
    } else {
        bi->glDisableClientState(GL_COLOR_ARRAY);
        bi->glColor4f(1, 1, 1, 1);
    }
    if (vTC) {
        if (flags & BADGPUDrawFlags_FreezeTC) {
            bi->glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            bi->glMultiTexCoord4f(GL_TEXTURE0, vTC->x, vTC->y, vTC->z, vTC->w);
        } else {
            bi->glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            bi->glTexCoordPointer(4, GL_FLOAT, sizeof(BADGPUVector), &vTC->x);
        }
    } else {
        bi->glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        bi->glMultiTexCoord4f(GL_TEXTURE0, 0, 0, 0, 1);
    }

    if (indices) {
        bi->glDrawElements(pType, iCount, GL_UNSIGNED_SHORT, indices + iStart);
    } else {
        bi->glDrawArrays(pType, iStart, iCount);
    }
    return badgpuChk(bi, "badgpuDrawGeom", 0);
}

BADGPU_EXPORT BADGPUBool badgpuDrawGeomNoDS(
    BADGPUTexture sTexture,
    uint32_t sFlags,
    int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight,
    uint32_t flags,
    // Vertex Loader
    const BADGPUVector * vPos, const BADGPUVector * vCol, const BADGPUVector * vTC,
    BADGPUPrimitiveType pType, float plSize,
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
    return badgpuDrawGeom(
    sTexture, NULL,
    sFlags,
    sScX, sScY, sScWidth, sScHeight,
    flags,
    vPos, vCol, vTC,
    pType, plSize,
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

