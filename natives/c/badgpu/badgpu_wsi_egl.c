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

BADGPUWSICtx badgpu_newWsiCtx(const char ** error, int * expectDesktopExtensions) {
    *expectDesktopExtensions = 0;
    BADGPUWSICtx ctx = malloc(sizeof(struct BADGPUWSICtx));
    if (!ctx)
        return badgpu_newWsiCtxError(error, "Could not allocate BADGPUWSICtx");
    memset(ctx, 0, sizeof(struct BADGPUWSICtx));
    // Can't guarantee a link to EGL, so we have to do it the *hard* way
    ctx->eglLibrary = dlopen("libEGL.so.1", 2);
    if (!ctx->eglLibrary)
        return badgpu_newWsiCtxError(error, "Could not open EGL");
    ctx->eglGetDisplay = dlsym(ctx->eglLibrary, "eglGetDisplay");
    ctx->eglInitialize = dlsym(ctx->eglLibrary, "eglInitialize");
    ctx->eglChooseConfig = dlsym(ctx->eglLibrary, "eglChooseConfig");
    ctx->eglCreateContext = dlsym(ctx->eglLibrary, "eglCreateContext");
    ctx->eglGetProcAddress = dlsym(ctx->eglLibrary, "eglGetProcAddress");
    ctx->eglMakeCurrent = dlsym(ctx->eglLibrary, "eglMakeCurrent");
    ctx->eglDestroyContext = dlsym(ctx->eglLibrary, "eglDestroyContext");
    ctx->eglTerminate = dlsym(ctx->eglLibrary, "eglTerminate");
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
    if (!ctx->eglMakeCurrent(ctx->dsp, NULL, NULL, ctx->ctx))
        return badgpu_newWsiCtxError(error, "Failed initial eglMakeCurrent");
    return ctx;
}

void badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx) {
    ctx->eglMakeCurrent(ctx->dsp, NULL, NULL, ctx->ctx);
}

void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc) {
    return ctx->eglGetProcAddress(proc);
}

void badgpu_destroyWsiCtx(BADGPUWSICtx ctx) {
    if (!ctx)
        return;
    if (ctx->ctx) {
        ctx->eglMakeCurrent(ctx->dsp, NULL, NULL, NULL);
        ctx->eglDestroyContext(ctx->dsp, ctx->ctx);
    }
    if (ctx->dsp)
        ctx->eglTerminate(ctx->dsp);
    if (ctx->eglLibrary)
        dlclose(ctx->eglLibrary);
    free(ctx);
}

