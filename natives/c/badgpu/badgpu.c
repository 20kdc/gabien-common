/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * BadGPU Reference Implementation
 */

#include "badgpu.h"
#include "badgpu_internal.h"
#include "badgpu_glbind.h"

// Types

struct BADGPUObject {
    size_t refs;
    void (*destroy)(BADGPUObject);
};

typedef struct BADGPUInstancePriv {
    struct BADGPUObject obj;
    BADGPUWSIContext ctx;
    int isBound;
    int backendCheck;
    int backendCheckAggressive;
    int canPrintf;
    uint32_t fbo;
    uint32_t fboBoundTex, fboBoundDS;
    // wsi stuff
    BADGPUGLBind gl;
} BADGPUInstancePriv;
#define BG_INSTANCE(x) ((BADGPUInstancePriv *) (x))

typedef struct BADGPUTexturePriv {
    struct BADGPUObject obj;
    BADGPUInstancePriv * i;
    uint32_t tex;
    int autoDel;
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

// Checks that the instance is bound.
static BADGPUBool badgpuBChk(BADGPUInstancePriv * bi, const char * location) {
    if (!bi->isBound) {
        if (bi->canPrintf)
            printf("BADGPU: %s: Instance not bound\n", location);
        return 0;
    }
    return 1;
}

static void destroyInstance(BADGPUObject obj) {
    BADGPUInstancePriv * bi = BG_INSTANCE(obj);
    if (badgpuBChk(bi, "destroyInstance")) {
        bi->gl.DeleteFramebuffers(1, &bi->fbo);
        bi->ctx->stopCurrent(bi->ctx);
        bi->ctx->close(bi->ctx);
        free(obj);
    }
}

static BADGPUBool badgpuChkInnards(BADGPUInstancePriv * bi, const char * location) {
    BADGPUBool ok = 1;
    while (1) {
        int err = bi->gl.GetError();
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

static KHRABI void badgpuDebugCB(int32_t a, int32_t b, int32_t c, int32_t d, int32_t len, const char * text, const void * g) {
    printf("BADGPU: GLDebug: %s\n", text);
}

BADGPU_EXPORT BADGPUInstance badgpuNewInstance(uint32_t flags, const char ** error) {
    BADGPUWSIContext wsi = badgpu_newWsiCtx(error, (flags & BADGPUNewInstanceFlags_CanPrintf) ? 1 : 0);
    if (!wsi) {
        // error provided by badgpu_newWsiCtx
        return NULL;
    }
    return badgpuNewInstanceWithWSI(flags, error, wsi);
}

BADGPU_EXPORT BADGPUInstance badgpuNewInstanceWithWSI(uint32_t flags, const char ** error, BADGPUWSIContext wsi) {
    BADGPUInstancePriv * bi = malloc(sizeof(BADGPUInstancePriv));
    if (!bi) {
        if (error)
            *error = "Failed to allocate BADGPUInstance.";
        wsi->close(wsi);
        return NULL;
    }
    memset(bi, 0, sizeof(BADGPUInstancePriv));
    bi->ctx = wsi;
    bi->backendCheck = (flags & BADGPUNewInstanceFlags_BackendCheck) != 0;
    bi->backendCheckAggressive = (flags & BADGPUNewInstanceFlags_BackendCheckAggressive) != 0;
    bi->canPrintf = (flags & BADGPUNewInstanceFlags_CanPrintf) != 0;
    badgpu_initObj((BADGPUObject) bi, destroyInstance);
    // determine context type stuff
    int desktopExt = 0;
    switch ((BADGPUContextType) (int) (intptr_t) wsi->getValue(wsi, BADGPUWSIQuery_ContextType)) {
    case BADGPUContextType_GLESv1:
        break;
    case BADGPUContextType_GL:
        desktopExt = 1;
        break;
    default:
        if (error)
            *error = "BadGPU does not support the given context type";
        wsi->close(wsi);
        free(bi);
        return NULL;
    }
    // Initial bind
    if (!wsi->makeCurrent(wsi)) {
        if (error)
            *error = "Failed to initially bind instance";
        wsi->close(wsi);
        free(bi);
        return NULL;
    }
    bi->isBound = 1;
    const char * failedFn = badgpu_glBind(wsi, &bi->gl, desktopExt);
    if (failedFn) {
        wsi->close(wsi);
        if (error)
            *error = failedFn;
        free(bi);
        return NULL;
    }

    const char * ext = bi->gl.GetString(GL_EXTENSIONS);
    if (bi->canPrintf) {
        if (ext) {
            printf("BADGPU: GL Extensions: %s\n", ext);
        } else {
            printf("BADGPU: GL Extensions not available!\n");
        }
    }
    if (bi->backendCheck && bi->canPrintf && ext) {
        const char * exCheck = strstr(ext, "GL_KHR_debug");
        if (exCheck && ((exCheck[12] == 0) || (exCheck[12] == ' '))) {
            printf("BADGPU: KHR_debug detected, testing...\n");
            bi->gl.Enable(GL_DEBUG_OUTPUT);
            void (KHRABI *glDebugMessageControl)(int32_t, int32_t, int32_t, int32_t, const int32_t *, int32_t) = wsi->getProcAddress(wsi, "glDebugMessageControl");
            if (glDebugMessageControl)
                glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, NULL, 1);
            void (KHRABI *glDebugMessageCallback)(void *, const void *) = wsi->getProcAddress(wsi, "glDebugMessageCallback");
            if (glDebugMessageCallback)
                glDebugMessageCallback(badgpuDebugCB, NULL);
            void (KHRABI *glDebugMessageInsert)(int32_t, int32_t, int32_t, int32_t, int32_t, const char *) = wsi->getProcAddress(wsi, "glDebugMessageInsert");
            if (glDebugMessageInsert)
                glDebugMessageInsert(GL_DEBUG_SOURCE_THIRD_PARTY, GL_DEBUG_TYPE_OTHER, 0, GL_DEBUG_SEVERITY_NOTIFICATION, -1, "BADGPU GL Debug Test Message");
        }
    }
    bi->gl.GenFramebuffers(1, &bi->fbo);
    bi->gl.BindFramebuffer(GL_FRAMEBUFFER, bi->fbo);
    // Not yet setup, so fboBoundTex/fboBoundDS being 0 is correct
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
    if (!badgpuBChk(bi, "badgpuGetMetaInfo"))
        return NULL;
    return bi->gl.GetString(mi);
}

BADGPU_EXPORT BADGPUBool badgpuBindInstance(BADGPUInstance instance) {
    if (!instance)
        return 0;
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (bi->isBound) {
        if (bi->canPrintf)
            printf("BADGPU: badgpuBindInstance: already bound somewhere\n");
        return 1;
    }
    if (!bi->ctx->makeCurrent(bi->ctx)) {
        if (bi->canPrintf)
            printf("BADGPU: badgpuBindInstance: failed to bind\n");
        return 1;
    }
    bi->isBound = 1;
    return 0;
}

BADGPU_EXPORT void badgpuUnbindInstance(BADGPUInstance instance) {
    if (!instance)
        return;
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (!badgpuBChk(bi, "badgpuUnbindInstance"))
        return;
    bi->ctx->stopCurrent(bi->ctx);
    bi->isBound = 0;
}

BADGPU_EXPORT void badgpuFlushInstance(BADGPUInstance instance) {
    if (!instance)
        return;
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (!badgpuBChk(bi, "badgpuFlushInstance"))
        return;
    bi->gl.Flush();
}

BADGPU_EXPORT void badgpuFinishInstance(BADGPUInstance instance) {
    if (!instance)
        return;
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (!badgpuBChk(bi, "badgpuFinishInstance"))
        return;
    bi->gl.Finish();
}

// FBM

static inline BADGPUBool fbSetup(BADGPUTexture sTexture, BADGPUDSBuffer sDSBuffer, BADGPUInstancePriv ** biR) {
    BADGPUTexturePriv * sTex = BG_TEXTURE(sTexture);
    BADGPUDSBufferPriv * sDS = BG_DSBUFFER(sDSBuffer);
    BADGPUInstancePriv * bi = NULL;
    if (sTex)
        bi = sTex->i;
    if (sDS)
        bi = sDS->i;
    *biR = bi;
    // Sadly, can't report this if it happens
    if (!bi)
        return 0;
    if (!badgpuBChk(bi, "fbSetup"))
        return 0;
    bi->gl.BindFramebuffer(GL_FRAMEBUFFER, bi->fbo);
    // badgpuChk(*bi, "fbSetup1", 0);
    // OPT:
    //  JUST TO BE CLEAR.
    //  JUST TO BE ABSOLUTELY CLEAR.
    //  PERFORMING NO-OP FBO ATTACHMENT REBINDS; YES, EVEN ONES THAT JUST REBIND AN ATTACHMENT TO EXACTLY WHAT IT WAS;
    //  WILL CAUSE THE ARM MALI DRIVERS TO RELOCATE YOUR SKELETON OUTSIDE OF YOUR BODY.
    //  HARDWARE DETAILS:
    //   ARM
    //   Mali-T830
    //   OpenGL ES-CM 1.1 v1.r20p0-01rel0.9a7fca3 f7dd712a473937294a8ae24b1
    if (sTex && (bi->fboBoundTex != sTex->tex)) {
        bi->gl.FramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, sTex->tex, 0);
        bi->fboBoundTex = sTex->tex;
    } else if ((!sTex) && (bi->fboBoundTex != 0)) {
        bi->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, 0);
        bi->fboBoundTex = 0;
    }
    // badgpuChk(*bi, "fbSetup2", 0);
    uint32_t newRBO = sDS ? sDS->rbo : 0;
    if (bi->fboBoundDS != newRBO) {
        bi->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, newRBO);
        bi->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, newRBO);
        bi->fboBoundDS = newRBO;
    }
    return badgpuChk(bi, "fbSetup", 1);
}

// Texture/2D Buffer Management

static void destroyTexture(BADGPUObject obj) {
    BADGPUTexturePriv * tex = BG_TEXTURE(obj);
    if (!badgpuBChk(tex->i, "destroyTexture"))
        return;
    // make SURE it's not bound to our FBO
    tex->i->gl.BindFramebuffer(GL_FRAMEBUFFER, tex->i->fbo);
    tex->i->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, 0);
    tex->i->fboBoundTex = 0;
    // continue
    if (tex->autoDel) {
        tex->i->gl.DeleteTextures(1, &tex->tex);
        badgpuChk(tex->i, "destroyTexture", 0);
    }
    badgpuUnref((BADGPUObject) tex->i);
    free(tex);
}

BADGPU_EXPORT BADGPUTexture badgpuNewTexture(BADGPUInstance instance,
    int16_t width, int16_t height, BADGPUTextureLoadFormat fmt,
    const void * data) {
    if (!instance)
        return NULL;

    // Continue.
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);

    if (!badgpuBChk(bi, "badgpuNewTexture"))
        return NULL;

    if (width <= 0 || height <= 0) {
        badgpuErr(bi, "badgpuNewTexture: Width or height <= 0.");
        return NULL;
    }

    void * tmpBuf = 0;

    if (data && fmt != BADGPUTextureLoadFormat_RGBA8888) {
        // Setup conversion buffer and convert
        uint32_t sz = badgpuPixelsSize(BADGPUTextureLoadFormat_RGBA8888, width, height);
        if (!sz) {
            badgpuErr(bi, "badgpuNewTexture: Invalid format.");
            return 0;
        }
        tmpBuf = malloc(sz);
        if (!tmpBuf) {
            badgpuErr(bi, "badgpuNewTexture: Unable to allocate CVB.");
            return NULL;
        }
        badgpuPixelsConvert(fmt, BADGPUTextureLoadFormat_RGBA8888, width, height, data, tmpBuf);
        // swap over data pointer
        data = tmpBuf;
    }

    BADGPUTexturePriv * tex = malloc(sizeof(BADGPUTexturePriv));
    if (!tex) {
        free(tmpBuf);
        badgpuErr(bi, "badgpuNewTexture: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroyTexture);

    tex->i = BG_INSTANCE(badgpuRef(instance));
    tex->autoDel = 1;
    bi->gl.GenTextures(1, &tex->tex);

    bi->gl.BindTexture(GL_TEXTURE_2D, tex->tex);
    bi->gl.TexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
    if (tmpBuf)
        free(tmpBuf);

    if (!badgpuChk(bi, "badgpuNewTexture", 1)) {
        badgpuUnref((BADGPUTexture) tex);
        return NULL;
    }
    return (BADGPUTexture) tex;
}

static void destroyDSBuffer(BADGPUObject obj) {
    BADGPUDSBufferPriv * ds = BG_DSBUFFER(obj);
    if (!badgpuBChk(ds->i, "destroyDSBuffer"))
        return;
    // make SURE it's not bound to our FBO
    ds->i->gl.BindFramebuffer(GL_FRAMEBUFFER, ds->i->fbo);
    ds->i->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
    ds->i->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, 0);
    ds->i->fboBoundDS = 0;
    // continue
    ds->i->gl.DeleteRenderbuffers(1, &ds->rbo);
    badgpuChk(ds->i, "destroyDSBuffer", 0);
    badgpuUnref((BADGPUObject) ds->i);
    free(ds);
}

BADGPU_EXPORT BADGPUDSBuffer badgpuNewDSBuffer(BADGPUInstance instance,
    int16_t width, int16_t height) {
    if (!instance)
        return NULL;

    BADGPUInstancePriv * bi = BG_INSTANCE(instance);

    if (width <= 0 || height <= 0) {
        badgpuErr(bi, "badgpuNewDSBuffer: Width or height <= 0.");
        return NULL;
    }

    if (!badgpuBChk(bi, "badgpuNewDSBuffer"))
        return NULL;

    BADGPUDSBufferPriv * ds = malloc(sizeof(BADGPUDSBufferPriv));
    if (!ds) {
        badgpuErr(bi, "badgpuNewDSBuffer: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) ds, destroyDSBuffer);

    ds->i = BG_INSTANCE(badgpuRef(instance));
    bi->gl.GenRenderbuffers(1, &ds->rbo);
    bi->gl.BindRenderbuffer(GL_RENDERBUFFER, ds->rbo);
    bi->gl.RenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);

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
    if (!badgpuBChk(tex->i, "badgpuGenerateMipmap"))
        return 0;
    tex->i->gl.BindTexture(GL_TEXTURE_2D, tex->tex);
    tex->i->gl.GenerateMipmap(GL_TEXTURE_2D);
    return badgpuChk(tex->i, "badgpuGenerateMipmap", 0);
}

BADGPU_EXPORT BADGPUBool badgpuReadPixels(BADGPUTexture texture,
    uint16_t x, uint16_t y, int16_t width, int16_t height,
    BADGPUTextureLoadFormat fmt, void * data) {
    BADGPUInstancePriv * bi;
    if (width == 0 || height == 0)
        return 1;
    if (!fbSetup(texture, NULL, &bi))
        return 0;
    if (width < 0 || height < 0)
        return badgpuErr(bi, "badgpuReadPixels: Width or height < 0.");
    if (!data)
        return badgpuErr(bi, "badgpuReadPixels: data == NULL for non-zero area");

    if (fmt == BADGPUTextureLoadFormat_RGBA8888) {
        bi->gl.ReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
    } else if (fmt == BADGPUTextureLoadFormat_ARGBI32) {
        // special fast-path because profiling said so
        bi->gl.ReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
        badgpuPixelsConvertRGBA8888ToARGBI32InPlace(width, height, data);
    } else {
        uint32_t sz = badgpuPixelsSize(BADGPUTextureLoadFormat_RGBA8888, width, height);
        if (!sz)
            return badgpuErr(bi, "badgpuReadPixels: Invalid format.");
        void * tmpBuf = malloc(sz);
        if (!tmpBuf)
            return badgpuErr(bi, "badgpuReadPixels: Unable to allocate conversion buffer.");
        bi->gl.ReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, tmpBuf);
        badgpuPixelsConvert(BADGPUTextureLoadFormat_RGBA8888, fmt, width, height, tmpBuf, data);
        free(tmpBuf);
    }
    return badgpuChk(bi, "badgpuReadPixels", 0);
}

// Drawing Commands

static inline BADGPUBool drawingCmdSetup(
    BADGPU_SESSIONFLAGS,
    BADGPUInstancePriv ** bi
) {
    if (!fbSetup(sTexture, sDSBuffer, bi))
        return 0;
    (*bi)->gl.ColorMask(
        (sFlags & BADGPUSessionFlags_MaskR) ? 1 : 0,
        (sFlags & BADGPUSessionFlags_MaskG) ? 1 : 0,
        (sFlags & BADGPUSessionFlags_MaskB) ? 1 : 0,
        (sFlags & BADGPUSessionFlags_MaskA) ? 1 : 0
    );
    // OPT: If we don't have a DSBuffer we don't need to setup the mask for it.
    if (sDSBuffer) {
        // StencilAll is deliberately at bottom of flags for this
        (*bi)->gl.StencilMask(sFlags & BADGPUSessionFlags_StencilAll);
        (*bi)->gl.DepthMask((sFlags & BADGPUSessionFlags_MaskDepth) ? 1 : 0);
    }
    if (sFlags & BADGPUSessionFlags_Scissor) {
        (*bi)->gl.Enable(GL_SCISSOR_TEST);
        (*bi)->gl.Scissor(sScX, sScY, sScWidth, sScHeight);
    } else {
        (*bi)->gl.Disable(GL_SCISSOR_TEST);
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
        bi->gl.ClearColor(cR, cG, cB, cA);
        cFlags |= GL_COLOR_BUFFER_BIT;
    }
    // OPT: If we don't have a DSBuffer we don't need to setup the clear for it.
    if (sDSBuffer) {
        if (sFlags & BADGPUSessionFlags_MaskDepth) {
            bi->gl.ClearDepthf(depth);
            cFlags |= GL_DEPTH_BUFFER_BIT;
        }
        if (sFlags & BADGPUSessionFlags_StencilAll) {
            bi->gl.ClearStencil(stencil);
            cFlags |= GL_STENCIL_BUFFER_BIT;
        }
    }
    if (cFlags)
        bi->gl.Clear(cFlags);
    return badgpuChk(bi, "badgpuDrawClear", 0);
}

static int32_t convertBlendWeight(int32_t bw) {
    switch (bw) {
    // GL_ZERO
    case BADGPUBlendWeight_Zero: return 0;
    // GL_ONE
    case BADGPUBlendWeight_One: return 1;
    // GL_SRC_ALPHA_SATURATE
    case BADGPUBlendWeight_SrcAlphaSaturate: return 0x308;
    // GL_DST_COLOR
    case BADGPUBlendWeight_Dst: return 0x306;
    // GL_ONE_MINUS_DST_COLOR
    case BADGPUBlendWeight_InvertDst: return 0x307;
    // GL_DST_ALPHA
    case BADGPUBlendWeight_DstA: return 0x304;
    // GL_ONE_MINUS_DST_ALPHA
    case BADGPUBlendWeight_InvertDstA: return 0x305;
    // GL_SRC_COLOR
    case BADGPUBlendWeight_Src: return 0x300;
    // GL_ONE_MINUS_SRC_COLOR
    case BADGPUBlendWeight_InvertSrc: return 0x301;
    // GL_SRC_ALPHA
    case BADGPUBlendWeight_SrcA: return 0x0302;
    // GL_ONE_MINUS_SRC_ALPHA
    case BADGPUBlendWeight_InvertSrcA: return 0x303;
    default: return 0;
    }
}

static int32_t convertBlendOp(int32_t be) {
    switch (be) {
    // GL_FUNC_ADD
    case BADGPUBlendOp_Add: return 0x8006;
    // GL_FUNC_SUBTRACT
    case BADGPUBlendOp_Sub: return 0x800A;
    // GL_FUNC_REVERSE_SUBTRACT
    case BADGPUBlendOp_ReverseSub: return 0x800B;
    default: return 0;
    }
}

BADGPU_EXPORT BADGPUBool badgpuDrawGeom(
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
    BADGPUInstancePriv * bi;
    if (!drawingCmdSetup(BADGPU_SESSIONFLAGS_PASSTHROUGH, &bi))
        return 0;

    if (!vPos)
        return badgpuErr(bi, "badgpuDrawGeom: vPos is NULL");

    if ((iCount < 0) || (iCount > 65536))
        return badgpuErr(bi, "badgpuDrawGeom: iCount out of range");

    if ((vPosD < 2) || (vPosD > 4))
        return badgpuErr(bi, "badgpuDrawGeom: vPosD out of range");

    if ((vTCD < 2) || (vTCD > 4))
        return badgpuErr(bi, "badgpuDrawGeom: vTCD out of range");

    // Vertex Shader
    bi->gl.MatrixMode(GL_MODELVIEW);
    if (!mvMatrix) bi->gl.LoadIdentity(); else bi->gl.LoadMatrixf((void *) mvMatrix);

    // DepthRange/Viewport
    bi->gl.Viewport(vX, vY, vW, vH);

    // Fragment Shader
    if (texture) {
        bi->gl.Enable(GL_TEXTURE_2D);
        bi->gl.BindTexture(GL_TEXTURE_2D, BG_TEXTURE(texture)->tex);
        bi->gl.MatrixMode(GL_TEXTURE);
        if (!matrixT) bi->gl.LoadIdentity(); else bi->gl.LoadMatrixf((void *) matrixT);

        bi->gl.TexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, (flags & BADGPUDrawFlags_WrapS) ? GL_REPEAT : GL_CLAMP_TO_EDGE);
        bi->gl.TexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, (flags & BADGPUDrawFlags_WrapT) ? GL_REPEAT : GL_CLAMP_TO_EDGE);

        int32_t minFilter = ((flags & BADGPUDrawFlags_Mipmap) ?
            ((flags & BADGPUDrawFlags_MinLinear) ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST_MIPMAP_NEAREST)
            :
            ((flags & BADGPUDrawFlags_MinLinear) ? GL_LINEAR : GL_NEAREST)
        );
        bi->gl.TexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
        bi->gl.TexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, (flags & BADGPUDrawFlags_MagLinear) ? GL_LINEAR : GL_NEAREST);
    } else {
        bi->gl.Disable(GL_TEXTURE_2D);
    }

    if (clipPlane) {
        bi->gl.Enable(GL_CLIP_PLANE0);
        if (bi->gl.ClipPlanef) {
            bi->gl.ClipPlanef(GL_CLIP_PLANE0, clipPlane);
        } else {
            double tmp[4];
            tmp[0] = clipPlane[0]; tmp[1] = clipPlane[1]; tmp[2] = clipPlane[2]; tmp[3] = clipPlane[3];
            bi->gl.ClipPlane(GL_CLIP_PLANE0, tmp);
        }
    } else {
        bi->gl.Disable(GL_CLIP_PLANE0);
    }

    // Alpha Test
    if (atFunc != BADGPUCompare_Always) {
        bi->gl.Enable(GL_ALPHA_TEST);
        // no conversion as values deliberately match
        bi->gl.AlphaFunc(atFunc, atRef);
    } else {
        bi->gl.Disable(GL_ALPHA_TEST);
    }

    // OPT: Depth and stencil test are force-disabled by GL if we have no d/s.
    // (ES1.1 4.1.5 Stencil Test, 4.1.6 Depth Buffer Test, last paragraph of
    //  both)
    // That in mind, skip anything we dare to.
    if (sDSBuffer) {
        bi->gl.DepthRangef(depthN, depthF);

        // PolygonOffset
        bi->gl.Enable(GL_POLYGON_OFFSET_FILL);
        bi->gl.PolygonOffset(poFactor, poUnits);

        // Stencil Test
        if (flags & BADGPUDrawFlags_StencilTest) {
            bi->gl.Enable(GL_STENCIL_TEST);
            // no conversion as values deliberately match
            bi->gl.StencilFunc(stFunc, stRef, stMask);
            bi->gl.StencilOp(stSF, stDF, stDP);
        } else {
            bi->gl.Disable(GL_STENCIL_TEST);
        }

        // Depth Test
        if (dtFunc != BADGPUCompare_Always) {
            bi->gl.Enable(GL_DEPTH_TEST);
            // no conversion as values deliberately match
            bi->gl.DepthFunc(dtFunc);
        } else {
            bi->gl.Disable(GL_DEPTH_TEST);
        }
    }

    // Misc. Flags Stuff
    bi->gl.FrontFace((flags & BADGPUDrawFlags_FrontFaceCW) ? GL_CW : GL_CCW);

    if (flags & BADGPUDrawFlags_CullFace) {
        bi->gl.Enable(GL_CULL_FACE);
        bi->gl.CullFace((flags & BADGPUDrawFlags_CullFaceFront) ? GL_FRONT : GL_BACK);
    } else {
        bi->gl.Disable(GL_CULL_FACE);
    }

    // Blending
    if (flags & BADGPUDrawFlags_Blend) {
        bi->gl.Enable(GL_BLEND);
        bi->gl.BlendFuncSeparate(
            convertBlendWeight((blendProgram >> 24) & 077),  // RGB S
            convertBlendWeight((blendProgram >> 18) & 077),  // RGB D
            convertBlendWeight((blendProgram >>  9) & 077),  // A   S
            convertBlendWeight((blendProgram >>  3) & 077)); // A   D
        bi->gl.BlendEquationSeparate(
            convertBlendOp((blendProgram >> 15) & 07),
            convertBlendOp(blendProgram & 07));
    } else {
        bi->gl.Disable(GL_BLEND);
    }

    // Vertex Loader
    if (pType == BADGPUPrimitiveType_Points) {
        bi->gl.PointSize(plSize);
    } else if (pType == BADGPUPrimitiveType_Lines) {
        bi->gl.LineWidth(plSize);
    }

    bi->gl.EnableClientState(GL_VERTEX_ARRAY);
    bi->gl.VertexPointer(vPosD, GL_FLOAT, 0, vPos);
    if (vCol) {
        if (flags & BADGPUDrawFlags_FreezeColour) {
            bi->gl.DisableClientState(GL_COLOR_ARRAY);
            bi->gl.Color4f(vCol[0], vCol[1], vCol[2], vCol[3]);
        } else {
            bi->gl.EnableClientState(GL_COLOR_ARRAY);
            bi->gl.ColorPointer(4, GL_FLOAT, 0, vCol);
        }
    } else {
        bi->gl.DisableClientState(GL_COLOR_ARRAY);
        bi->gl.Color4f(1, 1, 1, 1);
    }
    if (vTC) {
        if (flags & BADGPUDrawFlags_FreezeTC) {
            bi->gl.DisableClientState(GL_TEXTURE_COORD_ARRAY);
            if (vTCD == 4) {
                bi->gl.MultiTexCoord4f(GL_TEXTURE0, vTC[0], vTC[1], vTC[2], vTC[3]);
            } else if (vTCD == 3) {
                bi->gl.MultiTexCoord4f(GL_TEXTURE0, vTC[0], vTC[1], vTC[2], 1.0f);
            } else {
                bi->gl.MultiTexCoord4f(GL_TEXTURE0, vTC[0], vTC[1], 0.0f, 1.0f);
            }
        } else {
            bi->gl.EnableClientState(GL_TEXTURE_COORD_ARRAY);
            bi->gl.TexCoordPointer(vTCD, GL_FLOAT, 0, vTC);
        }
    } else {
        bi->gl.DisableClientState(GL_TEXTURE_COORD_ARRAY);
        bi->gl.MultiTexCoord4f(GL_TEXTURE0, 0, 0, 0, 1);
    }

    // Actual Draw
    if (indices) {
        bi->gl.DrawElements(pType, iCount, GL_UNSIGNED_SHORT, indices + iStart);
    } else {
        bi->gl.DrawArrays(pType, iStart, iCount);
    }
    return badgpuChk(bi, "badgpuDrawGeom", 0);
}

BADGPU_EXPORT BADGPUBool badgpuDrawGeomNoDS(
    BADGPUTexture sTexture,
    uint32_t sFlags,
    int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight,
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
    // Blending
    uint32_t blendProgram
) {
    return badgpuDrawGeom(
    sTexture, NULL,
    sFlags,
    sScX, sScY, sScWidth, sScHeight,
    flags,
    vPosD, vPos, vCol, vTCD, vTC,
    pType, plSize,
    iStart, iCount, indices,
    mvMatrix,
    vX, vY, vW, vH,
    texture, matrixT,
    clipPlane, atFunc, atRef,
    BADGPUCompare_Always, 0, 0,
    BADGPUStencilOp_Keep, BADGPUStencilOp_Keep, BADGPUStencilOp_Keep,
    BADGPUCompare_Always, 0, 0, 0, 0,
    blendProgram
    );
}

BADGPU_EXPORT BADGPUBool badgpuResetGLState(BADGPUInstance instance) {
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);

    if (!badgpuBChk(bi, "badgpuResetGLState"))
        return 0;

    bi->gl.BindFramebuffer(GL_FRAMEBUFFER, 0);
    bi->gl.ColorMask(1, 1, 1, 1);
    bi->gl.StencilMask(-1);
    bi->gl.DepthMask(1);
    bi->gl.Disable(GL_SCISSOR_TEST);

    bi->gl.MatrixMode(GL_PROJECTION);
    bi->gl.LoadIdentity();
    bi->gl.MatrixMode(GL_TEXTURE);
    bi->gl.LoadIdentity();
    bi->gl.MatrixMode(GL_MODELVIEW);
    bi->gl.LoadIdentity();

    bi->gl.Disable(GL_TEXTURE_2D);
    bi->gl.BindTexture(GL_TEXTURE_2D, 0);

    bi->gl.Disable(GL_CLIP_PLANE0);
    bi->gl.Disable(GL_ALPHA_TEST);

    bi->gl.DepthRangef(0, 1);

    bi->gl.Disable(GL_POLYGON_OFFSET_FILL);
    bi->gl.Disable(GL_STENCIL_TEST);
    bi->gl.Disable(GL_DEPTH_TEST);

    bi->gl.FrontFace(GL_CCW);
    bi->gl.Disable(GL_CULL_FACE);

    bi->gl.Disable(GL_BLEND);

    bi->gl.DisableClientState(GL_VERTEX_ARRAY);
    bi->gl.DisableClientState(GL_COLOR_ARRAY);
    bi->gl.Color4f(1, 1, 1, 1);
    bi->gl.DisableClientState(GL_TEXTURE_COORD_ARRAY);
    bi->gl.MultiTexCoord4f(GL_TEXTURE0, 0, 0, 0, 1);

    return badgpuChk(bi, "badgpuResetGLState", 0);
}

BADGPU_EXPORT void * badgpuGetWSIValue(BADGPUInstance instance, BADGPUWSIQuery query) {
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    return bi->ctx->getValue(bi->ctx, query);
}

BADGPU_EXPORT uint32_t badgpuGetGLTexture(BADGPUTexture texture) {
    BADGPUTexturePriv * sTex = BG_TEXTURE(texture);
    return sTex->tex;
}

BADGPU_EXPORT BADGPUTexture badgpuNewTextureFromGL(BADGPUInstance instance, uint32_t glTex) {
    if (!instance)
        return NULL;

    // Continue.
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);

    if (!badgpuBChk(bi, "badgpuNewTextureFromGL"))
        return NULL;

    BADGPUTexturePriv * tex = malloc(sizeof(BADGPUTexturePriv));
    if (!tex) {
        badgpuErr(bi, "badgpuNewTextureFromGL: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroyTexture);

    tex->i = BG_INSTANCE(badgpuRef(instance));
    tex->autoDel = 0;
    tex->tex = glTex;
    return (BADGPUTexture) tex;
}


