/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu.h"
#include "badgpu_internal.h"

#define REMINDER_PINCH "\nWhile you should report this, it is worth noting that on desktop platforms, you may set the environment variable BADGPU_EGL_LIBRARY to the location of an ANGLE 'libGLESv2.so' library (from any Chromium-based application).\nThis may provide working service.\nDO NOT use a non-ANGLE libGLESv2.so file; this won't work."

// WSICTX
typedef struct BADGPUWSICtx {
    struct BADGPUWSIContext wsi;
    void * eglLibrary;
    void * glLibrary;
    void * dsp;
    void * ctx;
    void * srf;
    void * cfg;
    BADGPUContextType glContextType;
    int32_t (KHRABI *eglGetError)();
    void * (KHRABI *eglGetDisplay)(void *);
    void * (KHRABI *eglGetPlatformDisplay)(int32_t, void *, const intptr_t *);
    unsigned int (KHRABI *eglInitialize)(void *, int32_t *, int32_t *);
    unsigned int (KHRABI *eglBindAPI)(int32_t);
    unsigned int (KHRABI *eglChooseConfig)(void *, const int32_t *, void *, int32_t, int32_t *);
    void * (KHRABI *eglCreateContext)(void *, void *, void *, const int32_t *);
    void * (KHRABI *eglGetProcAddress)(const char *);
    unsigned int (KHRABI *eglMakeCurrent)(void *, void *, void *, void *);
    unsigned int (KHRABI *eglDestroyContext)(void *, void *);
    unsigned int (KHRABI *eglTerminate)(void *);
    void * (KHRABI *eglCreatePbufferSurface)(void *, void *, const int32_t *);
    unsigned int (KHRABI *eglDestroySurface)(void *, void *);
} * BADGPUWSICtx;

static BADGPUWSIContext badgpu_newWsiCtxError(const char ** error, const char * err) {
    if (error)
        *error = err;
    return 0;
}

static const char * locationsEGL[] = {
    "libEGL.so.1",
    "libEGL.so", // Android needs this
    "libEGL.dll",
    "libEGL",
    // if this ends up being reached, you're probably doomed
    // try it anyway; it's POSSIBLE it might establish ANGLE contact
    "libGLESv2.dll",
    "libGLESv2",
    NULL
};

static const char * locationsGLES1[] = {
    // these two should be expected for Android
    "libGLESv1_CM.so.1",
    "libGLESv1_CM.so",
    // shot in the dark
    "libGLESv1_CM.dll",
    "libGLESv1_CM",
    "libGLESv1_C.so.1",
    "libGLESv1_C.so",
    "libGLESv1_C.dll",
    "libGLESv1_C",
    "libGLESv1.dll",
    "libGLESv1",
    NULL
};

// the entire use of GL here is another shot in the dark
static const char * locationsGL[] = {
    "libGL.so.1",
    "libGL.so",
    "libGL",
    NULL
};

#define EGL_PLATFORM_SURFACELESS_MESA 0x31DD

// Yes, it's backwards.
#define EGL_HEIGHT 0x3056
#define EGL_WIDTH 0x3057

#define EGL_SURFACE_TYPE 0x3033

#define EGL_RENDERABLE_TYPE 0x3040
#define EGL_OPENGL_ES_BIT 0x0001
#define EGL_OPENGL_BIT 0x0008

#define EGL_NONE 0x3038

#define EGL_OPENGL_API 0x30A2
#define EGL_OPENGL_ES_API 0x30A0

static BADGPUBool badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx);
static void badgpu_wsiCtxStopCurrent(BADGPUWSICtx ctx);
static void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc);
static void * badgpu_wsiCtxGetValue(BADGPUWSICtx ctx, BADGPUWSIQuery query);
static void badgpu_destroyWsiCtx(BADGPUWSICtx ctx);

static BADGPUBool attemptEGL(BADGPUWSICtx ctx, int32_t ctxTypeAttrib, int32_t api, BADGPUContextType ctxTypeBG, const char * glLibraryName, BADGPUBool logDetailed, BADGPUBool tryNoWSI) {
    void * config;
    int32_t attribs[] = {
        EGL_RENDERABLE_TYPE, ctxTypeAttrib,
        EGL_NONE
    };
    int32_t attribsNoWSI[] = {
        EGL_RENDERABLE_TYPE, ctxTypeAttrib,
        EGL_SURFACE_TYPE, 0,
        EGL_NONE
    };
    int32_t configCount;
    const char * modeName = ctxTypeAttrib == EGL_OPENGL_ES_BIT ? "(for OpenGL ES)" : "(for desktop OpenGL)";
    const char * wsiClarifier = tryNoWSI ? " (even without allowing WSI)" : "";
    // So, fun fact I didn't know until WAAAYYYY later than I'd like:
    // This is how EGL decides which API to use!
    if (ctx->eglBindAPI) {
        if (!ctx->eglBindAPI(api)) {
            if (logDetailed)
                printf("BADGPU: eglBindAPI %s error: %i\n", modeName, ctx->eglGetError());
            return 0;
        }
    }
    if (!ctx->eglChooseConfig(ctx->dsp, tryNoWSI ? attribsNoWSI : attribs, &config, 1, &configCount)) {
        if (logDetailed)
            printf("BADGPU: eglChooseConfig %s%s error: %i\n", modeName, wsiClarifier, ctx->eglGetError());
        return 0;
    }
    if (!configCount) {
        if (logDetailed)
            printf("BADGPU: eglChooseConfig %s%s returned no configs!\n", modeName, wsiClarifier);
        return 0;
    }
    // Android 10 doesn't support surfaceless GLESv1 contexts.
    // This codepath is optional, as it's a compatibility workaround anyway.
    // We don't actually use this surface for anything except to keep Android happy.
    if (ctx->eglCreatePbufferSurface) {
        int32_t attribsS[] = {
            EGL_WIDTH, 1,
            EGL_HEIGHT, 1,
            EGL_NONE
        };
        ctx->srf = ctx->eglCreatePbufferSurface(ctx->dsp, config, attribsS);
        if (logDetailed)
            printf("BADGPU: eglCreatePbufferSurface exists; creating surface for Android 10 workaround\n");
    } else {
        if (logDetailed)
            printf("BADGPU: eglCreatePbufferSurface does not exist\n");
    }
    // If we don't manage to create the PBuffer, then march on regardless.
    // The system may still support surfaceless contexts.
    // Finally, make the context.
    int32_t attribsCtx[] = {
        EGL_NONE
    };
    ctx->ctx = ctx->eglCreateContext(ctx->dsp, config, NULL, attribsCtx);
    if (!ctx->ctx) {
        if (logDetailed)
            printf("BADGPU: eglCreateContext %s error: %i\n", modeName, ctx->eglGetError());
        if (ctx->srf) {
            ctx->eglDestroySurface(ctx->dsp, ctx->srf);
            ctx->srf = NULL;
        }
        return 0;
    }
    ctx->cfg = config;
    return 1;
}

static BADGPUBool attemptInitPrimaryDisplay(BADGPUWSICtx ctx, BADGPUBool logDetailed) {
    ctx->dsp = ctx->eglGetDisplay(NULL);
    if (!ctx->dsp) {
        if (logDetailed)
            printf("BADGPU: Default EGLDisplay: eglGetDisplay error: %i\n", ctx->eglGetError());
        return 0;
    }
    if (!ctx->eglInitialize(ctx->dsp, NULL, NULL)) {
        if (logDetailed)
            printf("BADGPU: Default EGLDisplay: eglInitialize error: %i\n", ctx->eglGetError());
        return 0;
    }
    printf("BADGPU: Default EGLDisplay: OK\n");
    return 1;
}

static BADGPUBool attemptInitSurfacelessDisplay(BADGPUWSICtx ctx, BADGPUBool logDetailed) {
    if (!ctx->eglGetPlatformDisplay) {
        if (logDetailed)
            printf("BADGPU: Surfaceless EGLDisplay: Don't have eglGetPlatformDisplay, can't attempt!\n");
        return 0;
    }
    // So Mesa can give you a Display that you can't actually initialize.
    // I've found you can get slightly further through init with surfaceless.
    // Can't hurt to try if we get here.
    // Test system is an Alpine Linux container; maybe should turn this into a reproducible thing?
    // See https://registry.khronos.org/EGL/extensions/MESA/EGL_MESA_platform_surfaceless.txt
    intptr_t attribsPlatformDisplay[] = {
        EGL_NONE
    };
    ctx->dsp = ctx->eglGetPlatformDisplay(EGL_PLATFORM_SURFACELESS_MESA, NULL, attribsPlatformDisplay);
    if (!ctx->dsp) {
        if (logDetailed)
            printf("BADGPU: Surfaceless EGLDisplay: eglGetPlatformDisplay error: %i\n", ctx->eglGetError());
        return 0;
    }
    if (!ctx->eglInitialize(ctx->dsp, NULL, NULL)) {
        if (logDetailed)
            printf("BADGPU: Surfaceless EGLDisplay: eglInitialize error: %i\n", ctx->eglGetError());
        return 0;
    }
    printf("BADGPU: Surfaceless EGLDisplay: OK\n");
    return 1;
}

BADGPUWSIContext badgpu_newWsiCtxEGL(const char ** error, BADGPUBool logDetailed) {
    BADGPUWSICtx ctx = malloc(sizeof(struct BADGPUWSICtx));
    if (!ctx)
        return badgpu_newWsiCtxError(error, "Could not allocate BADGPUWSICtx");
    memset(ctx, 0, sizeof(struct BADGPUWSICtx));

    ctx->wsi.makeCurrent = (void *) badgpu_wsiCtxMakeCurrent;
    ctx->wsi.stopCurrent = (void *) badgpu_wsiCtxStopCurrent;
    ctx->wsi.getProcAddress = (void *) badgpu_wsiCtxGetProcAddress;
    ctx->wsi.getValue = (void *) badgpu_wsiCtxGetValue;
    ctx->wsi.close = (void *) badgpu_destroyWsiCtx;

    // Can't guarantee a link to EGL, so we have to do it the *hard* way
    ctx->eglLibrary = badgpu_dlOpen(locationsEGL, "BADGPU_EGL_LIBRARY");
    if (!ctx->eglLibrary)
        return badgpu_newWsiCtxError(error, "Could not open EGL!" REMINDER_PINCH);
    // Under extreme circumstances, we need to be able to link to the ANGLE libGLES2 binary directly.
    // The symbol names are different but the ABI is completely identical.
    ctx->eglGetError = badgpu_dlSym2(ctx->eglLibrary, "eglGetError", "EGL_GetError");
    ctx->eglGetDisplay = badgpu_dlSym2(ctx->eglLibrary, "eglGetDisplay", "EGL_GetDisplay");
    ctx->eglGetPlatformDisplay = badgpu_dlSym4(ctx->eglLibrary, "eglGetPlatformDisplay", "EGL_GetPlatformDisplay", "eglGetPlatformDisplayEXT", "EGL_GetPlatformDisplayEXT");
    ctx->eglInitialize = badgpu_dlSym2(ctx->eglLibrary, "eglInitialize", "EGL_Initialize");
    ctx->eglBindAPI = badgpu_dlSym2(ctx->eglLibrary, "eglBindAPI", "EGL_BindAPI");
    ctx->eglChooseConfig = badgpu_dlSym2(ctx->eglLibrary, "eglChooseConfig", "EGL_ChooseConfig");
    ctx->eglCreateContext = badgpu_dlSym2(ctx->eglLibrary, "eglCreateContext", "EGL_CreateContext");
    ctx->eglGetProcAddress = badgpu_dlSym2(ctx->eglLibrary, "eglGetProcAddress", "EGL_GetProcAddress");
    ctx->eglMakeCurrent = badgpu_dlSym2(ctx->eglLibrary, "eglMakeCurrent", "EGL_MakeCurrent");
    ctx->eglDestroyContext = badgpu_dlSym2(ctx->eglLibrary, "eglDestroyContext", "EGL_DestroyContext");
    ctx->eglTerminate = badgpu_dlSym2(ctx->eglLibrary, "eglTerminate", "EGL_Terminate");
    ctx->eglCreatePbufferSurface = badgpu_dlSym2(ctx->eglLibrary, "eglCreatePbufferSurface", "EGL_CreatePbufferSurface");
    ctx->eglDestroySurface = badgpu_dlSym2(ctx->eglLibrary, "eglDestroySurface", "EGL_DestroySurface");
    // try initializing
    if (!attemptInitPrimaryDisplay(ctx, logDetailed))
        if (!attemptInitSurfacelessDisplay(ctx, logDetailed))
            return badgpu_newWsiCtxError(error, "Could not create / initialize EGLDisplay" REMINDER_PINCH);
    // alright, EGL initialized... can we use it?
    if (attemptEGL(ctx, EGL_OPENGL_ES_BIT, EGL_OPENGL_ES_API, BADGPUContextType_GLESv1, "BADGPU_GLES1_LIBRARY", logDetailed, 0))
        return (BADGPUWSIContext) ctx;
    if (attemptEGL(ctx, EGL_OPENGL_BIT, EGL_OPENGL_API, BADGPUContextType_GL, "BADGPU_GL_LIBRARY", logDetailed, 0))
        return (BADGPUWSIContext) ctx;
    // we're getting a little desperate, disable WSI features
    if (attemptEGL(ctx, EGL_OPENGL_ES_BIT, EGL_OPENGL_ES_API, BADGPUContextType_GLESv1, "BADGPU_GLES1_LIBRARY", logDetailed, 1))
        return (BADGPUWSIContext) ctx;
    if (attemptEGL(ctx, EGL_OPENGL_BIT, EGL_OPENGL_API, BADGPUContextType_GL, "BADGPU_GL_LIBRARY", logDetailed, 1))
        return (BADGPUWSIContext) ctx;
    return badgpu_newWsiCtxError(error, "Failed to setup either a GLESv1 config or a desktop GL config." REMINDER_PINCH);
}

BADGPUBool badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx) {
    return ctx->eglMakeCurrent(ctx->dsp, ctx->srf, ctx->srf, ctx->ctx) != 0;
}

void badgpu_wsiCtxStopCurrent(BADGPUWSICtx ctx) {
    ctx->eglMakeCurrent(ctx->dsp, NULL, NULL, NULL);
}

void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc) {
    void * main = ctx->eglGetProcAddress(proc);
    if (ctx->glLibrary && !main)
        return badgpu_dlSym(ctx->glLibrary, proc);
    return main;
}

void * badgpu_wsiCtxGetValue(BADGPUWSICtx ctx, BADGPUWSIQuery query) {
    if (query == BADGPUWSIQuery_WSIType)
        return (void *) BADGPUWSIType_EGL;
    if (query == BADGPUWSIQuery_LibGL)
        return ctx->glLibrary;
    if (query == BADGPUWSIQuery_ContextType)
        return (void *) ctx->glContextType;
    if (query == BADGPUWSIQuery_ContextWrapper)
        return (void *) ctx;

    if (query == BADGPUWSIQuery_EGLDisplay)
        return ctx->dsp;
    if (query == BADGPUWSIQuery_EGLContext)
        return ctx->ctx;
    if (query == BADGPUWSIQuery_EGLConfig)
        return ctx->cfg;
    if (query == BADGPUWSIQuery_EGLSurface)
        return ctx->srf;
    if (query == BADGPUWSIQuery_EGLLibEGL)
        return ctx->eglLibrary;

    return NULL;
}

void badgpu_destroyWsiCtx(BADGPUWSICtx ctx) {
    if (!ctx)
        return;
    if (ctx->ctx)
        ctx->eglDestroyContext(ctx->dsp, ctx->ctx);
    if (ctx->srf)
        ctx->eglDestroySurface(ctx->dsp, ctx->srf);
    if (ctx->dsp)
        ctx->eglTerminate(ctx->dsp);
    if (ctx->glLibrary)
        badgpu_dlClose(ctx->glLibrary);
    if (ctx->eglLibrary)
        badgpu_dlClose(ctx->eglLibrary);
    free(ctx);
}

