/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu_internal.h"

// WSICTX
struct BADGPUWSICtx {
    HWND window;
    HDC hdc;
    HGLRC ctx;
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
}

void badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx) {
    wglMakeCurrent(ctx->hdc, ctx->ctx);
}

void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc) {
    return wglGetProcAddress(proc);
}

void badgpu_destroyWsiCtx(BADGPUWSICtx ctx) {
    wglMakeCurrent(NULL, NULL);
    if (!ctx)
        return;
    if (ctx->ctx)
        wglDeleteContext(ctx->ctx);
    if (ctx->hwnd)
        DestroyWindow(ctx->window);
    free(ctx);
}

