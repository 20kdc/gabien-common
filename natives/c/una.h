/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#pragma once

#include <stdint.h>
#include <stddef.h>
#ifdef WIN32
#include <windows.h>
#endif

#include "badgpu/badgpu.h"

#define J_BADGPU(x) Java_gabien_natives_BadGPUUnsafe_ ## x

#define C_PTR(l) ((void *) (intptr_t) (l))
#define J_PTR(l) ((int64_t) (intptr_t) (l))

// So there used to be a dedicated program in the repository for finding these
//  indexes. It turns out there's a much better option!
// https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/functions.html
// Contains the index numbers of every function.

#define JNIFN(idx) ((*((void***) env))[idx])

// JNI functions get listed here.
#define JNI_ThrowNew ((int32_t (*)(void *, void *, const char *)) JNIFN(14))
#define JNI_NewStringUTF ((void * (*)(void *, const char *)) JNIFN(167))
#define JNI_NewDirectBuffer ((void * (*)(void *, void *, int64_t)) JNIFN(229))
#define JNI_GetDirectBufferAddress ((void * (*)(void *, void *)) JNIFN(230))

#define JNI_ABORT 2

#define JNI_GetByteArrayElements ((void * (*)(void *, void *, unsigned char *)) JNIFN(184))
#define JNI_GetShortArrayElements ((void * (*)(void *, void *, unsigned char *)) JNIFN(186))
#define JNI_GetFloatArrayElements ((void * (*)(void *, void *, unsigned char *)) JNIFN(189))
#define JNI_ReleaseByteArrayElements ((void * (*)(void *, void *, void *, int32_t)) JNIFN(192))
#define JNI_ReleaseShortArrayElements ((void * (*)(void *, void *, void *, int32_t)) JNIFN(194))
#define JNI_ReleaseFloatArrayElements ((void * (*)(void *, void *, void *, int32_t)) JNIFN(197))

#define JNIBA_ARG(name) void * name ## _buf, int32_t name ## _ofs

#define JNIXA_L(name, type, get) type * name = 0; if (name ## _buf) { \
    name = get(env, name ## _buf, NULL); \
    name += name ## _ofs; \
}
#define JNIXA_R(name, rel, mode) if (name ## _buf) JNI_ReleaseByteArrayElements(env, name ## _buf, name, mode);

#define JNIBA_L(name) JNIXA_L(name, uint8_t, JNI_GetByteArrayElements)
#define JNIBA_R(name, mode) JNIXA_R(name, JNI_ReleaseByteArrayElements, mode)
#define JNISA_L(name) JNIXA_L(name, uint16_t, JNI_GetShortArrayElements)
#define JNISA_R(name, mode) JNIXA_R(name, JNI_ReleaseShortArrayElements, mode)
#define JNIFA_L(name) JNIXA_L(name, float, JNI_GetFloatArrayElements)
#define JNIFA_R(name, mode) JNIXA_R(name, JNI_ReleaseFloatArrayElements, mode)

