/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu_internal.h"

// WSICTX
struct BADGPUWSICtx {
#ifdef WIN32
    HWND window;
    HDC hdc;
    HGLRC ctx;
#else
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
#endif
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
#ifdef WIN32
    WNDCLASS wc = {
        .lpfnWndProc = DefWindowProcA,
        .hInstance = GetModuleHandleA(NULL),
        .hbrBackground = (HBRUSH) (COLOR_BACKGROUND),
        .lpszClassName = "gabien_una_gl_window",
        .style = CS_OWNDC
    };
    RegisterClass(&wc);
    ctx->hwnd = CreateWindowA("gabien_una_gl_window", "una", WS_OVERLAPPEDWINDOW | WS_VISIBLE, 0, 0, 256, 256, 0, 0, GetModuleHandleA(NULL), 0);
    if (!ctx->hwnd)
        return badgpu_newWsiCtxError(error, "Could not create working window");
    ctx->hdc = GetDC(ctx->hwnd);
    PIXELFORMATDESCRIPTOR pfd = {
        .nSize = sizeof(PIXELFORMATDESCRIPTOR),
        .nVersion = 1,
        .dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL,
        .iPixelType = PFD_TYPE_RGBA,
        .cColorBits = 32,
        .cDepthBits = 24,
        .cStencilBits = 8,
        .iLayerType = PFD_MAIN_PLANE
    };
    int pixFmt = ChoosePixelFormat(ctx->hdc, &pfd);
    SetPixelFormat(ctx->hdc, pixFmt, &pfd);
    ctx->ctx = wglCreateContext(ctx->hdc);
    if (!ctx->ctx)
        return badgpu_newWsiCtxError(error, "Could not create GL context");
    // Done, now make it current!
    wglMakeCurrent(ctx->hdc, ctx->ctx);
    return ctx;
#else
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
#endif
}

void badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx) {
#ifdef WIN32
    wglMakeCurrent(ctx->hdc, ctx->ctx);
#else
    ctx->eglMakeCurrent(ctx->dsp, NULL, NULL, ctx->ctx);
#endif
}

void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc) {
#ifdef WIN32
    return wglGetProcAddress(proc);
#else
    return ctx->eglGetProcAddress(proc);
#endif
}

void badgpu_destroyWsiCtx(BADGPUWSICtx ctx) {
    if (!ctx)
        return;
#ifdef WIN32
    if (ctx->ctx)
        wglDeleteContext(ctx->ctx);
    if (ctx->hwnd)
        DestroyWindow(ctx->window);
#else
    if (ctx->ctx)
        ctx->eglDestroyContext(ctx->dsp, ctx->ctx);
    if (ctx->dsp)
        ctx->eglTerminate(ctx->dsp);
    if (ctx->eglLibrary)
        dlclose(ctx->eglLibrary);
#endif
    free(ctx);
}

