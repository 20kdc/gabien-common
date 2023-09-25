/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu_internal.h"

// WSICTX
typedef struct BADGPUWSICtx {
    struct BADGPUWSIContext wsi;
    void * eglLibrary;
    void * glLibrary;
    void * dsp;
    void * ctx;
    void * srf;
    void * cfg;
    void * (KHRABI *eglGetDisplay)(void *);
    unsigned int (KHRABI *eglInitialize)(void *, int32_t *, int32_t *);
    unsigned int (KHRABI *eglChooseConfig)(void *, int32_t *, void *, int32_t, int32_t *);
    void * (KHRABI *eglCreateContext)(void *, void *, void *, int32_t *);
    void * (KHRABI *eglGetProcAddress)(const char *);
    unsigned int (KHRABI *eglMakeCurrent)(void *, void *, void *, void *);
    unsigned int (KHRABI *eglDestroyContext)(void *, void *);
    unsigned int (KHRABI *eglTerminate)(void *);
    void * (KHRABI *eglCreatePbufferSurface)(void *, void *, int32_t *);
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

// Yes, it's backwards.
#define EGL_HEIGHT 0x3056
#define EGL_WIDTH 0x3057

#define EGL_RENDERABLE_TYPE 0x3040
#define EGL_OPENGL_ES_BIT 0x0001
#define EGL_NONE 0x3038

static BADGPUBool badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx);
static void badgpu_wsiCtxStopCurrent(BADGPUWSICtx ctx);
static void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc);
static void * badgpu_wsiCtxGetValue(BADGPUWSICtx ctx, BADGPUWSIQuery query);
static void badgpu_destroyWsiCtx(BADGPUWSICtx ctx);

BADGPUWSIContext badgpu_newWsiCtx(const char ** error) {
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
        return badgpu_newWsiCtxError(error, "Could not open EGL");
    // Try this. If it fails it fails; don't worry about it too much.
    // Older Android versions require you do this to get core symbols.
    ctx->glLibrary = badgpu_dlOpen(locationsGLES1, "BADGPU_GLES1_LIBRARY");
    // Under extreme circumstances, we need to be able to link to the ANGLE libGLES2 binary directly.
    // The symbol names are different but the ABI is completely identical.
    ctx->eglGetDisplay = badgpu_dlSym2(ctx->eglLibrary, "eglGetDisplay", "EGL_GetDisplay");
    ctx->eglInitialize = badgpu_dlSym2(ctx->eglLibrary, "eglInitialize", "EGL_Initialize");
    ctx->eglChooseConfig = badgpu_dlSym2(ctx->eglLibrary, "eglChooseConfig", "EGL_ChooseConfig");
    ctx->eglCreateContext = badgpu_dlSym2(ctx->eglLibrary, "eglCreateContext", "EGL_CreateContext");
    ctx->eglGetProcAddress = badgpu_dlSym2(ctx->eglLibrary, "eglGetProcAddress", "EGL_GetProcAddress");
    ctx->eglMakeCurrent = badgpu_dlSym2(ctx->eglLibrary, "eglMakeCurrent", "EGL_MakeCurrent");
    ctx->eglDestroyContext = badgpu_dlSym2(ctx->eglLibrary, "eglDestroyContext", "EGL_DestroyContext");
    ctx->eglTerminate = badgpu_dlSym2(ctx->eglLibrary, "eglTerminate", "EGL_Terminate");
    ctx->eglCreatePbufferSurface = badgpu_dlSym2(ctx->eglLibrary, "eglCreatePbufferSurface", "EGL_CreatePbufferSurface");
    ctx->eglDestroySurface = badgpu_dlSym2(ctx->eglLibrary, "eglDestroySurface", "EGL_DestroySurface");
    ctx->dsp = ctx->eglGetDisplay(NULL);
    if (!ctx->dsp)
        return badgpu_newWsiCtxError(error, "Could not create EGLDisplay");
    if (!ctx->eglInitialize(ctx->dsp, NULL, NULL))
        return badgpu_newWsiCtxError(error, "Could not initialize EGL");
    void * config;
    int32_t attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES_BIT,
        EGL_NONE
    };
    int32_t configCount;
    if (!ctx->eglChooseConfig(ctx->dsp, attribs, &config, 1, &configCount))
        return badgpu_newWsiCtxError(error, "Failed to choose EGL config");
    if (!configCount)
        return badgpu_newWsiCtxError(error, "No EGL configs");
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
    }
    // If we don't manage to create the PBuffer, then march on regardless.
    // The system may still support surfaceless contexts.
    // Finally, make the context.
    int32_t attribsCtx[] = {
        EGL_NONE
    };
    ctx->ctx = ctx->eglCreateContext(ctx->dsp, config, NULL, attribsCtx);
    if (!ctx->ctx)
        return badgpu_newWsiCtxError(error, "Failed to create EGL context");
    ctx->cfg = config;
    return (BADGPUWSIContext) ctx;
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
        return (void *) BADGPUContextType_GLESv1;
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

