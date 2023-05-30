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

// TM

int64_t J_BADGPU(newTexture)(void * env, void * self, int64_t instance, int32_t flags, int32_t w, int32_t h, void * data, int64_t offset) {
    data = data ? JNI_GetDirectBufferAddress(env, data) : NULL;
    if (data)
        data += offset;
    return J_PTR(badgpuNewTexture(C_PTR(instance), flags, w, h, data));
}

int64_t J_BADGPU(newDSBuffer)(void * env, void * self, int64_t instance, int32_t w, int32_t h) {
    return J_PTR(badgpuNewDSBuffer(C_PTR(instance), w, h));
}

unsigned char J_BADGPU(generateMipmap)(void * env, void * self, int64_t texture) {
    return badgpuGenerateMipmap(C_PTR(texture));
}

unsigned char J_BADGPU(readPixels)(void * env, void * self, int64_t texture, int32_t x, int32_t y, int32_t w, int32_t h, void * data, int64_t offset) {
    data = data ? JNI_GetDirectBufferAddress(env, data) : NULL;
    if (data)
        data += offset;
    return badgpuReadPixels(C_PTR(texture), x, y, w, h, data);
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
    float depthN, float depthF,
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    int64_t texture, JNIBA_ARG(matrixT),
    float poFactor, float poUnits,
    float alphaTestMin,
    int32_t stFunc, int32_t stRef, int32_t stMask,
    int32_t stSF, int32_t stDF, int32_t stDP,
    int32_t dtFunc,
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
        iStart, iCount, indices,
        (void *) matrixA, (void *) matrixB,
        depthN, depthF,
        vX, vY, vW, vH,
        C_PTR(texture), (void *) matrixT,
        poFactor, poUnits,
        alphaTestMin,
        stFunc, stRef, stMask,
        stSF, stDF, stDP,
        dtFunc,
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
    float depthN, float depthF,
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    int64_t texture, JNIBA_ARG(matrixT),
    float poFactor, float poUnits,
    float alphaTestMin,
    int32_t stFunc, int32_t stRef, int32_t stMask,
    int32_t stSF, int32_t stDF, int32_t stDP,
    int32_t dtFunc,
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
        iStart, iCount, indices,
        (void *) matrixA, (void *) matrixB,
        vX, vY, vW, vH,
        C_PTR(texture), (void *) matrixT,
        alphaTestMin,
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

