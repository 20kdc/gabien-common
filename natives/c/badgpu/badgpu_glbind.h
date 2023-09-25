/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#ifndef BADGPU_GLBIND_H_
#define BADGPU_GLBIND_H_

#include "badgpu_internal.h"

// Tokens

// Luckily, these are equal between their regular and suffixed versions.
#define GL_FRAMEBUFFER 0x8D40
#define GL_RENDERBUFFER 0x8D41
#define GL_DEPTH24_STENCIL8 0x88F0
#define GL_COLOR_ATTACHMENT0 0x8CE0
#define GL_DEPTH_ATTACHMENT 0x8D00
#define GL_STENCIL_ATTACHMENT 0x8D20
#define GL_TEXTURE_2D 0x0DE1

#define GL_UNSIGNED_BYTE 0x1401
#define GL_UNSIGNED_SHORT 0x1403
#define GL_FLOAT 0x1406

#define GL_VERTEX_ARRAY 0x8074
#define GL_COLOR_ARRAY 0x8076
#define GL_TEXTURE_COORD_ARRAY 0x8078

#define GL_ALPHA 0x1906
#define GL_RGB 0x1907
#define GL_RGBA 0x1908
#define GL_LUMINANCE 0x1909
#define GL_LUMINANCE_ALPHA 0x190A

#define GL_ALPHA_TEST 0x0BC0
#define GL_STENCIL_TEST 0x0B90
#define GL_DEPTH_TEST 0x0B71
#define GL_SCISSOR_TEST 0x0C11
#define GL_CULL_FACE 0x0B44

#define GL_FRONT 0x0404
#define GL_BACK 0x0405

#define GL_DEPTH_BUFFER_BIT 0x00000100
#define GL_STENCIL_BUFFER_BIT 0x00000400
#define GL_COLOR_BUFFER_BIT 0x00004000

#define GL_NEAREST 0x2600
#define GL_LINEAR 0x2601
#define GL_NEAREST_MIPMAP_NEAREST 0x2700
#define GL_LINEAR_MIPMAP_LINEAR 0x2703
#define GL_REPEAT 0x2901
#define GL_CLAMP_TO_EDGE 0x812F
#define GL_TEXTURE_MAG_FILTER 0x2800
#define GL_TEXTURE_MIN_FILTER 0x2801
#define GL_TEXTURE_WRAP_S 0x2802
#define GL_TEXTURE_WRAP_T 0x2803

#define GL_MODELVIEW 0x1700
#define GL_PROJECTION 0x1701
#define GL_TEXTURE 0x1702

#define GL_POLYGON_OFFSET_FILL 0x8037

#define GL_BLEND 0x0BE2

#define GL_CW 0x0900
#define GL_CCW 0x0901

#define GL_TEXTURE0 0x84C0

#define GL_EXTENSIONS 0x1F03
#define GL_DONT_CARE 0x1100
#define GL_DEBUG_SOURCE_THIRD_PARTY 0x8249
#define GL_DEBUG_TYPE_OTHER 0x8251
#define GL_DEBUG_SEVERITY_NOTIFICATION 0x826B
#define GL_DEBUG_OUTPUT 0x92E0

#define GL_CLIP_PLANE0 0x3000

typedef struct BADGPUGLBind {
    int32_t (KHRABI *GetError)();
    void (KHRABI *Enable)(int32_t);
    void (KHRABI *Disable)(int32_t);
    void (KHRABI *EnableClientState)(int32_t);
    void (KHRABI *DisableClientState)(int32_t);
    void (KHRABI *GenTextures)(int32_t, uint32_t *);
    void (KHRABI *DeleteTextures)(int32_t, uint32_t *);
    void (KHRABI *StencilMask)(int32_t);
    void (KHRABI *ColorMask)(unsigned char, unsigned char, unsigned char, unsigned char);
    void (KHRABI *DepthMask)(unsigned char);
    void (KHRABI *Scissor)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *Viewport)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *ClearColor)(float, float, float, float);
    void (KHRABI *ClearDepthf)(float);
    void (KHRABI *DepthRangef)(float, float);
    void (KHRABI *PolygonOffset)(float, float);
    void (KHRABI *PointSize)(float);
    void (KHRABI *LineWidth)(float);
    void (KHRABI *ClearStencil)(int32_t);
    void (KHRABI *Clear)(int32_t);
    void (KHRABI *ReadPixels)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, void *);
    void (KHRABI *BindTexture)(int32_t, uint32_t);
    void (KHRABI *TexImage2D)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, const void *);
    void (KHRABI *TexParameteri)(int32_t, int32_t, int32_t);
    void (KHRABI *DrawArrays)(int32_t, int32_t, int32_t);
    void (KHRABI *DrawElements)(int32_t, int32_t, int32_t, const void *);
    void (KHRABI *VertexPointer)(int32_t, int32_t, int32_t, const void *);
    void (KHRABI *ColorPointer)(int32_t, int32_t, int32_t, const void *);
    void (KHRABI *TexCoordPointer)(int32_t, int32_t, int32_t, const void *);
    void (KHRABI *MatrixMode)(int32_t);
    void (KHRABI *LoadMatrixf)(const float *);
    void (KHRABI *LoadIdentity)();
    void (KHRABI *AlphaFunc)(int32_t, float);
    void (KHRABI *FrontFace)(int32_t);
    void (KHRABI *CullFace)(int32_t);
    void (KHRABI *DepthFunc)(int32_t);
    void (KHRABI *StencilFunc)(int32_t, int32_t, int32_t);
    void (KHRABI *StencilOp)(int32_t, int32_t, int32_t);
    void (KHRABI *Color4f)(float, float, float, float);
    void (KHRABI *MultiTexCoord4f)(int32_t, float, float, float, float);
    const char * (KHRABI *GetString)(int32_t);
    void (KHRABI *Flush)();
    void (KHRABI *Finish)();
    // Desktop/Non-Desktop variable area
    void (KHRABI *BlendFuncSeparate)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *BlendEquationSeparate)(int32_t, int32_t);
    void (KHRABI *GenFramebuffers)(int32_t, uint32_t *);
    void (KHRABI *DeleteFramebuffers)(int32_t, uint32_t *);
    void (KHRABI *GenRenderbuffers)(int32_t, uint32_t *);
    void (KHRABI *DeleteRenderbuffers)(int32_t, uint32_t *);
    void (KHRABI *RenderbufferStorage)(int32_t, int32_t, int32_t, int32_t);
    void (KHRABI *BindFramebuffer)(int32_t, uint32_t);
    void (KHRABI *FramebufferRenderbuffer)(int32_t, int32_t, int32_t, uint32_t);
    void (KHRABI *FramebufferTexture2D)(int32_t, int32_t, int32_t, uint32_t, int32_t);
    void (KHRABI *GenerateMipmap)(int32_t);
    void (KHRABI *BindRenderbuffer)(int32_t, uint32_t);
    // Variant-dependent
    void (KHRABI *ClipPlane)(int32_t, const double *);
    void (KHRABI *ClipPlanef)(int32_t, const float *);
} BADGPUGLBind;

// returns failed function name, if any
const char * badgpu_glBind(BADGPUWSIContext ctx, BADGPUGLBind * into, int desktopExt);

#endif

