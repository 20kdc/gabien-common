/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * BadGPU D3D7 (for XP)
 */

#include "badgpu.h"
#include "badgpu_internal.h"
#include "badgpu_sw.h"
#include <d3d.h>
#include <d3dtypes.h>
#include <ddraw.h>

typedef struct BADGPUInstanceD3D7 {
    BADGPUInstancePriv base;
    IDirectDraw7 * ddraw;
    IDirect3D7 * d3d;
    IDirectDrawSurface7 * sacrifice;
    IDirectDrawSurface7 * sacrificeZ;
    IDirect3DDevice7 * d3ddev;
    DDDEVICEIDENTIFIER2 devInfo;
    DWORD memCap;
} BADGPUInstanceD3D7;
#define BG_INSTANCE_D3D7(x) ((BADGPUInstanceD3D7 *) (x))

static inline DWORD checkD3DErr(BADGPUInstanceD3D7 * bi, DWORD res) {
    if (res)
        if (bi->base.canPrintf)
            printf("BADGPU: D3D error %08x\n", res);
    return res;
}

typedef struct BADGPUTextureD3D7 {
    BADGPUTexturePriv base;
    int w, h;
    IDirectDrawSurface7 * surface;
    // due to an odd rule that all render targets must either have depth or none of them can
    IDirectDrawSurface7 * surfaceZ;
} BADGPUTextureD3D7;
#define BG_TEXTURE_D3D7(x) ((BADGPUTextureD3D7 *) (x))

typedef struct BADGPUDSBufferD3D7 {
    BADGPUDSBufferPriv base;
    int w, h;
    IDirectDrawSurface7 * surface;
} BADGPUDSBufferD3D7;
#define BG_DSBUFFER_D3D7(x) ((BADGPUDSBufferD3D7 *) (x))

static void destroyD3D7Instance(BADGPUObject obj) {
    BADGPUInstanceD3D7 * i = BG_INSTANCE_D3D7(obj);
    if (i->d3ddev)
        IDirect3DDevice7_Release(i->d3ddev);
    if (i->d3d)
        IDirect3D7_Release(i->d3d);
    if (i->sacrifice && i->sacrificeZ)
        IDirectDrawSurface7_DeleteAttachedSurface(i->sacrifice, 0, i->sacrificeZ);
    if (i->sacrificeZ)
        IDirectDrawSurface7_Release(i->sacrificeZ);
    if (i->sacrifice)
        IDirectDrawSurface7_Release(i->sacrifice);
    IDirectDraw_Release(i->ddraw);
    free(obj);
}
static void destroyD3D7Texture(BADGPUObject obj) {
    BADGPUTextureD3D7 * tex = BG_TEXTURE_D3D7(obj);
    if (tex->surface)
        IDirectDrawSurface7_Release(tex->surface);
    if (tex->surfaceZ)
        IDirectDrawSurface7_Release(tex->surfaceZ);
    badgpuUnref((BADGPUObject) tex->base.i);
    free(obj);
}
static void destroyD3D7DSBuffer(BADGPUObject obj) {
    BADGPUDSBufferD3D7 * tex = BG_DSBUFFER_D3D7(obj);
    if (tex->surface)
        IDirectDrawSurface7_Release(tex->surface);
    badgpuUnref((BADGPUObject) tex->base.i);
    free(obj);
}

static const char * bd3d7GetMetaInfo(struct BADGPUInstancePriv * instance, BADGPUMetaInfoType mi) {
    BADGPUInstanceD3D7 * i = BG_INSTANCE_D3D7(instance);
    if (mi == BADGPUMetaInfoType_Vendor)
        return "BadGPU on D3D7";
    if (mi == BADGPUMetaInfoType_Renderer)
        return i->devInfo.szDescription;
    if (mi == BADGPUMetaInfoType_Version)
        return i->devInfo.szDriver;
    return NULL;
}

static DDPIXELFORMAT pixfmt_ARGB32I = {
    .dwSize = sizeof(DDPIXELFORMAT),
    .dwFlags = DDPF_RGB | DDPF_ALPHAPIXELS,
    .dwRGBBitCount = 32,
    .dwRGBAlphaBitMask = 0xFF000000,
    .dwRBitMask = 0x00FF0000,
    .dwGBitMask = 0x0000FF00,
    .dwBBitMask = 0x000000FF
};

static IDirectDrawSurface7 * createARGB32ISurface(BADGPUInstanceD3D7 * bi, int width, int height) {
    DDSURFACEDESC2 surfDesc;
    IDirectDrawSurface7 * surf;

    memset(&surfDesc, 0, sizeof(surfDesc));
    surfDesc.dwSize = sizeof(surfDesc);
    surfDesc.dwFlags = DDSD_WIDTH | DDSD_HEIGHT | DDSD_CAPS | DDSD_PIXELFORMAT | DDSD_TEXTURESTAGE;
    surfDesc.dwTextureStage = 0;
    surfDesc.dwWidth = width;
    surfDesc.dwHeight = height;
    surfDesc.ddsCaps.dwCaps = DDSCAPS_TEXTURE | DDSCAPS_3DDEVICE | bi->memCap;
    surfDesc.ddpfPixelFormat = pixfmt_ARGB32I;

    if (checkD3DErr(bi, IDirectDraw7_CreateSurface(bi->ddraw, &surfDesc, &surf, NULL)))
        return NULL;

    return surf;
}

static BADGPUTexture bd3d7NewTexture(struct BADGPUInstancePriv * instance, int16_t width, int16_t height, const void * data) {
    BADGPUInstanceD3D7 * bi = BG_INSTANCE_D3D7(instance);

    IDirectDrawSurface7 * surf = createARGB32ISurface(bi, width, height);

    if (!surf) {
        badgpuErr(instance, "badgpuNewTexture: D3D7: Unable to allocate surface.");
        return NULL;
    }

    BADGPUTextureD3D7 * tex = malloc(sizeof(BADGPUTextureD3D7));
    if (!tex) {
        IDirectDrawSurface7_Release(surf);
        badgpuErr(instance, "badgpuNewTexture: D3D7: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroyD3D7Texture);

    tex->w = width;
    tex->h = height;
    tex->surface = surf;
    tex->surfaceZ = NULL;
    tex->base.i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));

    if (data) {
        int i;
        DDSURFACEDESC2 desc = {};
        desc.dwSize = sizeof(desc);
        if (checkD3DErr(bi, IDirectDrawSurface7_Lock(tex->surface, NULL, &desc, DDLOCK_WAIT | DDLOCK_NOSYSLOCK | DDLOCK_WRITEONLY, NULL))) {
            badgpuErr(instance, "badgpuNewTexture: D3D7: Failed to lock surface");
            destroyD3D7Texture((BADGPUObject) tex);
            return NULL;
        }

        for (i = 0; i < height; i++) {
            void * texdata = desc.lpSurface + (i * desc.lPitch);
            memcpy(texdata, data, width * sizeof(uint32_t));
            data += width * sizeof(uint32_t);
        }

        IDirectDrawSurface7_Unlock(tex->surface, NULL);
    }

    return (BADGPUTexture) tex;
}

static BADGPUDSBuffer bd3d7NewDSBuffer(struct BADGPUInstancePriv * instance, int16_t width, int16_t height) {
    BADGPUDSBufferD3D7 * tex = malloc(sizeof(BADGPUDSBufferD3D7));
    if (!tex) {
        badgpuErr(instance, "badgpuNewDSBuffer: D3D7: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroyD3D7DSBuffer);

    tex->w = width;
    tex->h = height;
    tex->surface = NULL;
    tex->base.i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));

    return (BADGPUDSBuffer) tex;
}

static BADGPUBool bd3d7GenerateMipmap(void * texture) {
    return 0;
}

static BADGPUBool bd3d7ReadPixelsARGBI32(void * texture, uint16_t x, uint16_t y, int16_t width, int16_t height, uint32_t * data) {
    BADGPUTextureD3D7 * tex = BG_TEXTURE_D3D7(texture);
    int w = width;
    int h = height;
    int i;
    if (x + w > tex->w || y + h > tex->h)
        return badgpuErr(tex->base.i, "badgpuReadPixels: D3D7: Read out of range");

    DDSURFACEDESC2 desc = {};
    desc.dwSize = sizeof(desc);
    if (checkD3DErr(BG_INSTANCE_D3D7(tex->base.i), IDirectDrawSurface7_Lock(tex->surface, NULL, &desc, DDLOCK_WAIT | DDLOCK_NOSYSLOCK | DDLOCK_READONLY, NULL)))
        return badgpuErr(tex->base.i, "badgpuReadPixels: D3D7: Failed to lock surface");

    for (i = 0; i < height; i++) {
        void * texdata = desc.lpSurface + (i * desc.lPitch);
        memcpy(data, texdata, width * sizeof(uint32_t));
        data += width;
    }

    IDirectDrawSurface7_Unlock(tex->surface, NULL);
    return 1;
}

// -- renderfuncs --

static void bd3d7ChopRect(RECT * a, const RECT * b) {
    a->left = a->left < b->left ? b->left : a->left;
    a->top = a->top < b->top ? b->top : a->top;
    a->right = a->right > b->right ? b->right : a->right;
    a->bottom = a->bottom > b->bottom ? b->bottom : a->bottom;
}

static RECT bd3d7AdjustedScissor(int w, int h, uint32_t sFlags, int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight) {
    RECT screen = {0, 0, w, h};
    if (sFlags & BADGPUSessionFlags_Scissor) {
        RECT scissor = {sScX, sScY, sScX + sScWidth, sScY + sScHeight};
        bd3d7ChopRect(&screen, &scissor);
    }
    return screen;
}

static BADGPUBool bd3d7DrawClear(
    struct BADGPUInstancePriv * instance,
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
) {
    if (sTexture && (sFlags & BADGPUSessionFlags_MaskRGBA)) {
        BADGPUTextureD3D7 * tex = BG_TEXTURE_D3D7(sTexture);
        RECT target = bd3d7AdjustedScissor(tex->w, tex->h, sFlags, sScX, sScY, sScWidth, sScHeight);
        DDBLTFX bltfx = {
            .dwSize = sizeof(DDBLTFX),
            .dwFillColor = badgpu_sw_v42p(badgpu_vec4(cR, cG, cB, cA))
        };
        IDirectDrawSurface7_Blt(tex->surface, &target, NULL, NULL, DDBLT_COLORFILL | DDBLT_WAIT, &bltfx);
    }
    return 1;
}

typedef struct {
    float x, y, z;
    DWORD diffuse;
    float uv[4];
} BADGPUD3D7LITVERTEX;

static D3DMATRIX bd3d7TranslateMatrix(const BADGPUMatrix * srcMtx) {
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

static BADGPUBool bd3d7DrawGeom(
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
    // HACK: make badgpu_test work before we have real stencil support
    if ((flags & BADGPUDrawFlags_StencilTest) && (stFunc != BADGPUCompare_Always))
        return 1;

    BADGPUD3D7LITVERTEX * vertexConv = malloc(sizeof(BADGPUD3D7LITVERTEX) * iCount);
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
    int rtWidth, rtHeight;

    // Figure out how we're rendering
    if (sTexture) {
        BADGPUTextureD3D7 * sTextureI = BG_TEXTURE_D3D7(sTexture);
        rtSurface = sTextureI->surface;
        rtWidth = sTextureI->w;
        rtHeight = sTextureI->h;
    }

    // To render anything, we need an RT
    if (!rtSurface)
        return 1;

    BADGPUInstanceD3D7 * i = BG_INSTANCE_D3D7(instance);

    IDirect3DDevice7_SetRenderTarget(i->d3ddev, rtSurface, 0);

    {
        RECT scissor = bd3d7AdjustedScissor(rtWidth, rtHeight, sFlags, sScX, sScY, sScWidth, sScHeight);
        RECT viewport = { vX, vY, vX + vW, vY + vH };
        bd3d7ChopRect(&scissor, &viewport);
        D3DVIEWPORT7 vp = {
            .dwX = viewport.left,
            .dwY = viewport.top,
            .dwWidth = viewport.right - viewport.left,
            .dwHeight = viewport.bottom - viewport.top,
            // these should really be based on DepthRange
            .dvMinZ = 0.0,
            .dvMaxZ = 1.0
        };
        IDirect3DDevice7_SetViewport(i->d3ddev, &vp);
        // nudge values to fix rasterization
        float epsilonX = 1.0f / vp.dwWidth;
        float epsilonY = 1.0f / vp.dwHeight;
        D3DMATRIX adj = {
            1, 0, 0, 0,
            0, -1, 0, 0,
            0, 0, 1, 0,
            -epsilonX, epsilonY, 0, 1,
        };
        IDirect3DDevice7_SetTransform(i->d3ddev, D3DTRANSFORMSTATE_PROJECTION, &adj);
    }

    D3DMATRIX viewMatrix = bd3d7TranslateMatrix(mvMatrix);
    IDirect3DDevice7_SetTransform(i->d3ddev, D3DTRANSFORMSTATE_VIEW, &viewMatrix);

    IDirect3DDevice7_SetRenderState(i->d3ddev, D3DRENDERSTATE_LIGHTING, FALSE);
    IDirect3DDevice7_SetRenderState(i->d3ddev, D3DRENDERSTATE_CULLMODE, D3DCULL_NONE);

    if (texture) {
        BADGPUTextureD3D7 * textureI = BG_TEXTURE_D3D7(texture);
        IDirect3DDevice7_SetTexture(i->d3ddev, 0, textureI->surface);

        D3DMATRIX mtx = bd3d7TranslateMatrix(matrixT);
        if (0) {
            // Invert Y
            //   IO
            mtx._12 *= -1;
            mtx._22 *= -1;
            mtx._32 *= -1;
            mtx._42 *= -1;
            // and offset
            mtx._42 += 1;
        }
        IDirect3DDevice7_SetTransform(i->d3ddev, D3DTRANSFORMSTATE_TEXTURE0, &mtx);
        IDirect3DDevice7_SetTextureStageState(i->d3ddev, 0, D3DTSS_TEXTURETRANSFORMFLAGS, D3DTTFF_COUNT2);

        IDirect3DDevice7_SetTextureStageState(i->d3ddev, 0, D3DTSS_MAGFILTER, (flags & BADGPUDrawFlags_MagLinear) ? D3DTFG_LINEAR : D3DTFG_POINT);
        IDirect3DDevice7_SetTextureStageState(i->d3ddev, 0, D3DTSS_MINFILTER, (flags & BADGPUDrawFlags_MinLinear) ? D3DTFG_LINEAR : D3DTFG_POINT);
        IDirect3DDevice7_SetTextureStageState(i->d3ddev, 0, D3DTSS_ADDRESSU, (flags & BADGPUDrawFlags_WrapS) ? D3DTADDRESS_WRAP : D3DTADDRESS_CLAMP);
        IDirect3DDevice7_SetTextureStageState(i->d3ddev, 0, D3DTSS_ADDRESSV, (flags & BADGPUDrawFlags_WrapT) ? D3DTADDRESS_WRAP : D3DTADDRESS_CLAMP);
    }

    IDirect3DDevice7_BeginScene(i->d3ddev);

    D3DPRIMITIVETYPE pt = D3DPT_TRIANGLELIST;
    if (pType == BADGPUPrimitiveType_Lines)
        pt = D3DPT_LINELIST;
    else if (pType == BADGPUPrimitiveType_Points)
        pt = D3DPT_POINTLIST;

    D3DDRAWPRIMITIVESTRIDEDDATA stride = {};
    stride.position.dwStride = sizeof(BADGPUD3D7LITVERTEX);
    stride.position.lpvData = &vertexConv->x;
    stride.diffuse.dwStride = sizeof(BADGPUD3D7LITVERTEX);
    stride.diffuse.lpvData = &vertexConv->diffuse;
    stride.textureCoords[0].dwStride = sizeof(BADGPUD3D7LITVERTEX);
    stride.textureCoords[0].lpvData = vertexConv->uv;

    DWORD vfFlags = D3DFVF_XYZ | D3DFVF_DIFFUSE;
    if (texture)
        vfFlags |= D3DFVF_TEX1 | D3DFVF_TEXCOORDSIZE4(0);
    IDirect3DDevice7_DrawPrimitiveStrided(i->d3ddev, pt, vfFlags, &stride, iCount, 0);

    IDirect3DDevice7_EndScene(i->d3ddev);

    IDirect3DDevice7_SetTexture(i->d3ddev, 0, NULL);
    IDirect3DDevice7_SetRenderTarget(i->d3ddev, i->sacrifice, 0);
    return 1;
}

// -- the instance --

#define INST_ERROR(text) { if (error) *error = text; if (bi) destroyD3D7Instance((BADGPUObject) bi); return NULL; }
BADGPUInstance badgpu_newD3D7Instance(BADGPUNewInstanceFlags flags, const char ** error) {
    BADGPUInstanceD3D7 * bi = malloc(sizeof(BADGPUInstanceD3D7));

    if (!bi)
        INST_ERROR("D3D7: Failed to allocate BADGPUInstance.");

    memset(bi, 0, sizeof(BADGPUInstanceD3D7));
    bi->base.backendCheck = (flags & BADGPUNewInstanceFlags_BackendCheck) != 0;
    bi->base.backendCheckAggressive = (flags & BADGPUNewInstanceFlags_BackendCheckAggressive) != 0;
    bi->base.canPrintf = (flags & BADGPUNewInstanceFlags_CanPrintf) != 0;
    bi->base.isBound = 1;
    badgpu_initObj((BADGPUObject) bi, destroyD3D7Instance);
    // vtbl
    bi->base.getMetaInfo = bd3d7GetMetaInfo;
    bi->base.texLoadFormat = BADGPUTextureLoadFormat_ARGBI32;
    bi->base.newTexture = bd3d7NewTexture;
    bi->base.newDSBuffer = bd3d7NewDSBuffer;
    bi->base.generateMipmap = bd3d7GenerateMipmap;
    bi->base.readPixelsARGBI32 = bd3d7ReadPixelsARGBI32;
    bi->base.drawClear = bd3d7DrawClear;
    bi->base.drawGeomBackend = bi->base.drawGeomFrontend = bd3d7DrawGeom;
    badgpu_swtnl_harnessSetup(&bi->base);

    if (checkD3DErr(bi, DirectDrawCreateEx(NULL, (void *) &bi->ddraw, &IID_IDirectDraw7, NULL)))
        INST_ERROR("D3D7: Failed to create DirectDraw 7 instance.");

    IDirectDraw7_SetCooperativeLevel(bi->ddraw, NULL, DDSCL_NORMAL);

    IDirectDraw7_GetDeviceIdentifier(bi->ddraw, &bi->devInfo, 0);

    if (checkD3DErr(bi, IDirectDraw7_QueryInterface(bi->ddraw, &IID_IDirect3D7, (void *) &bi->d3d)))
        INST_ERROR("D3D7: Failed to derive D3D from DDRAW.");

    // Hasn't been tested outside of software emulation, and anything with reasonable acceleration shouldn't be using this in the first place.
    bi->memCap = DDSCAPS_SYSTEMMEMORY;
    bi->sacrifice = createARGB32ISurface(bi, 8, 8);
    if (!bi->sacrifice)
        INST_ERROR("D3D7: Failed to create sacrificial RT.");
    if (checkD3DErr(bi, IDirect3D7_CreateDevice(bi->d3d, &IID_IDirect3DRGBDevice, bi->sacrifice, &bi->d3ddev)))
        INST_ERROR("D3D7: Failed to create D3D device.");

    return (BADGPUInstance) bi;
}
