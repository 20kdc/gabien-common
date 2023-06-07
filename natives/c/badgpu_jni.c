/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "una.h"

// OM

int64_t J_BADGPU(ref)(void * env, void * self, int64_t obj) {
    return J_PTR(badgpuRef(C_PTR(obj)));
}

unsigned char J_BADGPU(unref)(void * env, void * self, int64_t obj) {
    return badgpuUnref(C_PTR(obj));
}

// IM

int64_t J_BADGPU(newInstance)(void * env, void * self, int32_t flags, void * cls) {
    const char * error;
    BADGPUInstance instance = badgpuNewInstance(flags, &error);
    if (instance)
        return J_PTR(instance);
    JNI_ThrowNew(env, cls, error);
    return 0;
}

void * J_BADGPU(getMetaInfo)(void * env, void * self, int64_t instance, int32_t type) {
    const char * text = badgpuGetMetaInfo(C_PTR(instance), type);
    if (text)
        return JNI_NewStringUTF(env, text);
    return NULL;
}

unsigned char J_BADGPU(bindInstance)(void * env, void * self, int64_t instance) {
    return badgpuBindInstance(C_PTR(instance));
}

void J_BADGPU(unbindInstance)(void * env, void * self, int64_t instance) {
    badgpuUnbindInstance(C_PTR(instance));
}

void J_BADGPU(flushInstance)(void * env, void * self, int64_t instance) {
    badgpuFlushInstance(C_PTR(instance));
}

// TCE

void J_BADGPU(pixelsConvertBB)(void * env, void * self, int32_t fF, int32_t tF, int32_t w, int32_t h, JNIBA_ARG(fD), JNIBA_ARG(tD)) {
    JNIBA_L(fD);
    JNIBA_L(tD);
    badgpuPixelsConvert(fF, tF, w, h, fD, tD);
    JNIBA_R(fD, JNI_ABORT);
    JNIBA_R(tD, 0);
}

void J_BADGPU(pixelsConvertBI)(void * env, void * self, int32_t fF, int32_t tF, int32_t w, int32_t h, JNIBA_ARG(fD), JNIBA_ARG(tD)) {
    JNIBA_L(fD);
    JNIIA_L(tD);
    badgpuPixelsConvert(fF, tF, w, h, fD, tD);
    JNIBA_R(fD, JNI_ABORT);
    JNIIA_R(tD, 0);
}

void J_BADGPU(pixelsConvertIB)(void * env, void * self, int32_t fF, int32_t tF, int32_t w, int32_t h, JNIBA_ARG(fD), JNIBA_ARG(tD)) {
    JNIIA_L(fD);
    JNIBA_L(tD);
    badgpuPixelsConvert(fF, tF, w, h, fD, tD);
    JNIIA_R(fD, JNI_ABORT);
    JNIBA_R(tD, 0);
}

void J_BADGPU(pixelsConvertRGBA8888ToARGBI32InPlaceB)(void * env, void * self, int32_t w, int32_t h, JNIBA_ARG(data)) {
    JNIBA_L(data);
    badgpuPixelsConvertRGBA8888ToARGBI32InPlace(w, h, data);
    JNIBA_R(data, 0);
}

void J_BADGPU(pixelsConvertRGBA8888ToARGBI32InPlaceI)(void * env, void * self, int32_t w, int32_t h, JNIBA_ARG(data)) {
    JNIIA_L(data);
    badgpuPixelsConvertRGBA8888ToARGBI32InPlace(w, h, data);
    JNIIA_R(data, 0);
}

// TM

int64_t J_BADGPU(newTextureB)(void * env, void * self, int64_t instance, int32_t w, int32_t h, int32_t fmt, JNIBA_ARG(data)) {
    JNIBA_L(data);
    int64_t res = J_PTR(badgpuNewTexture(C_PTR(instance), w, h, fmt, data));
    JNIBA_R(data, JNI_ABORT);
    return res;
}

int64_t J_BADGPU(newTextureI)(void * env, void * self, int64_t instance, int32_t w, int32_t h, int32_t fmt, JNIBA_ARG(data)) {
    JNIIA_L(data);
    int64_t res = J_PTR(badgpuNewTexture(C_PTR(instance), w, h, fmt, data));
    JNIIA_R(data, JNI_ABORT);
    return res;
}

int64_t J_BADGPU(newDSBuffer)(void * env, void * self, int64_t instance, int32_t w, int32_t h) {
    return J_PTR(badgpuNewDSBuffer(C_PTR(instance), w, h));
}

unsigned char J_BADGPU(generateMipmap)(void * env, void * self, int64_t texture) {
    return badgpuGenerateMipmap(C_PTR(texture));
}

unsigned char J_BADGPU(readPixelsB)(void * env, void * self, int64_t texture, int32_t x, int32_t y, int32_t w, int32_t h, int32_t fmt, JNIBA_ARG(data)) {
    JNIBA_L(data);
    BADGPUBool res = badgpuReadPixels(C_PTR(texture), x, y, w, h, fmt, data);
    JNIBA_R(data, 0);
    return res;
}

unsigned char J_BADGPU(readPixelsI)(void * env, void * self, int64_t texture, int32_t x, int32_t y, int32_t w, int32_t h, int32_t fmt, JNIBA_ARG(data)) {
    JNIIA_L(data);
    BADGPUBool res = badgpuReadPixels(C_PTR(texture), x, y, w, h, fmt, data);
    JNIIA_R(data, 0);
    return res;
}

// DC

#define JSESS_ARGS int64_t sTexture, int64_t sDSBuffer, int32_t sFlags, int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight
#define JSESS_CALL C_PTR(sTexture), C_PTR(sDSBuffer), sFlags, sScX, sScY, sScWidth, sScHeight

unsigned char J_BADGPU(drawClear)(void * env, void * self,
    JSESS_ARGS,
    float cR, float cG, float cB, float cA, float depth, int32_t stencil
) {
    return badgpuDrawClear(
        JSESS_CALL,
        cR, cG, cB, cA, depth, stencil
    );
}

unsigned char J_BADGPU(drawGeom)(void * env, void * self,
    JSESS_ARGS,
    int32_t flags,
    JNIBA_ARG(vPos), JNIBA_ARG(vCol), JNIBA_ARG(vTC),
    int32_t pType, float plSize,
    int32_t iStart, int32_t iCount, JNIBA_ARG(indices),
    JNIBA_ARG(matrixA), JNIBA_ARG(matrixB),
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    int64_t texture, JNIBA_ARG(matrixT),
    int32_t stFunc, int32_t stRef, int32_t stMask,
    int32_t stSF, int32_t stDF, int32_t stDP,
    int32_t dtFunc, float depthN, float depthF, float poFactor, float poUnits,
    int32_t bwRGBS, int32_t bwRGBD, int32_t beRGB,
    int32_t bwAS, int32_t bwAD, int32_t beA
) {
    JNIFA_L(vPos);
    JNIFA_L(vCol);
    JNIFA_L(vTC);
    JNISA_L(indices);
    JNIFA_L(matrixA);
    JNIFA_L(matrixB);
    JNIFA_L(matrixT);
    BADGPUBool res = badgpuDrawGeom(
        JSESS_CALL,
        flags,
        (void *) vPos, (void *) vCol, (void *) vTC,
        pType, plSize,
        iStart, iCount, (void *) indices,
        (void *) matrixA, (void *) matrixB,
        vX, vY, vW, vH,
        C_PTR(texture), (void *) matrixT,
        stFunc, stRef, stMask,
        stSF, stDF, stDP,
        dtFunc, depthN, depthF, poFactor, poUnits,
        bwRGBS, bwRGBD, beRGB,
        bwAS, bwAD, beA
    );
    JNIFA_R(matrixT, JNI_ABORT);
    JNIFA_R(matrixB, JNI_ABORT);
    JNIFA_R(matrixA, JNI_ABORT);
    JNISA_R(indices, JNI_ABORT);
    JNIFA_R(vTC, JNI_ABORT);
    JNIFA_R(vCol, JNI_ABORT);
    JNIFA_R(vPos, JNI_ABORT);
    return res;
}

unsigned char J_BADGPU(drawGeomNoDS)(void * env, void * self,
    int64_t sTexture, int32_t sFlags, int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight,
    int32_t flags,
    JNIBA_ARG(vPos), JNIBA_ARG(vCol), JNIBA_ARG(vTC),
    int32_t pType, float plSize,
    int32_t iStart, int32_t iCount, JNIBA_ARG(indices),
    JNIBA_ARG(matrixA), JNIBA_ARG(matrixB),
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    int64_t texture, JNIBA_ARG(matrixT),
    int32_t bwRGBS, int32_t bwRGBD, int32_t beRGB,
    int32_t bwAS, int32_t bwAD, int32_t beA
) {
    JNIFA_L(vPos);
    JNIFA_L(vCol);
    JNIFA_L(vTC);
    JNISA_L(indices);
    JNIFA_L(matrixA);
    JNIFA_L(matrixB);
    JNIFA_L(matrixT);
    BADGPUBool res = badgpuDrawGeomNoDS(
        C_PTR(sTexture), sFlags, sScX, sScY, sScWidth, sScHeight,
        flags,
        (void *) vPos, (void *) vCol, (void *) vTC,
        pType, plSize,
        iStart, iCount, (void *) indices,
        (void *) matrixA, (void *) matrixB,
        vX, vY, vW, vH,
        C_PTR(texture), (void *) matrixT,
        bwRGBS, bwRGBD, beRGB,
        bwAS, bwAD, beA
    );
    JNIFA_R(matrixT, JNI_ABORT);
    JNIFA_R(matrixB, JNI_ABORT);
    JNIFA_R(matrixA, JNI_ABORT);
    JNISA_R(indices, JNI_ABORT);
    JNIFA_R(vTC, JNI_ABORT);
    JNIFA_R(vCol, JNI_ABORT);
    JNIFA_R(vPos, JNI_ABORT);
    return res;
}

