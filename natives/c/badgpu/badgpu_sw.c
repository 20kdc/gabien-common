/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * The BadGPU Software Rasterizer... would go here, if it existed.
 */

#include "badgpu.h"
#include "badgpu_internal.h"
#include "badgpu_sw.h"

typedef struct BADGPUTextureSW {
    BADGPUTexturePriv base;
    int w, h;
    uint32_t data[];
} BADGPUTextureSW;
#define BG_TEXTURE_SW(x) ((BADGPUTextureSW *) (x))

typedef struct BADGPUDSBufferSW {
    BADGPUDSBufferPriv base;
    int w, h;
    badgpu_ds_t data[];
} BADGPUDSBufferSW;
#define BG_DSBUFFER_SW(x) ((BADGPUDSBufferSW *) (x))

static void destroySWInstance(BADGPUObject obj) {
    free(obj);
}
static void destroySWTexture(BADGPUObject obj) {
    BADGPUTextureSW * tex = BG_TEXTURE_SW(obj);
    badgpuUnref((BADGPUObject) tex->base.i);
    free(obj);
}
static void destroySWDSBuffer(BADGPUObject obj) {
    BADGPUDSBufferSW * tex = BG_DSBUFFER_SW(obj);
    badgpuUnref((BADGPUObject) tex->base.i);
    free(obj);
}

static const char * bswGetMetaInfo(struct BADGPUInstancePriv * instance, BADGPUMetaInfoType mi) {
    if (mi == BADGPUMetaInfoType_Vendor)
        return "BadGPU";
    if (mi == BADGPUMetaInfoType_Renderer)
        return "BadGPU Software Rasterizer";
    if (mi == BADGPUMetaInfoType_Version)
        return "Non-Functional";
    return NULL;
}

static BADGPUTexture bswNewTexture(struct BADGPUInstancePriv * instance, int16_t width, int16_t height, const void * data) {
    size_t datasize = ((size_t) width) * ((size_t) height) * sizeof(uint32_t);
    BADGPUTextureSW * tex = malloc(sizeof(BADGPUTextureSW) + datasize);
    if (!tex) {
        badgpuErr(instance, "badgpuNewTexture: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroySWTexture);

    tex->w = width;
    tex->h = height;
    tex->base.i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));

    if (data)
        memcpy(tex->data, data, datasize);

    return (BADGPUTexture) tex;
}

static BADGPUDSBuffer bswNewDSBuffer(struct BADGPUInstancePriv * instance, int16_t width, int16_t height) {
    size_t datasize = ((size_t) width) * ((size_t) height) * sizeof(badgpu_ds_t);
    BADGPUDSBufferSW * tex = malloc(sizeof(BADGPUDSBufferSW) + datasize);
    if (!tex) {
        badgpuErr(instance, "badgpuNewDSBuffer: Unable to allocate memory.");
        return NULL;
    }
    badgpu_initObj((BADGPUObject) tex, destroySWDSBuffer);

    tex->w = width;
    tex->h = height;
    tex->base.i = BG_INSTANCE(badgpuRef((BADGPUInstance) instance));

    memset(tex->data, 0, datasize);

    return (BADGPUDSBuffer) tex;
}

static BADGPUBool bswGenerateMipmap(void * texture) {
    return 0;
}

static BADGPUBool bswReadPixelsARGBI32(void * texture, uint16_t x, uint16_t y, int16_t width, int16_t height, uint32_t * data) {
    BADGPUTextureSW * tex = BG_TEXTURE_SW(texture);
    int w = width;
    int h = height;
    int i;
    if (x + w > tex->w || y + h > tex->h)
        return badgpuErr(tex->base.i, "badgpuReadPixels: Read out of range");
    const uint32_t * texdata = tex->data;
    for (i = 0; i < height; i++) {
        memcpy(data, texdata, width * sizeof(uint32_t));
        data += width;
        texdata += width;
    }
    return 1;
}

// -- renderfuncs --

static BADGPUBool bswVSize(BADGPU_SESSIONFLAGS, int * w, int * h, badgpu_rect_t * region) {
    if (!sTexture) {
        if (sDSBuffer) {
            *w = BG_DSBUFFER_SW(sDSBuffer)->w;
            *h = BG_DSBUFFER_SW(sDSBuffer)->h;
        } else {
            return 0;
        }
    } else if (sDSBuffer) {
        if (BG_TEXTURE_SW(sTexture)->w != BG_DSBUFFER_SW(sDSBuffer)->w)
            return 0;
        if (BG_TEXTURE_SW(sTexture)->h != BG_DSBUFFER_SW(sDSBuffer)->h)
            return 0;
        *w = BG_TEXTURE_SW(sTexture)->w;
        *h = BG_TEXTURE_SW(sTexture)->h;
    } else {
        *w = BG_TEXTURE_SW(sTexture)->w;
        *h = BG_TEXTURE_SW(sTexture)->h;
    }
    region->l = 0;
    region->u = 0;
    region->r = *w;
    region->d = *h;
    if (sFlags & BADGPUSessionFlags_Scissor)
        badgpu_rectClip(region, badgpu_rect(sScX, sScY, sScWidth, sScHeight));
    return 1;
}

static BADGPUBool bswDrawClear(
    struct BADGPUInstancePriv * instance,
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
) {
    uint32_t pixel = f8topixel(cR, cG, cB, cA);
    badgpu_ds_t ds = { depth, stencil };

    int vpW, vpH;
    BADGPUTextureSW * rTex = BG_TEXTURE_SW(sTexture);
    BADGPUDSBufferSW * rDS = BG_DSBUFFER_SW(sDSBuffer);
    badgpu_rect_t region;
    if (!bswVSize(BADGPU_SESSIONFLAGS_PASSTHROUGH, &vpW, &vpH, &region))
        return 0;

    int x, y;

    if (rTex && (sFlags & BADGPUSessionFlags_MaskRGBA)) {
        uint32_t mask = sessionFlagsToARGBMask(sFlags);
        pixel &= mask;
        for (y = region.u; y < region.d; y++) {
            for (x = region.l; x < region.r; x++) {
                size_t p = x + (y * vpW);
                rTex->data[p] &= ~mask;
                rTex->data[p] |= pixel;
            }
        }
    }
    if (rDS && (sFlags & BADGPUSessionFlags_StencilAll)) {
        for (y = region.u; y < region.d; y++) {
            for (x = region.l; x < region.r; x++) {
                size_t p = x + (y * vpW);
                if (sFlags & BADGPUSessionFlags_MaskDepth)
                    rDS->data[p].depth = ds.depth;
                rDS->data[p].stencil &= ~(sFlags & BADGPUSessionFlags_StencilAll);
                rDS->data[p].stencil |= ds.stencil & sFlags;
            }
        }
    }
    return 1;
}

#define CLIPCON_NON 0
#define CLIPCON_POS 1
#define CLIPCON_NEG 2
#define CLIPCON_CUT 3

typedef struct {
    int type;
    float point;
} clipconclusion_t;

// Find the intersection of two slopes.
// For slope1Offset == 0 && slope2Offset == 0 the answer is always 0.
static inline float slopeIntersection(float slope1Offset, float slope, float slope2Offset, float slope2) {
    // the difference from slope to slope2 increases at this rate
    float diffRate = slope2 - slope;
    return (slope2Offset - slope1Offset) / -diffRate;
}

static clipconclusion_t bswClipperInner(float ax, float aw, float bx, float bw) {
    clipconclusion_t res = {CLIPCON_NON, 0.0f};
    if (ax < -aw) {
        if (bx < -bw) {
            res.type = CLIPCON_CUT;
        } else {
            float xSlope = bx - ax;
            float wSlope = (-bw) - (-aw);
            res.type = CLIPCON_POS;
            res.point = slopeIntersection(ax, xSlope, -aw, wSlope);
            if (res.point <= 0 || res.point >= 1)
                res.type = CLIPCON_CUT;
        }
        return res;
    }
    if (ax > aw) {
        if (bx > bw) {
            res.type = CLIPCON_CUT;
        } else {
            float xSlope = bx - ax;
            float wSlope = bw - aw;
            res.type = CLIPCON_NEG;
            res.point = slopeIntersection(ax, xSlope, aw, wSlope);
            if (res.point <= 0 || res.point >= 1)
                res.type = CLIPCON_CUT;
        }
        return res;
    }
    if (bx < -bw) {
        float xSlope = bx - ax;
        float wSlope = (-bw) - (-aw);
        res.type = CLIPCON_POS;
        res.point = slopeIntersection(ax, xSlope, -aw, wSlope);
        if (res.point <= 0 || res.point >= 1)
            res.type = CLIPCON_CUT;
        return res;
    }
    if (bx > bw) {
        float xSlope = bx - ax;
        float wSlope = bw - aw;
        res.type = CLIPCON_NEG;
        res.point = slopeIntersection(ax, xSlope, aw, wSlope);
        if (res.point <= 0 || res.point >= 1)
            res.type = CLIPCON_CUT;
        return res;
    }
    return res;
}

#define PLANE_COUNT 4

// Finds the clip point (0-1) between two vectors for the given plane.
static clipconclusion_t bswClipper(const BADGPUVector * a, const BADGPUVector * b, const BADGPURasterizerContext * ctx, int planeIndex) {
    if (planeIndex == 0) {
        return bswClipperInner(a->x, a->w, b->x, b->w);
    } else if (planeIndex == 1) {
        return bswClipperInner(a->y, a->w, b->y, b->w);
    } else if (planeIndex == 2) {
        return bswClipperInner(a->z, a->w, b->z, b->w);
    } else if (planeIndex == 3) {
        if (ctx->clipPlane) {
            // todo, understand this well enough to implement
        }
        clipconclusion_t res = {CLIPCON_NON, 0.0f};
        return res;
    } else {
        // non-intersecting
        clipconclusion_t res = {CLIPCON_NON, 0.0f};
        return res;
    }
}

static void bswDrawPoint(
    struct BADGPUInstanceSWTNL * instance,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    float plSize
) {
    int vpW, vpH;
    BADGPUTextureSW * rTex = BG_TEXTURE_SW(ctx->sTexture);
    BADGPUDSBufferSW * rDS = BG_DSBUFFER_SW(ctx->sDSBuffer);
    badgpu_rect_t region;
    if (!bswVSize(ctx->sTexture, ctx->sDSBuffer, ctx->sFlags, ctx->sScX, ctx->sScY, ctx->sScWidth, ctx->sScHeight, &vpW, &vpH, &region))
        return;
    badgpu_swrop_t rop;
    badgpu_ropConfigure(&rop, ctx->sFlags, ctx->blendProgram);
    // would be nice if there was something actually here
    return;
}

static void bswDrawLineClipper(
    struct BADGPUInstanceSWTNL * instance,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    BADGPURasterizerVertex b,
    float plSize,
    int vpW,
    const badgpu_rect_t * region,
    const badgpu_swrop_t * rop,
    int planeIndex
) {
    /*
    while (planeIndex < PLANE_COUNT) {
        clipconclusion_t clip = bswClipper(&a.p, &b.p, ctx, planeIndex);
        planeIndex++;
        if (clip.type == CLIPCON_CUT)
            return;
        if (clip.type == CLIPCON_NEG) {
            bswDrawLineClipper(instance, ctx, badgpu_rvtxLerp(a, b, clip.point), b, plSize, vpW, region, rop, planeIndex);
            return;
        }
        if (clip.type == CLIPCON_POS) {
            bswDrawLineClipper(instance, ctx, a, badgpu_rvtxLerp(a, b, clip.point), plSize, vpW, region, rop, planeIndex);
            return;
        }
    }
    */
    // would be nice if there was something actually here
}

static void bswDrawLine(
    struct BADGPUInstanceSWTNL * instance,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    BADGPURasterizerVertex b,
    float plSize
) {
    int vpW, vpH;
    BADGPUTextureSW * rTex = BG_TEXTURE_SW(ctx->sTexture);
    BADGPUDSBufferSW * rDS = BG_DSBUFFER_SW(ctx->sDSBuffer);
    badgpu_rect_t region;
    if (!bswVSize(ctx->sTexture, ctx->sDSBuffer, ctx->sFlags, ctx->sScX, ctx->sScY, ctx->sScWidth, ctx->sScHeight, &vpW, &vpH, &region))
        return;
    badgpu_swrop_t rop;
    badgpu_ropConfigure(&rop, ctx->sFlags, ctx->blendProgram);
    bswDrawLineClipper(instance, ctx, a, b, plSize, vpW, &region, &rop, 0);
    return;
}

static void bswDrawTriangleClipper(
    struct BADGPUInstanceSWTNL * instance,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    BADGPURasterizerVertex b,
    BADGPURasterizerVertex c,
    int vpW,
    const badgpu_rect_t * region,
    const badgpu_swrop_t * rop,
    int planeIndex
) {
    /*
    while (planeIndex < PLANE_COUNT) {
        clipconclusion_t clipAB = bswClipper(&a.p, &b.p, ctx, planeIndex);
        clipconclusion_t clipBC = bswClipper(&b.p, &c.p, ctx, planeIndex);
        clipconclusion_t clipCA = bswClipper(&c.p, &a.p, ctx, planeIndex);
        if (clipAB.type == CLIPCON_CUT && clipBC.type == CLIPCON_CUT) {
            return;
        } else if (clipBC.type == CLIPCON_CUT && clipCA.type == CLIPCON_CUT) {
            return;
        } else if (clipAB.type == CLIPCON_CUT && clipCA.type == CLIPCON_CUT) {
            return;
        } else if (clipAB.type != CLIPCON_NON && clipAB.type != CLIPCON_CUT) {
            BADGPURasterizerVertex mid = badgpu_rvtxLerp(a, b, clipAB.point);
            bswDrawTriangleClipper(instance, ctx, a, mid, c, vpW, region, rop, planeIndex);
            bswDrawTriangleClipper(instance, ctx, mid, b, c, vpW, region, rop, planeIndex);
            return;
        } else if (clipBC.type != CLIPCON_NON && clipAB.type != CLIPCON_CUT) {
            BADGPURasterizerVertex mid = badgpu_rvtxLerp(b, c, clipBC.point);
            bswDrawTriangleClipper(instance, ctx, a, mid, c, vpW, region, rop, planeIndex);
            bswDrawTriangleClipper(instance, ctx, a, b, mid, vpW, region, rop, planeIndex);
            return;
        } else if (clipCA.type != CLIPCON_NON && clipAB.type != CLIPCON_CUT) {
            BADGPURasterizerVertex mid = badgpu_rvtxLerp(c, a, clipCA.point);
            bswDrawTriangleClipper(instance, ctx, mid, b, c, vpW, region, rop, planeIndex);
            bswDrawTriangleClipper(instance, ctx, a, b, mid, vpW, region, rop, planeIndex);
            return;
        }
        planeIndex++;
    }
    */

    // to window coordinates

    float ndcax = a.p.x / a.p.w;
    float ndcay = a.p.y / a.p.w;
    float ndcbx = b.p.x / b.p.w;
    float ndcby = b.p.y / b.p.w;
    float ndccx = c.p.x / c.p.w;
    float ndccy = c.p.y / c.p.w;

    float wax = ctx->vX + (((ndcax + 1) / 2) * ctx->vW);
    float way = ctx->vY + (((ndcay + 1) / 2) * ctx->vH);
    float wbx = ctx->vX + (((ndcbx + 1) / 2) * ctx->vW);
    float wby = ctx->vY + (((ndcby + 1) / 2) * ctx->vH);
    float wcx = ctx->vX + (((ndccx + 1) / 2) * ctx->vW);
    float wcy = ctx->vY + (((ndccy + 1) / 2) * ctx->vH);

    int x, y;

    for (y = region->u; y < region->d; y++) {
        for (x = region->l; x < region->r; x++) {
            size_t p = x + (y * vpW);
            uint32_t * rgb = ctx->sTexture ? (BG_TEXTURE_SW(ctx->sTexture)->data + p) : NULL;
            //badgpu_rop(rgb, NULL, a.c.x, a.c.y, a.c.z, a.c.w, rop);
        }
    }
}

static void bswDrawTriangle(
    struct BADGPUInstanceSWTNL * instance,
    const BADGPURasterizerContext * ctx,
    BADGPURasterizerVertex a,
    BADGPURasterizerVertex b,
    BADGPURasterizerVertex c
) {
    int vpW, vpH;
    BADGPUTextureSW * rTex = BG_TEXTURE_SW(ctx->sTexture);
    BADGPUDSBufferSW * rDS = BG_DSBUFFER_SW(ctx->sDSBuffer);
    badgpu_rect_t region;
    if (!bswVSize(ctx->sTexture, ctx->sDSBuffer, ctx->sFlags, ctx->sScX, ctx->sScY, ctx->sScWidth, ctx->sScHeight, &vpW, &vpH, &region))
        return;
    badgpu_swrop_t rop;
    badgpu_ropConfigure(&rop, ctx->sFlags, ctx->blendProgram);
    bswDrawTriangleClipper(instance, ctx, a, b, c, vpW, &region, &rop, 0);
    return;
}

// -- the instance --

BADGPUInstance badgpu_newSoftwareInstance(BADGPUNewInstanceFlags flags, const char ** error) {
    BADGPUInstanceSWTNL * bi = malloc(sizeof(BADGPUInstanceSWTNL));
    if (!bi) {
        if (error)
            *error = "Failed to allocate BADGPUInstance.";
        return NULL;
    }
    memset(bi, 0, sizeof(BADGPUInstanceSWTNL));
    bi->base.backendCheck = (flags & BADGPUNewInstanceFlags_BackendCheck) != 0;
    bi->base.backendCheckAggressive = (flags & BADGPUNewInstanceFlags_BackendCheckAggressive) != 0;
    bi->base.canPrintf = (flags & BADGPUNewInstanceFlags_CanPrintf) != 0;
    bi->base.isBound = 1;
    badgpu_initObj((BADGPUObject) bi, destroySWInstance);
    // vtbl
    bi->base.getMetaInfo = bswGetMetaInfo;
    bi->base.texLoadFormat = BADGPUTextureLoadFormat_ARGBI32;
    bi->base.newTexture = bswNewTexture;
    bi->base.newDSBuffer = bswNewDSBuffer;
    bi->base.generateMipmap = bswGenerateMipmap;
    bi->base.readPixelsARGBI32 = bswReadPixelsARGBI32;
    bi->base.drawClear = bswDrawClear;
    bi->base.drawGeom = badgpu_swtnl_drawGeom;
    bi->drawPoint = bswDrawPoint;
    bi->drawLine = bswDrawLine;
    bi->drawTriangle = bswDrawTriangle;
    return (BADGPUInstance) bi;
}
