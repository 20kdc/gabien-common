/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * BadGPU Reference Implementation
 */

#include "badgpu.h"
#include "badgpu_internal.h"
#include "badgpu_glbind.h"

// Object Management

BADGPU_EXPORT BADGPUObject badgpuRef(BADGPUObject obj) {
    if (!obj)
        return 0;

    obj->refs++;
    return obj;
}

BADGPU_EXPORT BADGPUBool badgpuUnref(BADGPUObject obj) {
    if (!obj)
        return 0;

    obj->refs--;
    if (!obj->refs) {
        obj->destroy(obj);
        return 1;
    }
    return 0;
}

// Instance Creation

BADGPU_EXPORT BADGPUInstance badgpuNewInstance(uint32_t flags, const char ** error) {
    if (flags & BADGPUNewInstanceFlags_ForceInternalRasterizer) {
        if (error)
            *error = "No internal rasterizer.";
        return 0;
    }
    BADGPUBool logDetailed = (flags & BADGPUNewInstanceFlags_CanPrintf) ? 1 : 0;
    BADGPUWSIContext wsi;
    if (flags & BADGPUNewInstanceFlags_PreferEGL) {
        wsi = badgpu_newWsiCtxEGL(error, logDetailed);
        if (!wsi && !badgpu_newWsiCtxPlatformIsEGL())
            wsi = badgpu_newWsiCtxPlatform(NULL, logDetailed);
    } else {
        wsi = badgpu_newWsiCtxPlatform(error, logDetailed);
        if (!wsi && !badgpu_newWsiCtxPlatformIsEGL())
            wsi = badgpu_newWsiCtxEGL(NULL, logDetailed);
    }
    // error provided by preferred hardware source
    if (!wsi)
        return NULL;
    BADGPUInstance instance = badgpuNewInstanceWithWSI(flags, error, wsi);
    return instance;
}

BADGPU_EXPORT const char * badgpuGetMetaInfo(BADGPUInstance instance,
    BADGPUMetaInfoType mi) {
    BADGPUInstancePriv * bi = badgpuBChk(instance, "badgpuGetMetaInfo");
    if (!bi)
        return NULL;
    return bi->getMetaInfo(bi, mi);
}

BADGPU_EXPORT BADGPUBool badgpuBindInstance(BADGPUInstance instance) {
    if (!instance)
        return 0;
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (bi->isBound) {
        if (bi->canPrintf)
            printf("BADGPU: badgpuBindInstance: already bound somewhere\n");
        return 1;
    }
    if (bi->bind)
        if (!bi->bind(bi))
            return 1;
    bi->isBound = 1;
    return 0;
}

BADGPU_EXPORT void badgpuUnbindInstance(BADGPUInstance instance) {
    BADGPUInstancePriv * bi = badgpuBChk(instance, "badgpuUnbindInstance");
    if (!bi)
        return;
    if (bi->unbind)
        bi->unbind(bi);
    bi->isBound = 0;
}

BADGPU_EXPORT void badgpuFlushInstance(BADGPUInstance instance) {
    BADGPUInstancePriv * bi = badgpuBChk(instance, "badgpuFlushInstance");
    if (!bi)
        return;
    if (bi->flush)
        bi->flush(bi);
}

BADGPU_EXPORT void badgpuFinishInstance(BADGPUInstance instance) {
    BADGPUInstancePriv * bi = badgpuBChk(instance, "badgpuFinishInstance");
    if (!bi)
        return;
    if (bi->finish)
        bi->finish(bi);
}

// Texture/2D Buffer Management

BADGPU_EXPORT BADGPUTexture badgpuNewTexture(BADGPUInstance instance,
    int16_t width, int16_t height, BADGPUTextureLoadFormat fmt,
    const void * data) {
    BADGPUInstancePriv * bi = badgpuBChk(instance, "badgpuNewTexture");
    if (!bi)
        return NULL;

    if (width <= 0 || height <= 0) {
        badgpuErr(bi, "badgpuNewTexture: Width or height <= 0.");
        return NULL;
    }

    void * tmpBuf = 0;

    if (data && fmt != bi->texLoadFormat) {
        // Setup conversion buffer and convert
        uint32_t sz = badgpuPixelsSize(bi->texLoadFormat, width, height);
        if (!sz) {
            badgpuErr(bi, "badgpuNewTexture: Invalid format.");
            return 0;
        }
        tmpBuf = malloc(sz);
        if (!tmpBuf) {
            badgpuErr(bi, "badgpuNewTexture: Unable to allocate CVB.");
            return NULL;
        }
        badgpuPixelsConvert(fmt, bi->texLoadFormat, width, height, data, tmpBuf);
        // swap over data pointer
        data = tmpBuf;
    }

    BADGPUTexture tex = bi->newTexture(bi, width, height, data);
    free(tmpBuf);

    return tex;
}

BADGPU_EXPORT BADGPUDSBuffer badgpuNewDSBuffer(BADGPUInstance instance,
    int16_t width, int16_t height) {
    BADGPUInstancePriv * bi = badgpuBChk(instance, "badgpuNewDSBuffer");
    if (!bi)
        return NULL;

    if (width <= 0 || height <= 0) {
        badgpuErr(bi, "badgpuNewDSBuffer: Width or height <= 0.");
        return NULL;
    }

    return bi->newDSBuffer(bi, width, height);
}

BADGPU_EXPORT BADGPUBool badgpuGenerateMipmap(BADGPUTexture texture) {
    if (!texture)
        return 0;
    BADGPUTexturePriv * tex = BG_TEXTURE(texture);
    BADGPUInstancePriv * bi = badgpuBChk((BADGPUInstance) tex->i, "badgpuGenerateMipmap");
    if (!bi)
        return 0;
    return bi->generateMipmap(texture);
}

BADGPU_EXPORT BADGPUBool badgpuReadPixels(BADGPUTexture texture,
    uint16_t x, uint16_t y, int16_t width, int16_t height,
    BADGPUTextureLoadFormat fmt, void * data) {
    BADGPUInstancePriv * bi = badgpuBChk((BADGPUInstance) BG_TEXTURE(texture)->i, "badgpuReadPixels");
    if (width == 0 || height == 0)
        return 1;
    if (width < 0 || height < 0)
        return badgpuErr(bi, "badgpuReadPixels: Width or height < 0.");
    if (!data)
        return badgpuErr(bi, "badgpuReadPixels: data == NULL for non-zero area");

    BADGPUBool res;
    if (fmt == BADGPUTextureLoadFormat_RGBA8888) {
        res = bi->readPixelsRGBA8888(texture, x, y, width, height, data);
    } else if (fmt == BADGPUTextureLoadFormat_ARGBI32) {
        // special fast-path because profiling said so
        res = bi->readPixelsRGBA8888(texture, x, y, width, height, data);
        badgpuPixelsConvertRGBA8888ToARGBI32InPlace(width, height, data);
    } else if (fmt == BADGPUTextureLoadFormat_ARGBI32_SA) {
        // special fast-path because profiling said so
        res = bi->readPixelsRGBA8888(texture, x, y, width, height, data);
        badgpuPixelsConvertRGBA8888ToARGBI32InPlace(width, height, data);
        badgpuPixelsConvertARGBI32PremultipliedToStraightInPlace(width, height, data);
    } else {
        uint32_t sz = badgpuPixelsSize(BADGPUTextureLoadFormat_RGBA8888, width, height);
        if (!sz)
            return badgpuErr(bi, "badgpuReadPixels: Invalid format.");
        void * tmpBuf = malloc(sz);
        if (!tmpBuf)
            return badgpuErr(bi, "badgpuReadPixels: Unable to allocate conversion buffer.");
        res = bi->readPixelsRGBA8888(texture, x, y, width, height, tmpBuf);
        badgpuPixelsConvert(BADGPUTextureLoadFormat_RGBA8888, fmt, width, height, tmpBuf, data);
        free(tmpBuf);
    }
    return res;
}

// Drawing Commands

static inline BADGPUInstancePriv * dcFindInstance(BADGPUTexture sTexture, BADGPUDSBuffer sDSBuffer) {
    BADGPUTexturePriv * sTex = BG_TEXTURE(sTexture);
    BADGPUDSBufferPriv * sDS = BG_DSBUFFER(sDSBuffer);
    BADGPUInstancePriv * bi = NULL;
    if (sTex)
        bi = sTex->i;
    if (sDS)
        bi = sDS->i;
    return bi;
}

BADGPU_EXPORT BADGPUBool badgpuDrawClear(
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
) {
    BADGPUInstancePriv * bi = dcFindInstance(sTexture, sDSBuffer);
    if (!badgpuBChk((BADGPUInstance) bi, "badgpuDrawClear"))
        return 0;
    return bi->drawClear(bi, BADGPU_SESSIONFLAGS_PASSTHROUGH, cR, cG, cB, cA, depth, stencil);
}

BADGPU_EXPORT BADGPUBool badgpuDrawGeom(
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
    BADGPUInstancePriv * bi = dcFindInstance(sTexture, sDSBuffer);
    if (!badgpuBChk((BADGPUInstance) bi, "badgpuDrawGeom"))
        return 0;
    return bi->drawGeom(bi, BADGPU_SESSIONFLAGS_PASSTHROUGH,
        flags,
        vPosD, vPos,
        vCol,
        vTCD, vTC,
        pType, plSize,
        iStart, iCount, indices,
        mvMatrix,
        vX, vY, vW, vH,
        texture, matrixT,
        clipPlane, atFunc, atRef,
        stFunc, stRef, stMask,
        stSF, stDF, stDP,
        dtFunc, depthN, depthF, poFactor, poUnits,
        blendProgram
    );
}

BADGPU_EXPORT BADGPUBool badgpuDrawGeomNoDS(
    BADGPUTexture sTexture,
    uint32_t sFlags,
    int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight,
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
    // Blending
    uint32_t blendProgram
) {
    return badgpuDrawGeom(
    sTexture, NULL,
    sFlags,
    sScX, sScY, sScWidth, sScHeight,
    flags,
    vPosD, vPos, vCol, vTCD, vTC,
    pType, plSize,
    iStart, iCount, indices,
    mvMatrix,
    vX, vY, vW, vH,
    texture, matrixT,
    clipPlane, atFunc, atRef,
    BADGPUCompare_Always, 0, 0,
    BADGPUStencilOp_Keep, BADGPUStencilOp_Keep, BADGPUStencilOp_Keep,
    BADGPUCompare_Always, 0, 0, 0, 0,
    blendProgram
    );
}

// Integration

BADGPU_EXPORT void * badgpuGetWSIValue(BADGPUInstance instance, BADGPUWSIQuery query) {
    BADGPUInstancePriv * bi = BG_INSTANCE(instance);
    if (!bi)
        return NULL;
    if (!bi->ctx) {
        if (query == BADGPUWSIQuery_ContextType)
            return (void *) BADGPUContextType_None;
        return NULL;
    }
    return bi->ctx->getValue(bi->ctx, query);
}

BADGPU_EXPORT uint32_t badgpuGetGLTexture(BADGPUTexture texture) {
    return BG_TEXTURE(texture)->glTex;
}

BADGPU_EXPORT BADGPUTexture badgpuNewTextureFromGL(BADGPUInstance instance, uint32_t glTex) {
    BADGPUInstancePriv * bi = badgpuBChk(instance, "badgpuNewTextureFromGL");
    if (!bi)
        return NULL;
    if (bi->newTextureFromGL)
        return bi->newTextureFromGL(bi, glTex);
    badgpuErr(BG_INSTANCE(bi), "badgpuNewTextureFromGL: Backend does not support this operation.");
    return NULL;
}

BADGPU_EXPORT BADGPUBool badgpuResetGLState(BADGPUInstance instance) {
    BADGPUInstancePriv * bi = badgpuBChk(instance, "badgpuResetGLState");
    if (!bi)
        return 0;
    if (bi->resetGLState)
        return bi->resetGLState(bi);
    badgpuErr(BG_INSTANCE(bi), "badgpuResetGLState: Backend does not support this operation.");
    return NULL;
}
