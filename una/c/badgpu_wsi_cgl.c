/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu_internal.h"

// WSICTX
struct BADGPUWSICtx {
    void * glLibrary;
    void * pixFmt;
    void * ctx;
    unsigned int (*CGLChoosePixelFormat)(int32_t *, void *, int32_t *);
    unsigned int (*CGLCreateContext)(void *, void *, void *);
    unsigned int (*CGLSetCurrentContext)(void *);
    unsigned int (*CGLDestroyContext)(void *);
    unsigned int (*CGLDestroyPixelFormat)(void *);
};

static BADGPUWSICtx badgpu_newWsiCtxError(char ** error, const char * err) {
    if (error)
        *error = (char *) err;
    return 0;
}

BADGPUWSICtx badgpu_newWsiCtx(char ** error) {
    BADGPUWSICtx ctx = malloc(sizeof(struct BADGPUWSICtx));
    if (!ctx)
        return badgpu_newWsiCtxError(error, "Could not allocate BADGPUWSICtx");
    memset(ctx, 0, sizeof(struct BADGPUWSICtx));
    ctx->glLibrary = dlOpen("/System/Library/Frameworks/OpenGL.framework/Libraries/libGL.dylib", 2);
    if (!ctx->glLibrary)
        return badgpu_newWsiCtxError(error, "Could not open EGL");
    ctx->CGLChoosePixelFormat = dlsym(ctx->glLibrary, "CGLChoosePixelFormat");
    ctx->CGLCreateContext = dlsym(ctx->glLibrary, "CGLCreateContext");
    ctx->CGLSetCurrentContext = dlsym(ctx->glLibrary, "CGLSetCurrentContext");
    ctx->CGLDestroyContext = dlsym(ctx->glLibrary, "CGLDestroyContext");
    ctx->CGLDestroyPixelFormat = dlsym(ctx->glLibrary, "CGLDestroyPixelFormat");
    void * config;
    int32_t attribs[] = {
        0
    };
    int32_t configCount;
    if (!ctx->eglChooseConfig(ctx->dsp, attribs, &config, 1, &configCount))
        return badgpu_newWsiCtxError(error, "Failed to choose EGL config");
    if (!configCount)
        return badgpu_newWsiCtxError(error, "No EGL configs");
    int32_t attribs2[] = {
        0
    };
    ctx->ctx = ctx->eglCreateContext(ctx->dsp, config, NULL, attribs2);
    if (!ctx->ctx)
        return badgpu_newWsiCtxError(error, "Failed to create EGL context");
    if (!ctx->eglMakeCurrent(ctx->dsp, NULL, NULL, ctx->ctx))
        return badgpu_newWsiCtxError(error, "Failed initial eglMakeCurrent");
    return ctx;
}

void badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx) {
    ctx->CGLSetCurrentContext(ctx->ctx);
}

void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc) {
    return dlsym(ctx->glLibrary, proc);
}

void badgpu_destroyWsiCtx(BADGPUWSICtx ctx) {
    if (!ctx)
        return;
    if (ctx->ctx)
        ctx->CGLDestroyContext(ctx->ctx);
    if (ctx->pixFmt)
        ctx->CGLDestroyPixelFormat(ctx->pixFmt);
    if (ctx->glLibrary)
        dlclose(ctx->glLibrary);
    free(ctx);
}

