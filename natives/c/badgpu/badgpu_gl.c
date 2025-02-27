/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * BadGPU Reference Implementation : GL wrapper
 */

#include "badgpu_internal.h"
#include "badgpu_glbind.h"

typedef struct BADGPUInstanceGL {
    BADGPUInstancePriv base;
    int disabledTextureMatrices;
    uint32_t fbo;
    uint32_t fboBoundTex, fboBoundDS;
    // wsi stuff
    BADGPUGLBind gl;
} BADGPUInstanceGL;
#define BG_INSTANCE_GL(x) ((BADGPUInstanceGL *) (x))

typedef struct BADGPUDSBufferGL {
    BADGPUDSBufferPriv base;
    uint32_t rbo;
} BADGPUDSBufferGL;
#define BG_DSBUFFER_GL(x) ((BADGPUDSBufferGL *) (x))

// Instance Creation

BADGPU_INLINE BADGPUInstanceGL * badgpuGLBChk(BADGPUInstance bi, const char * location) {
    return BG_INSTANCE_GL(badgpuBChk(bi, location));
}

static void destroyGLInstance(BADGPUObject obj) {
    BADGPUInstanceGL * bi = badgpuGLBChk(obj, "destroyGLInstance");
    if (bi) {
        bi->gl.DeleteFramebuffers(1, &bi->fbo);
        bi->base.ctx->stopCurrent(bi->base.ctx);
        bi->base.ctx->close(bi->base.ctx);
        free(obj);
    }
}

static BADGPUBool badgpuChkInnards(BADGPUInstanceGL * bi, const char * location) {
    BADGPUBool ok = 1;
    while (1) {
        int err = bi->gl.GetError();
        if (!err)
            break;
        if (bi->base.canPrintf)
            printf("BADGPU: %s: GL error 0x%x\n", location, err);
        ok = 0;
    }
    return ok;
}

BADGPU_INLINE BADGPUBool badgpuChk(BADGPUInstanceGL * bi, const char * location, BADGPUBool failureIsAggressive) {
    if (bi->base.backendCheck)
        return badgpuChkInnards(bi, location) || (failureIsAggressive && !bi->base.backendCheckAggressive);
    return 1;
}

static const char * bglGetMetaInfo(BADGPUInstancePriv * instance, BADGPUMetaInfoType mi) {
    return BG_INSTANCE_GL(instance)->gl.GetString(mi);
}

static BADGPUBool bglBindInstance(BADGPUInstancePriv * instance) {
    BADGPUInstanceGL * bi = BG_INSTANCE_GL(instance);
    if (!bi->base.ctx->makeCurrent(bi->base.ctx)) {
        if (bi->base.canPrintf)
            printf("BADGPU: badgpuBindInstance: failed to bind\n");
        return 0;
    }
    return 1;
}

static void bglUnbindInstance(BADGPUInstancePriv * bi) {
    bi->ctx->stopCurrent(bi->ctx);
}

static void bglFlushInstance(BADGPUInstancePriv * bi) {
    BG_INSTANCE_GL(bi)->gl.Flush();
}

static void bglFinishInstance(BADGPUInstancePriv * bi) {
    BG_INSTANCE_GL(bi)->gl.Finish();
}

// FBM

BADGPU_INLINE BADGPUBool fbSetup(struct BADGPUInstanceGL * bi, BADGPUTexture sTexture, BADGPUDSBuffer sDSBuffer) {
    BADGPUTexturePriv * sTex = BG_TEXTURE(sTexture);
    BADGPUDSBufferGL * sDS = BG_DSBUFFER_GL(sDSBuffer);
    bi->gl.BindFramebuffer(GL_FRAMEBUFFER, bi->fbo);
    // badgpuChk(*bi, "fbSetup1", 0);
    // OPT:
    //  JUST TO BE CLEAR.
    //  JUST TO BE ABSOLUTELY CLEAR.
    //  PERFORMING NO-OP FBO ATTACHMENT REBINDS; YES, EVEN ONES THAT JUST REBIND AN ATTACHMENT TO EXACTLY WHAT IT WAS;
    //  WILL CAUSE THE ARM MALI DRIVERS TO RELOCATE YOUR SKELETON OUTSIDE OF YOUR BODY.
    //  HARDWARE DETAILS:
    //   ARM
    //   Mali-T830
    //   OpenGL ES-CM 1.1 v1.r20p0-01rel0.9a7fca3 f7dd712a473937294a8ae24b1
    if (sTex && (bi->fboBoundTex != sTex->glTex)) {
        bi->gl.FramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, sTex->glTex, 0);
        bi->fboBoundTex = sTex->glTex;
    } else if ((!sTex) && (bi->fboBoundTex != 0)) {
        bi->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, 0);
        bi->fboBoundTex = 0;
    }
    // badgpuChk(*bi, "fbSetup2", 0);
    uint32_t newRBO = sDS ? sDS->rbo : 0;
    if (bi->fboBoundDS != newRBO) {
        bi->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, newRBO);
        bi->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, newRBO);
        bi->fboBoundDS = newRBO;
    }
    return badgpuChk(bi, "fbSetup", 1);
}

// Texture/2D Buffer Management

static void destroyTexture(BADGPUObject obj) {
    BADGPUTexturePriv * tex = BG_TEXTURE(obj);
    BADGPUInstanceGL * bi = badgpuGLBChk((BADGPUInstance) tex->i, "destroyTexture");
    if (!bi)
        return;
    // make SURE it's not bound to our FBO
    bi->gl.BindFramebuffer(GL_FRAMEBUFFER, bi->fbo);
    bi->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, 0);
    bi->fboBoundTex = 0;
    // continue
    if (tex->autoDel) {
        bi->gl.DeleteTextures(1, &tex->glTex);
        badgpuChk(bi, "destroyTexture", 0);
    }
    badgpuUnref((BADGPUObject) bi);
    free(tex);
}

static BADGPUTexture bglNewTexture(BADGPUInstancePriv * instance,
    int16_t width, int16_t height, const void * data) {

    BADGPUTexturePriv * tex = malloc(sizeof(BADGPUTexturePriv));
    if (!tex) {
        badgpuErr(instance, "badgpuNewTexture: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroyTexture);

    tex->i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));
    tex->autoDel = 1;

    BADGPUInstanceGL * bi = BG_INSTANCE_GL(instance);

    bi->gl.GenTextures(1, &tex->glTex);

    bi->gl.BindTexture(GL_TEXTURE_2D, tex->glTex);
    bi->gl.TexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

    if (!badgpuChk(bi, "badgpuNewTexture", 1)) {
        badgpuUnref((BADGPUTexture) tex);
        return NULL;
    }
    return (BADGPUTexture) tex;
}

static void destroyDSBuffer(BADGPUObject obj) {
    BADGPUDSBufferGL * ds = BG_DSBUFFER_GL(obj);
    BADGPUInstanceGL * bi = badgpuGLBChk((BADGPUInstance) ds->base.i, "destroyDSBuffer");
    if (!bi)
        return;
    // make SURE it's not bound to our FBO
    bi->gl.BindFramebuffer(GL_FRAMEBUFFER, bi->fbo);
    bi->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
    bi->gl.FramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, 0);
    bi->fboBoundDS = 0;
    // continue
    bi->gl.DeleteRenderbuffers(1, &ds->rbo);
    badgpuChk(bi, "destroyDSBuffer", 0);
    badgpuUnref((BADGPUObject) bi);
    free(ds);
}

static BADGPUDSBuffer bglNewDSBuffer(BADGPUInstancePriv * instance,
    int16_t width, int16_t height) {
    BADGPUInstanceGL * bi = BG_INSTANCE_GL(instance);
    BADGPUDSBufferGL * ds = malloc(sizeof(BADGPUDSBufferGL));
    if (!ds) {
        badgpuErr(&bi->base, "badgpuNewDSBuffer: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) ds, destroyDSBuffer);

    ds->base.i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));
    bi->gl.GenRenderbuffers(1, &ds->rbo);
    bi->gl.BindRenderbuffer(GL_RENDERBUFFER, ds->rbo);
    bi->gl.RenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);

    if (!badgpuChk(bi, "badgpuNewDSBuffer", 1)) {
        badgpuUnref((BADGPUDSBuffer) ds);
        return NULL;
    }
    return (BADGPUDSBuffer) ds;
}

static BADGPUBool bglGenerateMipmap(void * texture) {
    BADGPUTexturePriv * tex = (BADGPUTexturePriv *) texture;
    BADGPUInstanceGL * bi = (BADGPUInstanceGL *) tex->i;
    bi->gl.BindTexture(GL_TEXTURE_2D, tex->glTex);
    bi->gl.GenerateMipmap(GL_TEXTURE_2D);
    return badgpuChk(bi, "badgpuGenerateMipmap", 0);
}

static BADGPUBool bglReadPixelsRGBA8888(void * texture,
    uint16_t x, uint16_t y, int16_t width, int16_t height,
    void * data) {
    BADGPUInstanceGL * bi = BG_INSTANCE_GL(BG_TEXTURE(texture)->i);
    if (!fbSetup(bi, texture, NULL))
        return 0;
    bi->gl.ReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
    return badgpuChk(bi, "badgpuReadPixels", 0);
}

// Drawing Commands

BADGPU_INLINE BADGPUBool drawingCmdSetup(
    BADGPUInstanceGL * bi,
    BADGPU_SESSIONFLAGS
) {
    if (!fbSetup(bi, sTexture, sDSBuffer))
        return 0;
    bi->gl.ColorMask(
        (sFlags & BADGPUSessionFlags_MaskR) ? 1 : 0,
        (sFlags & BADGPUSessionFlags_MaskG) ? 1 : 0,
        (sFlags & BADGPUSessionFlags_MaskB) ? 1 : 0,
        (sFlags & BADGPUSessionFlags_MaskA) ? 1 : 0
    );
    // OPT: If we don't have a DSBuffer we don't need to setup the mask for it.
    if (sDSBuffer) {
        // StencilAll is deliberately at bottom of flags for this
        bi->gl.StencilMask(sFlags & BADGPUSessionFlags_StencilAll);
        bi->gl.DepthMask((sFlags & BADGPUSessionFlags_MaskDepth) ? 1 : 0);
    }
    if (sFlags & BADGPUSessionFlags_Scissor) {
        bi->gl.Enable(GL_SCISSOR_TEST);
        bi->gl.Scissor(sScX, sScY, sScWidth, sScHeight);
    } else {
        bi->gl.Disable(GL_SCISSOR_TEST);
    }
    return 1;
}

static BADGPUBool bglDrawClear(
    struct BADGPUInstancePriv * instance,
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
) {
    BADGPUInstanceGL * bi = BG_INSTANCE_GL(instance);
    if (!drawingCmdSetup(bi, BADGPU_SESSIONFLAGS_PASSTHROUGH))
        return 0;
    int32_t cFlags = 0;
    if (sFlags & BADGPUSessionFlags_MaskRGBA) {
        bi->gl.ClearColor(cR, cG, cB, cA);
        cFlags |= GL_COLOR_BUFFER_BIT;
    }
    // OPT: If we don't have a DSBuffer we don't need to setup the clear for it.
    if (sDSBuffer) {
        if (sFlags & BADGPUSessionFlags_MaskDepth) {
            if (bi->gl.ClearDepthf) {
                bi->gl.ClearDepthf(depth);
            } else {
                bi->gl.ClearDepth(depth);
            }
            cFlags |= GL_DEPTH_BUFFER_BIT;
        }
        if (sFlags & BADGPUSessionFlags_StencilAll) {
            bi->gl.ClearStencil(stencil);
            cFlags |= GL_STENCIL_BUFFER_BIT;
        }
    }
    if (cFlags)
        bi->gl.Clear(cFlags);
    return badgpuChk(bi, "badgpuDrawClear", 0);
}

static int32_t convertBlendWeight(int32_t bw) {
    switch (bw) {
    // GL_ZERO
    case BADGPUBlendWeight_Zero: return 0;
    // GL_ONE
    case BADGPUBlendWeight_One: return 1;
    // GL_SRC_ALPHA_SATURATE
    case BADGPUBlendWeight_SrcAlphaSaturate: return 0x308;
    // GL_DST_COLOR
    case BADGPUBlendWeight_Dst: return 0x306;
    // GL_ONE_MINUS_DST_COLOR
    case BADGPUBlendWeight_InvertDst: return 0x307;
    // GL_DST_ALPHA
    case BADGPUBlendWeight_DstA: return 0x304;
    // GL_ONE_MINUS_DST_ALPHA
    case BADGPUBlendWeight_InvertDstA: return 0x305;
    // GL_SRC_COLOR
    case BADGPUBlendWeight_Src: return 0x300;
    // GL_ONE_MINUS_SRC_COLOR
    case BADGPUBlendWeight_InvertSrc: return 0x301;
    // GL_SRC_ALPHA
    case BADGPUBlendWeight_SrcA: return 0x0302;
    // GL_ONE_MINUS_SRC_ALPHA
    case BADGPUBlendWeight_InvertSrcA: return 0x303;
    default: return 0;
    }
}

static int32_t convertBlendOp(int32_t be) {
    switch (be) {
    // GL_FUNC_ADD
    case BADGPUBlendOp_Add: return 0x8006;
    // GL_FUNC_SUBTRACT
    case BADGPUBlendOp_Sub: return 0x800A;
    // GL_FUNC_REVERSE_SUBTRACT
    case BADGPUBlendOp_ReverseSub: return 0x800B;
    default: return 0;
    }
}

static BADGPUBool bglDrawGeom(
    struct BADGPUInstancePriv * instance,
    BADGPU_SESSIONFLAGS,
    uint32_t flags,
    // Vertex Loader
    int32_t vPosD, const float * vPos,
    const float * vCol,
    int32_t vTCD, const float * vTC,
    BADGPUPrimitiveType pType, float plSize,
    uint32_t iStart, uint32_t iCount, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * mvMatrix,
    // Viewport
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    // Fragment Shader
    BADGPUTexture texture, const BADGPUMatrix * matrixT,
    const float * clipPlane, BADGPUCompare atFunc, float atRef,
    // Stencil Test
    BADGPUCompare stFunc, uint8_t stRef, uint8_t stMask,
    BADGPUStencilOp stSF, BADGPUStencilOp stDF, BADGPUStencilOp stDP,
    // Depth Test / DepthRange / PolygonOffset
    BADGPUCompare dtFunc, float depthN, float depthF, float poFactor, float poUnits,
    // Blending
    uint32_t blendProgram
) {
    BADGPUInstanceGL * bi = BG_INSTANCE_GL(instance);
    if (!drawingCmdSetup(bi, BADGPU_SESSIONFLAGS_PASSTHROUGH))
        return 0;

    // Vertex Shader
    bi->gl.MatrixMode(GL_PROJECTION);
    if (!mvMatrix) bi->gl.LoadIdentity(); else bi->gl.LoadMatrixf((void *) mvMatrix);

    // DepthRange/Viewport
    bi->gl.Viewport(vX, vY, vW, vH);

    // Fragment Shader
    if (texture) {
        bi->gl.Enable(GL_TEXTURE_2D);
        bi->gl.BindTexture(GL_TEXTURE_2D, BG_TEXTURE(texture)->glTex);
        if (!bi->disabledTextureMatrices) {
            bi->gl.MatrixMode(GL_TEXTURE);
            if (!badgpuChkInnards(bi, "badgpuDrawGeom: glMatrixMode(GL_TEXTURE) [disabling support for texture matrices and continuing]")) {
                bi->disabledTextureMatrices = 1;
            } else {
                if (!matrixT) bi->gl.LoadIdentity(); else bi->gl.LoadMatrixf((void *) matrixT);
            }
        }

        bi->gl.TexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, (flags & BADGPUDrawFlags_WrapS) ? GL_REPEAT : GL_CLAMP_TO_EDGE);
        bi->gl.TexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, (flags & BADGPUDrawFlags_WrapT) ? GL_REPEAT : GL_CLAMP_TO_EDGE);

        int32_t minFilter = ((flags & BADGPUDrawFlags_Mipmap) ?
            ((flags & BADGPUDrawFlags_MinLinear) ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST_MIPMAP_NEAREST)
            :
            ((flags & BADGPUDrawFlags_MinLinear) ? GL_LINEAR : GL_NEAREST)
        );
        bi->gl.TexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
        bi->gl.TexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, (flags & BADGPUDrawFlags_MagLinear) ? GL_LINEAR : GL_NEAREST);
    } else {
        bi->gl.Disable(GL_TEXTURE_2D);
    }

    if (clipPlane) {
        bi->gl.Enable(GL_CLIP_PLANE0);
        if (bi->gl.ClipPlanef) {
            bi->gl.ClipPlanef(GL_CLIP_PLANE0, clipPlane);
        } else {
            double tmp[4];
            tmp[0] = clipPlane[0]; tmp[1] = clipPlane[1]; tmp[2] = clipPlane[2]; tmp[3] = clipPlane[3];
            bi->gl.ClipPlane(GL_CLIP_PLANE0, tmp);
        }
    } else {
        bi->gl.Disable(GL_CLIP_PLANE0);
    }

    // Alpha Test
    if (atFunc != BADGPUCompare_Always) {
        bi->gl.Enable(GL_ALPHA_TEST);
        // no conversion as values deliberately match
        bi->gl.AlphaFunc(atFunc, atRef);
    } else {
        bi->gl.Disable(GL_ALPHA_TEST);
    }

    // OPT: Depth and stencil test are force-disabled by GL if we have no d/s.
    // (ES1.1 4.1.5 Stencil Test, 4.1.6 Depth Buffer Test, last paragraph of
    //  both)
    // That in mind, skip anything we dare to.
    if (sDSBuffer) {
        if (bi->gl.DepthRangef) {
            bi->gl.DepthRangef(depthN, depthF);
        } else {
            bi->gl.DepthRange(depthN, depthF);
        }

        // PolygonOffset
        bi->gl.Enable(GL_POLYGON_OFFSET_FILL);
        bi->gl.PolygonOffset(poFactor, poUnits);

        // Stencil Test
        if (flags & BADGPUDrawFlags_StencilTest) {
            bi->gl.Enable(GL_STENCIL_TEST);
            // no conversion as values deliberately match
            bi->gl.StencilFunc(stFunc, stRef, stMask);
            bi->gl.StencilOp(stSF, stDF, stDP);
        } else {
            bi->gl.Disable(GL_STENCIL_TEST);
        }

        // Depth Test
        if (dtFunc != BADGPUCompare_Always) {
            bi->gl.Enable(GL_DEPTH_TEST);
            // no conversion as values deliberately match
            bi->gl.DepthFunc(dtFunc);
        } else {
            bi->gl.Disable(GL_DEPTH_TEST);
        }
    }

    // Misc. Flags Stuff
    bi->gl.FrontFace((flags & BADGPUDrawFlags_FrontFaceCW) ? GL_CW : GL_CCW);

    if (flags & BADGPUDrawFlags_CullFace) {
        bi->gl.Enable(GL_CULL_FACE);
        bi->gl.CullFace((flags & BADGPUDrawFlags_CullFaceFront) ? GL_FRONT : GL_BACK);
    } else {
        bi->gl.Disable(GL_CULL_FACE);
    }

    // Blending
    if (flags & BADGPUDrawFlags_Blend) {
        bi->gl.Enable(GL_BLEND);
        bi->gl.BlendFuncSeparate(
            convertBlendWeight(BADGPU_BP_RGBS(blendProgram)),
            convertBlendWeight(BADGPU_BP_RGBD(blendProgram)),
            convertBlendWeight(BADGPU_BP_AS(blendProgram)),
            convertBlendWeight(BADGPU_BP_AD(blendProgram)));
        bi->gl.BlendEquationSeparate(
            convertBlendOp(BADGPU_BP_RGBE(blendProgram)),
            convertBlendOp(BADGPU_BP_AE(blendProgram)));
    } else {
        bi->gl.Disable(GL_BLEND);
    }

    // Vertex Loader
    if (pType == BADGPUPrimitiveType_Points) {
        bi->gl.PointSize(plSize);
    } else if (pType == BADGPUPrimitiveType_Lines) {
        bi->gl.LineWidth(plSize);
    }

    bi->gl.EnableClientState(GL_VERTEX_ARRAY);
    bi->gl.VertexPointer(vPosD, GL_FLOAT, 0, vPos);
    if (vCol) {
        if (flags & BADGPUDrawFlags_FreezeColour) {
            bi->gl.DisableClientState(GL_COLOR_ARRAY);
            bi->gl.Color4f(vCol[0], vCol[1], vCol[2], vCol[3]);
        } else {
            bi->gl.EnableClientState(GL_COLOR_ARRAY);
            bi->gl.ColorPointer(4, GL_FLOAT, 0, vCol);
        }
    } else {
        bi->gl.DisableClientState(GL_COLOR_ARRAY);
        bi->gl.Color4f(1, 1, 1, 1);
    }
    if (vTC) {
        if (flags & BADGPUDrawFlags_FreezeTC) {
            bi->gl.DisableClientState(GL_TEXTURE_COORD_ARRAY);
            if (vTCD == 4) {
                bi->gl.MultiTexCoord4f(GL_TEXTURE0, vTC[0], vTC[1], vTC[2], vTC[3]);
            } else if (vTCD == 3) {
                bi->gl.MultiTexCoord4f(GL_TEXTURE0, vTC[0], vTC[1], vTC[2], 1.0f);
            } else {
                bi->gl.MultiTexCoord4f(GL_TEXTURE0, vTC[0], vTC[1], 0.0f, 1.0f);
            }
        } else {
            bi->gl.EnableClientState(GL_TEXTURE_COORD_ARRAY);
            bi->gl.TexCoordPointer(vTCD, GL_FLOAT, 0, vTC);
        }
    } else {
        bi->gl.DisableClientState(GL_TEXTURE_COORD_ARRAY);
        bi->gl.MultiTexCoord4f(GL_TEXTURE0, 0, 0, 0, 1);
    }

    // Actual Draw
    if (indices) {
        bi->gl.DrawElements(pType, iCount, GL_UNSIGNED_SHORT, indices + iStart);
    } else {
        bi->gl.DrawArrays(pType, iStart, iCount);
    }
    return badgpuChk(bi, "badgpuDrawGeom", 0);
}

static BADGPUBool bglResetGLState(BADGPUInstancePriv * instance) {
    BADGPUInstanceGL * bi = BG_INSTANCE_GL(instance);

    bi->gl.BindFramebuffer(GL_FRAMEBUFFER, 0);
    bi->gl.ColorMask(1, 1, 1, 1);
    bi->gl.StencilMask(-1);
    bi->gl.DepthMask(1);
    bi->gl.Disable(GL_SCISSOR_TEST);

    bi->gl.MatrixMode(GL_PROJECTION);
    bi->gl.LoadIdentity();
    bi->gl.MatrixMode(GL_TEXTURE);
    bi->gl.LoadIdentity();
    bi->gl.MatrixMode(GL_MODELVIEW);
    bi->gl.LoadIdentity();

    bi->gl.Disable(GL_TEXTURE_2D);
    bi->gl.BindTexture(GL_TEXTURE_2D, 0);

    bi->gl.Disable(GL_CLIP_PLANE0);
    bi->gl.Disable(GL_ALPHA_TEST);

    if (bi->gl.DepthRangef) {
        bi->gl.DepthRangef(0, 1);
    } else {
        bi->gl.DepthRange(0, 1);
    }

    bi->gl.Disable(GL_POLYGON_OFFSET_FILL);
    bi->gl.Disable(GL_STENCIL_TEST);
    bi->gl.Disable(GL_DEPTH_TEST);

    bi->gl.FrontFace(GL_CCW);
    bi->gl.Disable(GL_CULL_FACE);

    bi->gl.Disable(GL_BLEND);

    bi->gl.DisableClientState(GL_VERTEX_ARRAY);
    bi->gl.DisableClientState(GL_COLOR_ARRAY);
    bi->gl.Color4f(1, 1, 1, 1);
    bi->gl.DisableClientState(GL_TEXTURE_COORD_ARRAY);
    bi->gl.MultiTexCoord4f(GL_TEXTURE0, 0, 0, 0, 1);

    return badgpuChk(bi, "badgpuResetGLState", 0);
}

static BADGPUTexture bglNewTextureFromGL(BADGPUInstancePriv * bi2, uint32_t glTex) {
    BADGPUInstanceGL * bi = BG_INSTANCE_GL(bi2);

    BADGPUTexturePriv * tex = malloc(sizeof(BADGPUTexturePriv));
    if (!tex) {
        badgpuErr(BG_INSTANCE(bi), "badgpuNewTextureFromGL: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroyTexture);

    tex->i = BG_INSTANCE(badgpuRef((BADGPUObject) bi2));
    tex->autoDel = 0;
    tex->glTex = glTex;
    return (BADGPUTexture) tex;
}

static KHRABI void badgpuDebugCB(int32_t a, int32_t b, int32_t c, int32_t d, int32_t len, const char * text, const void * g) {
    printf("BADGPU: GLDebug: %s\n", text);
}

BADGPU_EXPORT BADGPUInstance badgpuNewInstanceWithWSI(uint32_t flags, const char ** error, BADGPUWSIContext wsi) {
    BADGPUInstanceGL * bi = malloc(sizeof(BADGPUInstanceGL));
    if (!bi) {
        if (error)
            *error = "Failed to allocate BADGPUInstance.";
        wsi->close(wsi);
        return NULL;
    }
    memset(bi, 0, sizeof(BADGPUInstanceGL));
    bi->base.ctx = wsi;
    bi->base.backendCheck = (flags & BADGPUNewInstanceFlags_BackendCheck) != 0;
    bi->base.backendCheckAggressive = (flags & BADGPUNewInstanceFlags_BackendCheckAggressive) != 0;
    bi->base.canPrintf = (flags & BADGPUNewInstanceFlags_CanPrintf) != 0;
    badgpu_initObj((BADGPUObject) bi, destroyGLInstance);
    // vtbl
    bi->base.bind = bglBindInstance;
    bi->base.unbind = bglUnbindInstance;
    bi->base.flush = bglFlushInstance;
    bi->base.finish = bglFinishInstance;
    bi->base.resetGLState = bglResetGLState;
    bi->base.newTextureFromGL = bglNewTextureFromGL;
    // --
    bi->base.getMetaInfo = bglGetMetaInfo;
    bi->base.texLoadFormat = BADGPUTextureLoadFormat_RGBA8888;
    bi->base.newTexture = bglNewTexture;
    bi->base.newDSBuffer = bglNewDSBuffer;
    bi->base.generateMipmap = bglGenerateMipmap;
    bi->base.readPixelsRGBA8888 = bglReadPixelsRGBA8888;
    bi->base.drawClear = bglDrawClear;
    bi->base.drawGeomBackend = bi->base.drawGeomFrontend = bglDrawGeom;
    bi->base.drawPointBackend = bi->base.drawPointFrontend = badgpu_swtnl_harnessDrawPoint;
    bi->base.drawLineBackend = bi->base.drawLineFrontend = badgpu_swtnl_harnessDrawLine;
    bi->base.drawTriangleBackend = bi->base.drawTriangleFrontend = badgpu_swtnl_harnessDrawTriangle;

    if (badgpu_getEnvFlag("BADGPU_DEBUG_SWTNL")) {
        bi->base.drawGeomFrontend = badgpu_swtnl_drawGeom;
        if (badgpu_getEnvFlag("BADGPU_DEBUG_SWCLIP")) {
            bi->base.drawPointFrontend = badgpu_swclip_drawPoint;
            bi->base.drawLineFrontend = badgpu_swclip_drawLine;
            bi->base.drawTriangleFrontend = badgpu_swclip_drawTriangle;
        }
    }

    // determine context type stuff
    int desktopExt = 0;
    switch ((BADGPUContextType) (int) (intptr_t) wsi->getValue(wsi, BADGPUWSIQuery_ContextType)) {
    case BADGPUContextType_GLESv1:
        break;
    case BADGPUContextType_GL:
        desktopExt = 1;
        break;
    default:
        if (error)
            *error = "BadGPU does not support the given context type";
        wsi->close(wsi);
        free(bi);
        return NULL;
    }
    // Initial bind
    if (!wsi->makeCurrent(wsi)) {
        if (error)
            *error = "Failed to initially bind instance";
        wsi->close(wsi);
        free(bi);
        return NULL;
    }
    bi->base.isBound = 1;
    const char * failedFn = badgpu_glBind(wsi, &bi->gl, desktopExt);
    if (failedFn) {
        wsi->close(wsi);
        if (error)
            *error = failedFn;
        free(bi);
        return NULL;
    }

    const char * ext = bi->gl.GetString(GL_EXTENSIONS);
    if (bi->base.canPrintf) {
        if (ext) {
            printf("BADGPU: GL Extensions: %s\n", ext);
        } else {
            printf("BADGPU: GL Extensions not available!\n");
        }
    }
    if (bi->base.backendCheck && bi->base.canPrintf && ext) {
        const char * exCheck = strstr(ext, "GL_KHR_debug");
        if (exCheck && ((exCheck[12] == 0) || (exCheck[12] == ' '))) {
            printf("BADGPU: KHR_debug detected, testing...\n");
            bi->gl.Enable(GL_DEBUG_OUTPUT);
            void (KHRABI *glDebugMessageControl)(int32_t, int32_t, int32_t, int32_t, const int32_t *, int32_t) = wsi->getProcAddress(wsi, "glDebugMessageControl");
            if (glDebugMessageControl)
                glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, NULL, 1);
            void (KHRABI *glDebugMessageCallback)(void *, const void *) = wsi->getProcAddress(wsi, "glDebugMessageCallback");
            if (glDebugMessageCallback)
                glDebugMessageCallback(badgpuDebugCB, NULL);
            void (KHRABI *glDebugMessageInsert)(int32_t, int32_t, int32_t, int32_t, int32_t, const char *) = wsi->getProcAddress(wsi, "glDebugMessageInsert");
            if (glDebugMessageInsert)
                glDebugMessageInsert(GL_DEBUG_SOURCE_THIRD_PARTY, GL_DEBUG_TYPE_OTHER, 0, GL_DEBUG_SEVERITY_NOTIFICATION, -1, "BADGPU GL Debug Test Message");
        }
    }
    bi->gl.GenFramebuffers(1, &bi->fbo);
    bi->gl.BindFramebuffer(GL_FRAMEBUFFER, bi->fbo);
    // Not yet setup, so fboBoundTex/fboBoundDS being 0 is correct
    if (!badgpuChk(bi, "badgpuNewInstance", 1)) {
        badgpuUnref((BADGPUInstance) bi);
        if (error)
            *error = "Initial GL resource setup returned an error.";
        return NULL;
    }
    return (BADGPUInstance) bi;
}
