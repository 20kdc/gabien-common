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
    HWND hwnd;
    HDC hdc;
    HGLRC ctx;
} * BADGPUWSICtx;

static BADGPUWSIContext badgpu_newWsiCtxError(const char ** error, const char * err) {
    if (error)
        *error = err;
    return 0;
}

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

    WNDCLASS wc = {
        .lpfnWndProc = DefWindowProcA,
        .hInstance = GetModuleHandleA(NULL),
        .hbrBackground = (HBRUSH) (COLOR_BACKGROUND),
        .lpszClassName = "gabien_una_gl_window",
        .style = CS_OWNDC
    };
    RegisterClass(&wc);
    ctx->hwnd = CreateWindowA("gabien_una_gl_window", "una", WS_OVERLAPPEDWINDOW, 0, 0, 256, 256, 0, 0, GetModuleHandleA(NULL), 0);
    if (!ctx->hwnd) {
        free(ctx);
        return badgpu_newWsiCtxError(error, "Could not create working window");
    }
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
    if (!ctx->ctx) {
        badgpu_destroyWsiCtx(ctx);
        return badgpu_newWsiCtxError(error, "Could not create GL context");
    }
    return (BADGPUWSIContext) ctx;
}

BADGPUBool badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx) {
    return wglMakeCurrent(ctx->hdc, ctx->ctx) != 0;
}

void badgpu_wsiCtxStopCurrent(BADGPUWSICtx ctx) {
    wglMakeCurrent(NULL, NULL);
}

void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc) {
    void * main = wglGetProcAddress(proc);
    if (!main)
        return GetProcAddress(GetModuleHandleA("opengl32"), proc);
    return main;
}

void * badgpu_wsiCtxGetValue(BADGPUWSICtx ctx, BADGPUWSIQuery query) {
    if (query == BADGPUWSIQuery_WSIType)
        return (void *) BADGPUWSIType_WGL;
    if (query == BADGPUWSIQuery_LibGL)
        return GetModuleHandleA("opengl32");
    if (query == BADGPUWSIQuery_ContextType)
        return (void *) BADGPUContextType_GL;
    if (query == BADGPUWSIQuery_ContextWrapper)
        return (void *) ctx;

    if (query == BADGPUWSIQuery_WGLHWND)
        return ctx->hwnd;
    if (query == BADGPUWSIQuery_WGLHDC)
        return ctx->hdc;
    if (query == BADGPUWSIQuery_WGLHGLRC)
        return ctx->ctx;

    return NULL;
}

void badgpu_destroyWsiCtx(BADGPUWSICtx ctx) {
    if (!ctx)
        return;
    if (ctx->ctx)
        wglDeleteContext(ctx->ctx);
    if (ctx->hwnd)
        DestroyWindow(ctx->hwnd);
    free(ctx);
}

