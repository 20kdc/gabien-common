/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#pragma once

// So we have printf, etc.
#include "badgpu/badgpu_internal.h"

// things not included there:
#include <limits.h>

#ifndef alloca
#define alloca(s) __builtin_alloca(s)
#endif

#define assert(x)

extern int sprintf(char *, const char *, ...);

extern double exp(double);
extern double log(double);
extern double floor(double);
extern double cos(double);
extern double sin(double);
extern int abs(int);
extern double pow(double, double);
extern double ldexp(double, int);
extern void qsort(void *, size_t, size_t, int (*)(const void *, const void *));
extern int memcmp(const void *, const void *, size_t);
void * memmove(void * dst, const void * src, size_t len);

//

extern const char una_version[];

#ifdef WIN32
#define JNICALL __stdcall
#else
#define JNICALL
#endif

#define J_BADGPU(x) JNICALL Java_gabien_natives_BadGPUUnsafe_ ## x
#define J_VORBIS(x) JNICALL Java_gabien_natives_VorbisUnsafe_ ## x
#define J_MP3(x) JNICALL Java_gabien_natives_MP3Unsafe_ ## x
#define J_LOADER(x) JNICALL Java_gabien_natives_Loader_ ## x

#define C_PTR(l) ((void *) (intptr_t) (l))
#define J_PTR(l) ((int64_t) (intptr_t) (l))

// So there used to be a dedicated program in the repository for finding these
//  indexes. It turns out there's a much better option!
// https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/functions.html
// Contains the index numbers of every function.

#define JNIFN(idx) ((*((void***) env))[idx])

// JNI functions get listed here.
#define JNI_ThrowNew ((int32_t (JNICALL *)(void *, void *, const char *)) JNIFN(14))
#define JNI_NewStringUTF ((void * (JNICALL *)(void *, const char *)) JNIFN(167))
#define JNI_NewDirectBuffer ((void * (JNICALL *)(void *, void *, int64_t)) JNIFN(229))
#define JNI_GetDirectBufferAddress ((void * (JNICALL *)(void *, void *)) JNIFN(230))

#define JNI_ABORT 2

#define JNI_GetPrimitiveArrayCritical ((void * (JNICALL *)(void *, void *, unsigned char *)) JNIFN(222))
#define JNI_ReleasePrimitiveArrayCritical ((void * (JNICALL *)(void *, void *, void *, int32_t)) JNIFN(223))

#define JNIBA_ARG(name) void * name ## _buf, int32_t name ## _ofs

#define JNIXA_L(name, type) type * name = 0, * name ## _ori = 0; if (name ## _buf) { \
    name = name ## _ori = JNI_GetPrimitiveArrayCritical(env, name ## _buf, NULL); \
    name += name ## _ofs; \
}
#define JNIXA_R(name, mode) if (name ## _buf) JNI_ReleasePrimitiveArrayCritical(env, name ## _buf, name ## _ori, mode);

#define JNIBA_L(name) JNIXA_L(name, int8_t)
#define JNISA_L(name) JNIXA_L(name, int16_t)
#define JNIIA_L(name) JNIXA_L(name, int32_t)
#define JNIFA_L(name) JNIXA_L(name, float)
#define JNIBA_R(name, mode) JNIXA_R(name, mode)
#define JNISA_R(name, mode) JNIXA_R(name, mode)
#define JNIIA_R(name, mode) JNIXA_R(name, mode)
#define JNIFA_R(name, mode) JNIXA_R(name, mode)

