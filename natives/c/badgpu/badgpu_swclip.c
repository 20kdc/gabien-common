/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * The BadGPU Software Clipper (would go here if it was adapted to the new pipeline arrangement)
 */

#include "badgpu_internal.h"

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

void badgpu_swclip_drawPoint(struct BADGPUInstancePriv * bi, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, float plSize) {
    bi->drawPointBackend(bi, ctx, a, plSize);
}

static void bswDrawLineClipper(struct BADGPUInstancePriv * bi, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, float plSize, int planeIndex) {
    while (planeIndex < PLANE_COUNT) {
        clipconclusion_t clip = bswClipper(&a.p, &b.p, ctx, planeIndex);
        planeIndex++;
        if (clip.type == CLIPCON_CUT)
            return;
        if (clip.type == CLIPCON_NEG) {
            bswDrawLineClipper(bi, ctx, badgpu_rvtxLerp(a, b, clip.point), b, plSize, planeIndex);
            return;
        }
        if (clip.type == CLIPCON_POS) {
            bswDrawLineClipper(bi, ctx, a, badgpu_rvtxLerp(a, b, clip.point), plSize, planeIndex);
            return;
        }
    }
    bi->drawLineBackend(bi, ctx, a, b, plSize);
}

void badgpu_swclip_drawLine(struct BADGPUInstancePriv * bi, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, float plSize) {
    bswDrawLineClipper(bi, ctx, a, b, plSize, 0);
}

static void bswDrawTriangleClipper(struct BADGPUInstancePriv * bi, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, BADGPURasterizerVertex c, int planeIndex) {
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
            bswDrawTriangleClipper(bi, ctx, a, mid, c, planeIndex);
            bswDrawTriangleClipper(bi, ctx, mid, b, c, planeIndex);
            return;
        } else if (clipBC.type != CLIPCON_NON && clipAB.type != CLIPCON_CUT) {
            BADGPURasterizerVertex mid = badgpu_rvtxLerp(b, c, clipBC.point);
            bswDrawTriangleClipper(bi, ctx, a, mid, c, planeIndex);
            bswDrawTriangleClipper(bi, ctx, a, b, mid, planeIndex);
            return;
        } else if (clipCA.type != CLIPCON_NON && clipAB.type != CLIPCON_CUT) {
            BADGPURasterizerVertex mid = badgpu_rvtxLerp(c, a, clipCA.point);
            bswDrawTriangleClipper(bi, ctx, mid, b, c, planeIndex);
            bswDrawTriangleClipper(bi, ctx, a, b, mid, planeIndex);
            return;
        }
        planeIndex++;
    }
    bi->drawTriangleBackend(bi, ctx, a, b, c);
}

void badgpu_swclip_drawTriangle(struct BADGPUInstancePriv * bi, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, BADGPURasterizerVertex c) {
    bswDrawTriangleClipper(bi, ctx, a, b, c, 0);
}
