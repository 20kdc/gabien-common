/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * This is the BadGPU software transform-and-actually not light at all module.
 * It's probably not going to be used anytime soon...?
 */

#include "badgpu.h"
#include "badgpu_internal.h"

void badgpu_swtnl_transform(
    uint32_t flags,
    // Vertex Loader
    int32_t vPosD, const float * vPos,
    const float * vCol,
    int32_t vTCD, const float * vTC,
    uint32_t iStart, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * mvMatrix,
    // Fragment Shader
    const BADGPUMatrix * matrixT,
    BADGPURasterizerVertex * out
) {
    size_t vtxIdx = indices ? indices[iStart] : iStart;
    size_t tIdx = (flags & BADGPUDrawFlags_FreezeTC) ? 0 : vtxIdx;
    size_t cIdx = (flags & BADGPUDrawFlags_FreezeColour) ? 0 : vtxIdx;

    vtxIdx *= vPosD;
    tIdx *= vTCD;
    cIdx *= 4;

    BADGPUVector posi = { vPos[vtxIdx], vPos[vtxIdx + 1], 0, 1 };
    if (vPosD >= 3)
        posi.z = vPos[vtxIdx + 2];
    if (vPosD >= 4)
        posi.w = vPos[vtxIdx + 3];

    BADGPUVector tci = { 0, 0, 0, 1 };
    if (vTC) {
        tci.x = vTC[tIdx];
        tci.y = vTC[tIdx + 1];
        if (vTCD >= 3)
            tci.z = vTC[tIdx + 2];
        if (vTCD >= 4)
            tci.w = vTC[tIdx + 3];
    }

    out->p = mvMatrix ? badgpu_vectorByMatrix(posi, mvMatrix) : posi;
    BADGPUVector tco = matrixT ? badgpu_vectorByMatrix(tci, matrixT) : tci;
    out->u = tco.x;
    out->v = tco.y;

    if (vCol) {
        out->c.x = vCol[cIdx];
        out->c.y = vCol[cIdx + 1];
        out->c.z = vCol[cIdx + 2];
        out->c.w = vCol[cIdx + 3];
    } else {
        out->c.x = 1;
        out->c.y = 1;
        out->c.z = 1;
        out->c.w = 1;
    }
}

BADGPUBool badgpu_swtnl_drawGeom(
    struct BADGPUInstancePriv * bi,
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
    BADGPURasterizerContext rc = {
        sTexture, sDSBuffer, sFlags,
        sScX, sScY, sScWidth, sScHeight,
        flags,
        vX, vY, vW, vH,
        texture,
        clipPlane,
        atFunc, atRef,
        stFunc, stRef, stMask,
        stSF, stDF, stDP,
        dtFunc,
        depthN, depthF, poFactor, poUnits,
        blendProgram
    };
    if (pType == BADGPUPrimitiveType_Triangles) {
        BADGPURasterizerVertex vtx[3];
        int rr = 0;
        while (iCount) {
            badgpu_swtnl_transform(flags, vPosD, vPos, vCol, vTCD, vTC, iStart, indices, mvMatrix, matrixT, vtx + rr);
            rr = (rr + 1) % 3;
            iCount--;
            iStart++;
            if (!rr)
                bi->drawTriangleFrontend(bi, &rc, vtx[0], vtx[1], vtx[2]);
        }
        return 1;
    } else if (pType == BADGPUPrimitiveType_Lines) {
        BADGPURasterizerVertex vtx[2];
        int rr = 0;
        while (iCount) {
            badgpu_swtnl_transform(flags, vPosD, vPos, vCol, vTCD, vTC, iStart, indices, mvMatrix, matrixT, vtx + rr);
            rr ^= 1;
            iCount--;
            iStart++;
            if (!rr)
                bi->drawLineFrontend(bi, &rc, vtx[0], vtx[1], plSize);
        }
        return 1;
    } else if (pType == BADGPUPrimitiveType_Points) {
        BADGPURasterizerVertex vtx;
        while (iCount) {
            badgpu_swtnl_transform(flags, vPosD, vPos, vCol, vTCD, vTC, iStart, indices, mvMatrix, matrixT, &vtx);
            iCount--;
            iStart++;
            bi->drawPointFrontend(bi, &rc, vtx, plSize);
        }
        return 1;
    }
    return 0;
}

// SWTNL test harness

#define HARNESS_FWD ctx->sTexture, ctx->sDSBuffer, ctx->sFlags, ctx->sScX, ctx->sScY, ctx->sScWidth, ctx->sScHeight, ctx->flags
#define HARNESS_FWD2 ctx->vX, ctx->vY, ctx->vW, ctx->vH, ctx->texture, NULL, ctx->clipPlane, ctx->atFunc, ctx->atRef, ctx->stFunc, ctx->stRef, ctx->stMask, ctx->stSF, ctx->stDF, ctx->stDP, ctx->dtFunc, ctx->depthN, ctx->depthF, ctx->poFactor, ctx->poUnits, ctx->blendProgram
#define HARNESS_FWD_ALL(primType, plSize, vCount) bi->drawGeomBackend(bi, HARNESS_FWD, 4, (float *) pos, (float *) col, 2, uvs, primType, plSize, 0, vCount, NULL, NULL, HARNESS_FWD2);

void badgpu_swtnl_harnessDrawPoint(struct BADGPUInstancePriv * bi, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, float plSize) {
    BADGPUVector pos[1] = {
        a.p
    };
    BADGPUVector col[1] = {
        a.c
    };
    float uvs[2] = {
        a.u, a.v
    };
    HARNESS_FWD_ALL(BADGPUPrimitiveType_Points, plSize, 1);
}

void badgpu_swtnl_harnessDrawLine(struct BADGPUInstancePriv * bi, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, float plSize) {
    BADGPUVector pos[2] = {
        a.p
    };
    BADGPUVector col[2] = {
        a.c
    };
    float uvs[4] = {
        a.u, a.v,
        b.u, b.v
    };
    HARNESS_FWD_ALL(BADGPUPrimitiveType_Lines, plSize, 2);
}

void badgpu_swtnl_harnessDrawTriangle(struct BADGPUInstancePriv * bi, const BADGPURasterizerContext * ctx, BADGPURasterizerVertex a, BADGPURasterizerVertex b, BADGPURasterizerVertex c) {
    BADGPUVector pos[3] = {
        a.p,
        b.p,
        c.p
    };
    BADGPUVector col[3] = {
        a.c,
        b.c,
        c.c
    };
    float uvs[6] = {
        a.u, a.v,
        b.u, b.v,
        c.u, c.v
    };
    HARNESS_FWD_ALL(BADGPUPrimitiveType_Triangles, 1.0f, 3);
}
