/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu_glbind.h"
#include <string.h>

// returns failed function name, if any
const char * badgpu_glBind(BADGPUWSIContext ctx, BADGPUGLBind * into, int desktopExt, int logDetailed) {
    // Function bind
#define CHKGLFN(fn) \
if (!(into->fn)) \
    return "Failed to bind function: gl" #fn;

#define BINDGLFN(fn) into->fn = ctx->getProcAddress(ctx, "gl" #fn); \
CHKGLFN(fn)

// WORKAROUND:
// Rigging ANGLE drivers for BadGPU requires shaving off the OES suffix from extensions.
// We should still prefer the suffixes to avoid confusion with GLES2/GLES3.
#define BINDGLFN2(fn, ext) into->fn = ctx->getProcAddress(ctx, "gl" #fn #ext); \
if (!into->fn) into->fn = ctx->getProcAddress(ctx, "gl" #fn); \
CHKGLFN(fn)

#define BINDGLFN2_REV(fn, ext) into->fn = ctx->getProcAddress(ctx, "gl" #fn); \
if (!into->fn) into->fn = ctx->getProcAddress(ctx, "gl" #fn #ext); \
CHKGLFN(fn)

    // I spent a day debugging an NVIDIA setup because of this BS.
    // I wonder how many users this cost.
    // Apparently, Mesa also does this.
    void * fakeProcAddress = ctx->getProcAddress(ctx, "glBADGPUThisFunctionDoesNotExist");
    if (fakeProcAddress && logDetailed)
        printf("BADGPU: GL implementation is lying about support.\n");

    BINDGLFN(GetError);
    BINDGLFN(Enable);
    BINDGLFN(Disable);
    BINDGLFN(EnableClientState);
    BINDGLFN(DisableClientState);
    BINDGLFN(GenTextures);
    BINDGLFN(DeleteTextures);
    BINDGLFN(StencilMask);
    BINDGLFN(ColorMask);
    BINDGLFN(DepthMask);
    BINDGLFN(Scissor);
    BINDGLFN(Viewport);
    BINDGLFN(ClearColor);
    BINDGLFN(PolygonOffset);
    BINDGLFN(PointSize);
    BINDGLFN(LineWidth);
    BINDGLFN(ClearStencil);
    BINDGLFN(Clear);
    BINDGLFN(ReadPixels);
    BINDGLFN(BindTexture);
    BINDGLFN(TexImage2D);
    BINDGLFN(TexParameteri);
    BINDGLFN(DrawArrays);
    BINDGLFN(DrawElements);
    BINDGLFN(VertexPointer);
    BINDGLFN(ColorPointer);
    BINDGLFN(TexCoordPointer);
    BINDGLFN(MatrixMode);
    BINDGLFN(LoadMatrixf);
    BINDGLFN(LoadIdentity);
    BINDGLFN(AlphaFunc);
    BINDGLFN(FrontFace);
    BINDGLFN(CullFace);
    BINDGLFN(DepthFunc);
    BINDGLFN(StencilFunc);
    BINDGLFN(StencilOp);
    BINDGLFN(Color4f);
    BINDGLFN(MultiTexCoord4f);
    BINDGLFN(GetString);
    BINDGLFN(Flush);
    BINDGLFN(Finish);
    if (desktopExt) {
        BINDGLFN2(BlendFuncSeparate, EXT);
        BINDGLFN2(BlendEquationSeparate, EXT);
        BINDGLFN2(GenFramebuffers, EXT);
        BINDGLFN2(DeleteFramebuffers, EXT);
        BINDGLFN2(GenRenderbuffers, EXT);
        BINDGLFN2(DeleteRenderbuffers, EXT);
        BINDGLFN2(RenderbufferStorage, EXT);
        BINDGLFN2(BindFramebuffer, EXT);
        BINDGLFN2(FramebufferRenderbuffer, EXT);
        BINDGLFN2(FramebufferTexture2D, EXT);
        BINDGLFN2(GenerateMipmap, EXT);
        BINDGLFN2(BindRenderbuffer, EXT);
        // Variant-dependent
        BINDGLFN(ClipPlane);
        BINDGLFN(ClearDepth);
        BINDGLFN(DepthRange);
    } else {
        const char * extensions = into->GetString(GL_EXTENSIONS);
        BINDGLFN2(BlendFuncSeparate, OES);
        BINDGLFN2(BlendEquationSeparate, OES);
        if (fakeProcAddress && extensions && !strstr(extensions, "GL_OES_blend_equation_separate")) {
            // WORKAROUND: So this code means I owe you an explanation.
            // On Linux (seen with NVIDIA and Mesa), GetProcAddress is happy to lie about functions existing that silently fail.
            // NVIDIA however apparently CBA to alias glBlendFuncSeparateOES to glBlendFuncSeparate.
            // Binding to the unsuffixed functions, however, works as expected.
            // That in mind, if all three apply:
            // 1. We caught the implementation lying (by asking for a fake function)
            // 2. We're on OpenGL ES
            // 3. The implementation doesn't implement GL_OES_blend_equation_separate (which we need)
            // Then:
            // 1. The implementation is probably NVIDIA's, because who doesn't implement `GL_OES_blend_equation_separate`
            // 2. We probably just got fed dummy pointers
            // 3. If we ask for the 'real' (unsuffixed) functions, they'll probably work
            if (logDetailed)
                printf("BADGPU: badgpu_glbind.c: GL_OES_blend_equation_separate is not supported, but we didn't fail function resolution. Due to NVIDIA, this is suspicious. Working around it.\n");
            BINDGLFN2_REV(BlendFuncSeparate, OES);
            BINDGLFN2_REV(BlendEquationSeparate, OES);
        }
        BINDGLFN2(GenFramebuffers, OES);
        BINDGLFN2(DeleteFramebuffers, OES);
        BINDGLFN2(GenRenderbuffers, OES);
        BINDGLFN2(DeleteRenderbuffers, OES);
        BINDGLFN2(RenderbufferStorage, OES);
        BINDGLFN2(BindFramebuffer, OES);
        BINDGLFN2(FramebufferRenderbuffer, OES);
        BINDGLFN2(FramebufferTexture2D, OES);
        BINDGLFN2(GenerateMipmap, OES);
        BINDGLFN2(BindRenderbuffer, OES);
        // Variant-dependent
        BINDGLFN(ClipPlanef);
        BINDGLFN(ClearDepthf);
        BINDGLFN(DepthRangef);
    }
    return NULL;
}

