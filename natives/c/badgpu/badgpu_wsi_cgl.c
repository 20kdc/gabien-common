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

static BADGPUWSICtx badgpu_newWsiCtxError(const char ** error, const char * err) {
    if (error)
        *error = err;
    return 0;
}

BADGPUWSICtx badgpu_newWsiCtx(const char ** error, int * expectDesktopExtensions) {
    *expectDesktopExtensions = 1;
    BADGPUWSICtx ctx = malloc(sizeof(struct BADGPUWSICtx));
    if (!ctx)
        return badgpu_newWsiCtxError(error, "Could not allocate BADGPUWSICtx");
    memset(ctx, 0, sizeof(struct BADGPUWSICtx));
    ctx->glLibrary = dlopen("/System/Library/Frameworks/OpenGL.framework/Libraries/libGL.dylib", 2);
    if (!ctx->glLibrary)
        return badgpu_newWsiCtxError(error, "Could not open CGL");
    ctx->CGLChoosePixelFormat = dlsym(ctx->glLibrary, "CGLChoosePixelFormat");
    ctx->CGLCreateContext = dlsym(ctx->glLibrary, "CGLCreateContext");
    ctx->CGLSetCurrentContext = dlsym(ctx->glLibrary, "CGLSetCurrentContext");
    ctx->CGLDestroyContext = dlsym(ctx->glLibrary, "CGLDestroyContext");
    ctx->CGLDestroyPixelFormat = dlsym(ctx->glLibrary, "CGLDestroyPixelFormat");
    int32_t attribs[] = {
        0
    };
    int32_t ignoreMe;
    if (ctx->CGLChoosePixelFormat(attribs, &ctx->pixFmt, &ignoreMe))
        return badgpu_newWsiCtxError(error, "Failed to choose CGL config");
    if (!ctx->pixFmt)
        return badgpu_newWsiCtxError(error, "No CGL configs");
    int32_t attribs2[] = {
        0
    };
    if (ctx->CGLCreateContext(ctx->pixFmt, NULL, &ctx->ctx))
        return badgpu_newWsiCtxError(error, "Failed to create CGL context");
    if (!ctx->ctx)
        return badgpu_newWsiCtxError(error, "Failed to create CGL context");
    return ctx;
}

BADGPUBool badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx) {
    return !ctx->CGLSetCurrentContext(ctx->ctx);
}

void badgpu_wsiCtxStopCurrent(BADGPUWSICtx ctx) {
    ctx->CGLSetCurrentContext(NULL);
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

