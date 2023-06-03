/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu_internal.h"

// WSICTX
struct BADGPUWSICtx {
    void * eglLibrary;
    void * dsp;
    void * ctx;
    void * (KHRABI *eglGetDisplay)(void *);
    unsigned int (KHRABI *eglInitialize)(void *, int32_t *, int32_t *);
    unsigned int (KHRABI *eglChooseConfig)(void *, int32_t *, void *, int32_t, int32_t *);
    void * (KHRABI *eglCreateContext)(void *, void *, void *, int32_t *);
    void * (KHRABI *eglGetProcAddress)(const char *);
    unsigned int (KHRABI *eglMakeCurrent)(void *, void *, void *, void *);
    unsigned int (KHRABI *eglDestroyContext)(void *, void *);
    unsigned int (KHRABI *eglTerminate)(void *);
};

static BADGPUWSICtx badgpu_newWsiCtxError(const char ** error, const char * err) {
    if (error)
        *error = err;
    return 0;
}

static const char * locations[] = {
    "libEGL.so.1",
    "libEGL.so", // Android needs this
    NULL
};

BADGPUWSICtx badgpu_newWsiCtx(const char ** error, int * expectDesktopExtensions) {
    *expectDesktopExtensions = 0;
    BADGPUWSICtx ctx = malloc(sizeof(struct BADGPUWSICtx));
    if (!ctx)
        return badgpu_newWsiCtxError(error, "Could not allocate BADGPUWSICtx");
    memset(ctx, 0, sizeof(struct BADGPUWSICtx));
    // Can't guarantee a link to EGL, so we have to do it the *hard* way
    ctx->eglLibrary = badgpu_dlOpen(locations, "BADGPU_EGL_LIBRARY");
    if (!ctx->eglLibrary)
        return badgpu_newWsiCtxError(error, "Could not open EGL");
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
    ctx->dsp = ctx->eglGetDisplay(NULL);
    if (!ctx->dsp)
        return badgpu_newWsiCtxError(error, "Could not create EGLDisplay");
    if (!ctx->eglInitialize(ctx->dsp, NULL, NULL))
        return badgpu_newWsiCtxError(error, "Could not initialize EGL");
    void * config;
    int32_t attribs[] = {
        0x3038
    };
    int32_t configCount;
    if (!ctx->eglChooseConfig(ctx->dsp, attribs, &config, 1, &configCount))
        return badgpu_newWsiCtxError(error, "Failed to choose EGL config");
    if (!configCount)
        return badgpu_newWsiCtxError(error, "No EGL configs");
    int32_t attribs2[] = {
        0x3038
    };
    ctx->ctx = ctx->eglCreateContext(ctx->dsp, config, NULL, attribs2);
    if (!ctx->ctx)
        return badgpu_newWsiCtxError(error, "Failed to create EGL context");
    return ctx;
}

BADGPUBool badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx) {
    return ctx->eglMakeCurrent(ctx->dsp, NULL, NULL, ctx->ctx) != 0;
}

void badgpu_wsiCtxStopCurrent(BADGPUWSICtx ctx) {
    ctx->eglMakeCurrent(ctx->dsp, NULL, NULL, NULL);
}

void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc) {
    return ctx->eglGetProcAddress(proc);
}

void badgpu_destroyWsiCtx(BADGPUWSICtx ctx) {
    if (!ctx)
        return;
    if (ctx->ctx)
        ctx->eglDestroyContext(ctx->dsp, ctx->ctx);
    if (ctx->dsp)
        ctx->eglTerminate(ctx->dsp);
    if (ctx->eglLibrary)
        badgpu_dlClose(ctx->eglLibrary);
    free(ctx);
}

