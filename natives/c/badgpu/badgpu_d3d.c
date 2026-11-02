/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * BadGPU D3D (for XP)
 */

#include "badgpu.h"
#include "badgpu_internal.h"
#include "badgpu_sw.h"
#include <d3d9.h>
#include <d3d9caps.h>
#include <d3d9types.h>
#include <windows.h>

typedef struct BADGPUInstanceD3D {
    BADGPUInstancePriv base;
    IDirect3D9 * d3d;
    IDirect3DSurface9 * sacrifice;
    IDirect3DDevice9 * d3ddev;
    HWND hwnd;
} BADGPUInstanceD3D;
#define BG_INSTANCE_D3D(x) ((BADGPUInstanceD3D *) (x))

static inline DWORD checkD3DErr(BADGPUInstanceD3D * bi, DWORD res) {
    if (res)
        if (bi->base.canPrintf)
            printf("BADGPU: D3D error %08x\n", res);
    return res;
}

typedef struct BADGPUTextureD3D {
    BADGPUTexturePriv base;
    int w, h;
    float txMulX, txMulY;
    IDirect3DSurface9 * surface;
    IDirect3DSurface9 * surfaceBackup;
    // due to an odd rule that all render targets must have depth
    IDirect3DSurface9 * surfaceZ;
} BADGPUTextureD3D;
#define BG_TEXTURE_D3D(x) ((BADGPUTextureD3D *) (x))

typedef struct BADGPUDSBufferD3D {
    BADGPUDSBufferPriv base;
    int w, h;
    IDirect3DSurface9 * surface;
} BADGPUDSBufferD3D;
#define BG_DSBUFFER_D3D(x) ((BADGPUDSBufferD3D *) (x))

static void destroyD3DInstance(BADGPUObject obj) {
    BADGPUInstanceD3D * i = BG_INSTANCE_D3D(obj);
    if (i->sacrifice)
        IDirect3DSurface9_Release(i->sacrifice);
    if (i->d3ddev)
        IDirect3DDevice9_Release(i->d3ddev);
    if (i->hwnd)
        DestroyWindow(i->hwnd);
    if (i->d3d)
        IDirect3D9_Release(i->d3d);
    free(obj);
}
static void destroyD3DTexture(BADGPUObject obj) {
    BADGPUTextureD3D * tex = BG_TEXTURE_D3D(obj);
    if (tex->surface)
        IDirect3DSurface9_Release(tex->surface);
    if (tex->surfaceBackup)
        IDirect3DSurface9_Release(tex->surfaceBackup);
    if (tex->surfaceZ)
        IDirect3DSurface9_Release(tex->surfaceZ);
    badgpuUnref((BADGPUObject) tex->base.i);
    free(obj);
}
static void destroyD3DDSBuffer(BADGPUObject obj) {
    BADGPUDSBufferD3D * tex = BG_DSBUFFER_D3D(obj);
    if (tex->surface)
        IDirect3DSurface9_Release(tex->surface);
    badgpuUnref((BADGPUObject) tex->base.i);
    free(obj);
}

static const char * bd3dGetMetaInfo(struct BADGPUInstancePriv * instance, BADGPUMetaInfoType mi) {
    BADGPUInstanceD3D * i = BG_INSTANCE_D3D(instance);
    if (mi == BADGPUMetaInfoType_Vendor)
        return "BadGPU on D3D9";
    if (mi == BADGPUMetaInfoType_Renderer)
        return "?r";
    if (mi == BADGPUMetaInfoType_Version)
        return "?v";
    return NULL;
}

static IDirect3DSurface9 * createARGB32ISurface(BADGPUInstanceD3D * bi, int width, int height) {
    IDirect3DSurface9 * surf = NULL;
    if (checkD3DErr(bi, IDirect3DDevice9_CreateRenderTarget(bi->d3ddev, width, height, D3DFMT_A8B8G8R8, D3DMULTISAMPLE_NONE, 0, TRUE, &surf, NULL)))
        return NULL;
    return surf;
}

static int coercePOT(int v) {
    int i = 1;
    while (i < v && i < 32768)
        i *= 2;
    return i;
}

static BADGPUTexture bd3dNewTexture(struct BADGPUInstancePriv * instance, int16_t width, int16_t height, const void * data) {
    BADGPUInstanceD3D * bi = BG_INSTANCE_D3D(instance);

    // We need to coerce width/height into POT.
    // Wine will let us get away with not doing this, but Windows won't.
    int coercedW = coercePOT(width), coercedH = coercePOT(height);
    float txMulX = width / (float) coercedW;
    float txMulY = height / (float) coercedH;
    IDirect3DSurface9 * surf = createARGB32ISurface(bi, coercedW, coercedH);
    if (!surf) {
        badgpuErr(instance, "badgpuNewTexture: D3D: Unable to allocate surface.");
        return NULL;
    }
    IDirect3DSurface9 * surfBackup = createARGB32ISurface(bi, coercedW, coercedH);
    if (!surfBackup) {
        IDirect3DSurface9_Release(surf);
        badgpuErr(instance, "badgpuNewTexture: D3D: Unable to allocate surface backup.");
        return NULL;
    }

    BADGPUTextureD3D * tex = malloc(sizeof(BADGPUTextureD3D));
    if (!tex) {
        IDirect3DSurface9_Release(surf);
        IDirect3DSurface9_Release(surfBackup);
        badgpuErr(instance, "badgpuNewTexture: D3D: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroyD3DTexture);

    tex->w = width;
    tex->h = height;
    tex->txMulX = txMulX;
    tex->txMulY = txMulY;
    tex->surface = surf;
    tex->surfaceBackup = surfBackup;
    tex->surfaceZ = NULL;
    tex->base.i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));

    if (data) {
        int i;
        D3DLOCKED_RECT lockRect = {};
        if (checkD3DErr(bi, IDirect3DSurface9_LockRect(tex->surface, &lockRect, NULL, 0))) {
            badgpuErr(instance, "badgpuNewTexture: D3D: Failed to lock surface");
            destroyD3DTexture((BADGPUObject) tex);
            return NULL;
        }

        for (i = 0; i < height; i++) {
            void * texdata = lockRect.pBits + (i * lockRect.Pitch);
            memcpy(texdata, data, width * sizeof(uint32_t));
            data += width * sizeof(uint32_t);
        }

        IDirect3DSurface9_UnlockRect(tex->surface);
    }

    return (BADGPUTexture) tex;
}

static BADGPUDSBuffer bd3dNewDSBuffer(struct BADGPUInstancePriv * instance, int16_t width, int16_t height) {
    BADGPUDSBufferD3D * tex = malloc(sizeof(BADGPUDSBufferD3D));
    if (!tex) {
        badgpuErr(instance, "badgpuNewDSBuffer: D3D: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroyD3DDSBuffer);

    tex->w = width;
    tex->h = height;
    tex->surface = NULL;
    tex->base.i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));

    return (BADGPUDSBuffer) tex;
}

static BADGPUBool bd3dGenerateMipmap(void * texture) {
    return 0;
}

static BADGPUBool bd3dReadPixelsARGBI32(void * texture, uint16_t x, uint16_t y, int16_t width, int16_t height, uint32_t * data) {
    BADGPUTextureD3D * tex = BG_TEXTURE_D3D(texture);
    int w = width;
    int h = height;
    int i;
    if (x + w > tex->w || y + h > tex->h)
        return badgpuErr(tex->base.i, "badgpuReadPixels: D3D: Read out of range");

    RECT makeRect = {
        .left = x,
        .top = y,
        .right = x + w,
        .bottom = y + h,
    };
    D3DLOCKED_RECT lockRect = {};
    if (checkD3DErr(BG_INSTANCE_D3D(tex->base.i), IDirect3DSurface9_LockRect(tex->surface, &lockRect, &makeRect, D3DLOCK_READONLY)))
        return badgpuErr(tex->base.i, "badgpuReadPixels: D3D: Failed to lock surface");

    for (i = 0; i < height; i++) {
        void * texdata = lockRect.pBits + (i * lockRect.Pitch);
        memcpy(data, texdata, width * sizeof(uint32_t));
        data += width;
    }

    IDirect3DSurface9_UnlockRect(tex->surface);
    return 1;
}

// -- renderfuncs --

static void bd3dChopRect(RECT * a, const RECT * b) {
    a->left = a->left < b->left ? b->left : a->left;
    a->top = a->top < b->top ? b->top : a->top;
    a->right = a->right > b->right ? b->right : a->right;
    a->bottom = a->bottom > b->bottom ? b->bottom : a->bottom;
}

static RECT bd3dAdjustedScissor(int w, int h, uint32_t sFlags, int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight) {
    RECT screen = {0, 0, w, h};
    if (sFlags & BADGPUSessionFlags_Scissor) {
        RECT scissor = {sScX, sScY, sScX + sScWidth, sScY + sScHeight};
        bd3dChopRect(&screen, &scissor);
    }
    return screen;
}

static BADGPUBool bd3dDrawClear(
    struct BADGPUInstancePriv * instance,
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
) {
    if (sTexture && (sFlags & BADGPUSessionFlags_MaskRGBA)) {
        BADGPUTextureD3D * tex = BG_TEXTURE_D3D(sTexture);
        RECT target = bd3dAdjustedScissor(tex->w, tex->h, sFlags, sScX, sScY, sScWidth, sScHeight);
        /*
        DDBLTFX bltfx = {
            .dwSize = sizeof(DDBLTFX),
            .dwFillColor = badgpu_sw_v42p(badgpu_vec4(cR, cG, cB, cA))
        };
        IDirect3DSurface9_Blt(tex->surface, &target, NULL, NULL, DDBLT_COLORFILL | DDBLT_WAIT, &bltfx);
        */
    }
    return 1;
}

typedef struct {
    float x, y, z;
    DWORD diffuse;
    float uv[4];
} BADGPUD3DLITVERTEX;

static D3DMATRIX bd3dTranslateMatrix(const BADGPUMatrix * srcMtx) {
    if (!srcMtx) {
        D3DMATRIX mtx = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
        };
        return mtx;
    } else {
        /*
        D3DMATRIX mtx = {
            srcMtx->x.x, srcMtx->y.x, srcMtx->z.x, srcMtx->w.x,
            srcMtx->x.y, srcMtx->y.y, srcMtx->z.y, srcMtx->w.y,
            srcMtx->x.z, srcMtx->y.z, srcMtx->z.z, srcMtx->w.z,
            srcMtx->x.w, srcMtx->y.w, srcMtx->z.w, srcMtx->w.w,
        };
        */
        D3DMATRIX mtx = {
            srcMtx->x.x, srcMtx->x.y, srcMtx->x.z, srcMtx->x.w,
            srcMtx->y.x, srcMtx->y.y, srcMtx->y.z, srcMtx->y.w,
            srcMtx->z.x, srcMtx->z.y, srcMtx->z.z, srcMtx->z.w,
            srcMtx->w.x, srcMtx->w.y, srcMtx->w.z, srcMtx->w.w,
        };
        return mtx;
    }
}

static BADGPUBool bd3dDrawGeom(
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
    /*
    // HACK: make badgpu_test work before we have real stencil support
    if ((flags & BADGPUDrawFlags_StencilTest) && (stFunc != BADGPUCompare_Always))
        return 1;

    BADGPUD3DLITVERTEX * vertexConv = malloc(sizeof(BADGPUD3DLITVERTEX) * iCount);
    for (int i = 0; i < iCount; i++) {
        int vertexIdx = indices ? indices[iStart + i] : iStart + i;

        const float * vPosY = vPos + (vertexIdx * vPosD);
        vertexConv[i].x = vPosY[0];
        vertexConv[i].y = vPosY[1];
        vertexConv[i].z = vPosD >= 3 ? vPosY[2] : 0;

        vertexConv[i].uv[0] = 0;
        vertexConv[i].uv[1] = 0;
        vertexConv[i].uv[2] = 0;
        vertexConv[i].uv[3] = 1;

        if (vTC) {
            const float * vTCY = vTC + ((flags & BADGPUDrawFlags_FreezeTC) ? 0 : (vertexIdx * vTCD));
            for (int j = 0; j < vTCD; j++)
                vertexConv[i].uv[j] = vTCY[j];
        }

        if (vCol) {
            const float * vColY = vCol + ((flags & BADGPUDrawFlags_FreezeColour) ? 0 : (vertexIdx * 4));
            vertexConv[i].diffuse = badgpu_sw_v42p(badgpu_vec4(vColY[0], vColY[1], vColY[2], vColY[3]));
        } else {
            vertexConv[i].diffuse = 0xFFFFFFFF;
        }
    }

    IDirectDrawSurface7 * rtSurface;
    IDirectDrawSurface7 * rtSurfaceBackup;
    int rtWidth, rtHeight;

    // Figure out how we're rendering
    if (sTexture) {
        BADGPUTextureD3D * sTextureI = BG_TEXTURE_D3D(sTexture);
        rtSurface = sTextureI->surface;
        rtSurfaceBackup = sTextureI->surfaceBackup;
        rtWidth = sTextureI->w;
        rtHeight = sTextureI->h;
    }

    // To render anything, we need an RT
    if (!rtSurface)
        return 1;

    BADGPUInstanceD3D * i = BG_INSTANCE_D3D(instance);

    RECT scissor, *scissorBackup = NULL;

    {
        RECT viewport = { vX, vY, vX + vW, vY + vH };

        scissor = bd3dAdjustedScissor(rtWidth, rtHeight, sFlags, sScX, sScY, sScWidth, sScHeight);
        bd3dChopRect(&scissor, &viewport);

        // Do we need to perform the scissor backup process? :(
        if (scissor.left != viewport.left || scissor.right != viewport.right || scissor.top != viewport.top || scissor.bottom != viewport.bottom) {
            POINT copyDst = {viewport.left, viewport.top};
            // Answer: Yes, we do. Get it ready...
            scissorBackup = &scissor;
            // See the other call post-drawcall.
            IDirect3DDevice9_Load(i->d3ddev, rtSurfaceBackup, &copyDst, rtSurface, &viewport, 0);
            IDirect3DDevice9_SetRenderTarget(i->d3ddev, rtSurfaceBackup, 0);
        } else {
            IDirect3DDevice9_SetRenderTarget(i->d3ddev, rtSurface, 0);
        }

        D3DVIEWPORT7 vp = {
            .dwX = viewport.left,
            .dwY = viewport.top,
            .dwWidth = viewport.right - viewport.left,
            .dwHeight = viewport.bottom - viewport.top,
            // these should really be based on DepthRange
            .dvMinZ = 0.0,
            .dvMaxZ = 1.0
        };
        IDirect3DDevice9_SetViewport(i->d3ddev, &vp);
        // nudge values to fix rasterization
        float epsilonX = 1.0f / vp.dwWidth;
        float epsilonY = 1.0f / vp.dwHeight;
        D3DMATRIX adj = {
            1, 0, 0, 0,
            0, -1, 0, 0,
            0, 0, 1, 0,
            -epsilonX, epsilonY, 0, 1,
        };
        IDirect3DDevice9_SetTransform(i->d3ddev, D3DTRANSFORMSTATE_PROJECTION, &adj);
    }

    D3DMATRIX viewMatrix = bd3dTranslateMatrix(mvMatrix);
    IDirect3DDevice9_SetTransform(i->d3ddev, D3DTRANSFORMSTATE_VIEW, &viewMatrix);

    if (flags & BADGPUDrawFlags_Blend) {
        IDirect3DDevice9_SetRenderState(i->d3ddev, D3DRENDERSTATE_ALPHABLENDENABLE, TRUE);
        D3DBLEND srcBlend = D3DBLEND_ONE;
        D3DBLEND dstBlend = D3DBLEND_ZERO;
        if (BADGPU_BP_RGBS(blendProgram) == BADGPUBlendWeight_SrcA)
            srcBlend = D3DBLEND_SRCALPHA;
        if (BADGPU_BP_RGBD(blendProgram) == BADGPUBlendWeight_InvertSrcA)
            dstBlend = D3DBLEND_INVSRCALPHA;
        IDirect3DDevice9_SetRenderState(i->d3ddev, D3DRENDERSTATE_SRCBLEND, srcBlend);
        IDirect3DDevice9_SetRenderState(i->d3ddev, D3DRENDERSTATE_DESTBLEND, dstBlend);
    } else {
        IDirect3DDevice9_SetRenderState(i->d3ddev, D3DRENDERSTATE_ALPHABLENDENABLE, FALSE);
    }

    IDirect3DDevice9_SetRenderState(i->d3ddev, D3DRENDERSTATE_LIGHTING, FALSE);
    IDirect3DDevice9_SetRenderState(i->d3ddev, D3DRENDERSTATE_CULLMODE, D3DCULL_NONE);

    if (texture) {
        BADGPUTextureD3D * textureI = BG_TEXTURE_D3D(texture);
        IDirect3DDevice9_SetTexture(i->d3ddev, 0, textureI->surface);

        D3DMATRIX mtx = bd3dTranslateMatrix(matrixT);
#if 0
        // Invert Y
        //   IO
        mtx._12 *= -1;
        mtx._22 *= -1;
        mtx._32 *= -1;
        mtx._42 *= -1;
        // and offset
        mtx._42 += 1;
#endif
        mtx._11 *= textureI->txMulX;
        mtx._21 *= textureI->txMulX;
        mtx._31 *= textureI->txMulX;
        mtx._41 *= textureI->txMulX;
        mtx._12 *= textureI->txMulY;
        mtx._22 *= textureI->txMulY;
        mtx._32 *= textureI->txMulY;
        mtx._42 *= textureI->txMulY;
        IDirect3DDevice9_SetTransform(i->d3ddev, D3DTRANSFORMSTATE_TEXTURE0, &mtx);
        IDirect3DDevice9_SetTextureStageState(i->d3ddev, 0, D3DTSS_TEXTURETRANSFORMFLAGS, D3DTTFF_COUNT2);

        IDirect3DDevice9_SetTextureStageState(i->d3ddev, 0, D3DTSS_MAGFILTER, (flags & BADGPUDrawFlags_MagLinear) ? D3DTFG_LINEAR : D3DTFG_POINT);
        IDirect3DDevice9_SetTextureStageState(i->d3ddev, 0, D3DTSS_MINFILTER, (flags & BADGPUDrawFlags_MinLinear) ? D3DTFG_LINEAR : D3DTFG_POINT);
#if 1
        IDirect3DDevice9_SetTextureStageState(i->d3ddev, 0, D3DTSS_ADDRESSU, (flags & BADGPUDrawFlags_WrapS) ? D3DTADDRESS_WRAP : D3DTADDRESS_CLAMP);
        IDirect3DDevice9_SetTextureStageState(i->d3ddev, 0, D3DTSS_ADDRESSV, (flags & BADGPUDrawFlags_WrapT) ? D3DTADDRESS_WRAP : D3DTADDRESS_CLAMP);
#else
        // forcing wrapping helps debug some issues
        IDirect3DDevice9_SetTextureStageState(i->d3ddev, 0, D3DTSS_ADDRESSU, D3DTADDRESS_WRAP);
        IDirect3DDevice9_SetTextureStageState(i->d3ddev, 0, D3DTSS_ADDRESSV, D3DTADDRESS_WRAP);
#endif
    }

    IDirect3DDevice9_BeginScene(i->d3ddev);

    D3DPRIMITIVETYPE pt = D3DPT_TRIANGLELIST;
    if (pType == BADGPUPrimitiveType_Lines)
        pt = D3DPT_LINELIST;
    else if (pType == BADGPUPrimitiveType_Points)
        pt = D3DPT_POINTLIST;

    D3DDRAWPRIMITIVESTRIDEDDATA stride = {};
    stride.position.dwStride = sizeof(BADGPUD3DLITVERTEX);
    stride.position.lpvData = &vertexConv->x;
    stride.diffuse.dwStride = sizeof(BADGPUD3DLITVERTEX);
    stride.diffuse.lpvData = &vertexConv->diffuse;
    stride.textureCoords[0].dwStride = sizeof(BADGPUD3DLITVERTEX);
    stride.textureCoords[0].lpvData = vertexConv->uv;

    DWORD vfFlags = D3DFVF_XYZ | D3DFVF_DIFFUSE;
    if (texture)
        vfFlags |= D3DFVF_TEX1 | D3DFVF_TEXCOORDSIZE4(0);
    IDirect3DDevice9_DrawPrimitiveStrided(i->d3ddev, pt, vfFlags, &stride, iCount, 0);

    IDirect3DDevice9_EndScene(i->d3ddev);

    IDirect3DDevice9_SetTexture(i->d3ddev, 0, NULL);
    IDirect3DDevice9_SetRenderTarget(i->d3ddev, i->sacrifice, 0);

    if (scissorBackup) {
        // Rendering was *really* done on the backup surface. Retrieve the appropriately clipped results.
        POINT copyDst = {scissorBackup->left, scissorBackup->top};
        IDirect3DDevice9_Load(i->d3ddev, rtSurface, &copyDst, rtSurfaceBackup, scissorBackup, 0);
    }
    */
    return 1;
}

// -- the instance --

#define INST_ERROR(text) { if (error) *error = text; if (bi) destroyD3DInstance((BADGPUObject) bi); return NULL; }
BADGPUInstance badgpu_newD3DInstance(BADGPUNewInstanceFlags flags, const char ** error) {
    BADGPUInstanceD3D * bi = malloc(sizeof(BADGPUInstanceD3D));

    if (!bi)
        INST_ERROR("D3D: Failed to allocate BADGPUInstance.");

    memset(bi, 0, sizeof(BADGPUInstanceD3D));
    bi->base.backendCheck = (flags & BADGPUNewInstanceFlags_BackendCheck) != 0;
    bi->base.backendCheckAggressive = (flags & BADGPUNewInstanceFlags_BackendCheckAggressive) != 0;
    bi->base.canPrintf = (flags & BADGPUNewInstanceFlags_CanPrintf) != 0;
    bi->base.isBound = 1;
    badgpu_initObj((BADGPUObject) bi, destroyD3DInstance);
    // vtbl
    bi->base.getMetaInfo = bd3dGetMetaInfo;
    bi->base.texLoadFormat = BADGPUTextureLoadFormat_ARGBI32;
    bi->base.newTexture = bd3dNewTexture;
    bi->base.newDSBuffer = bd3dNewDSBuffer;
    bi->base.generateMipmap = bd3dGenerateMipmap;
    bi->base.readPixelsARGBI32 = bd3dReadPixelsARGBI32;
    bi->base.drawClear = bd3dDrawClear;
    bi->base.drawGeomBackend = bi->base.drawGeomFrontend = bd3dDrawGeom;
    badgpu_swtnl_harnessSetup(&bi->base);

    bi->d3d = Direct3DCreate9(D3D_SDK_VERSION);
    if (!bi->d3d)
        INST_ERROR("D3D: Failed to create D3D9 instance.");

    WNDCLASS wc = {
        .lpfnWndProc = DefWindowProcA,
        .hInstance = NULL,
        .hbrBackground = (HBRUSH) GetStockObject (WHITE_BRUSH),
        .lpszClassName = "gabien_una_d3d_window",
        .style = CS_HREDRAW | CS_VREDRAW
    };
    RegisterClass(&wc);
    bi->hwnd = CreateWindowA("gabien_una_d3d_window", "una", WS_OVERLAPPEDWINDOW, 0, 0, 0, 0, NULL, NULL, NULL, NULL);
    if (!bi->hwnd)
        INST_ERROR("D3D: Failed to create D3D9 window.");

    D3DPRESENT_PARAMETERS present = {
        .Windowed = TRUE,
        .BackBufferFormat = D3DFMT_UNKNOWN,
        .BackBufferWidth = 1,
        .BackBufferHeight = 1,
        .SwapEffect = D3DSWAPEFFECT_DISCARD,
    };

    DWORD behaviourFlags = D3DCREATE_FPU_PRESERVE | D3DCREATE_SOFTWARE_VERTEXPROCESSING;
    if (checkD3DErr(bi, IDirect3D9_CreateDevice(bi->d3d, D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, bi->hwnd, behaviourFlags, &present, &bi->d3ddev)))
        if (checkD3DErr(bi, IDirect3D9_CreateDevice(bi->d3d, D3DADAPTER_DEFAULT, D3DDEVTYPE_REF, bi->hwnd, behaviourFlags, &present, &bi->d3ddev)))
            INST_ERROR("D3D: Failed to create device.");

    bi->sacrifice = createARGB32ISurface(bi, 8, 8);
    if (!bi->sacrifice)
        INST_ERROR("D3D: Failed to create sacrificial RT.");

    return (BADGPUInstance) bi;
}
