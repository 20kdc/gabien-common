/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include <stdint.h>
#include <stddef.h>
#ifdef WIN32
#include <windows.h>
#endif

// libc stuff

void * malloc(size_t sz);
void free(void * mem);
void * realloc(void * mem, size_t sz);
size_t strlen(const char * mem);
void * dlsym(void * module, const char * symbol);

// Baseline

#define UNA(x) Java_UNA_## x

int64_t UNA(getSizeofPtr)(void * env, void * self) {
    return (int64_t) sizeof(void *);
}

// Serves as part of UNATest
int64_t UNA(getPurpose)(void * env, void * self) {
    return (int64_t) "󱥩󱤖󱥔󱥧󱤑󱤄";
}

int64_t UNA(lookupBootstrap)(void * env, void * self, int64_t str) {
#ifdef WIN32
    return (int64_t) (intptr_t) GetProcAddress(GetModuleHandleA("kernel32"), (const char *) (intptr_t) str);
#else
    return (int64_t) (intptr_t) dlsym(NULL, (const char *) (intptr_t) str);
#endif
}

// libc

int64_t UNA(strlen)(void * env, void * self, int64_t str) {
    return strlen((const char *) (intptr_t) str);
}

int64_t UNA(malloc)(void * env, void * self, int64_t sz) {
    return (int64_t) (intptr_t) malloc((size_t) sz);
}

void UNA(free)(void * env, void * self, int64_t address) {
    free((void *) (intptr_t) address);
}

int64_t UNA(realloc)(void * env, void * self, int64_t address, int64_t size) {
    return (int64_t) (intptr_t) realloc((void *) (intptr_t) address, (size_t) size);
}

// JNIEnv - base
// see jnifns.c to get indices

#define JNIFN(idx) ((*((void***) env))[idx])

#define JNIGR(n, idx) void UNA(n)(void * env, void * self, void * array, int64_t index, int64_t length, int64_t address) {\
    void * (*fn)(void *, void *, size_t, size_t, void *) = JNIFN(idx);\
    fn(env, array, (size_t) index, (size_t) length, (void *) (intptr_t) address);\
}

// JNIEnv - set/get

/*
 * BEWARE! These names have the opposite polarity to the JNIEnv function names.
 * THIS IS ON PURPOSE!!! From Java's perspective these do the opposite.
 */

JNIGR(setBooleanArrayRegion, 199)
JNIGR(setByteArrayRegion, 200)
JNIGR(setCharArrayRegion, 201)
JNIGR(setShortArrayRegion, 202)
JNIGR(setIntArrayRegion, 203)
JNIGR(setLongArrayRegion, 204)
JNIGR(setFloatArrayRegion, 205)
JNIGR(setDoubleArrayRegion, 206)

JNIGR(getBooleanArrayRegion, 207)
JNIGR(getByteArrayRegion, 208)
JNIGR(getCharArrayRegion, 209)
JNIGR(getShortArrayRegion, 210)
JNIGR(getIntArrayRegion, 211)
JNIGR(getLongArrayRegion, 212)
JNIGR(getFloatArrayRegion, 213)
JNIGR(getDoubleArrayRegion, 214)

// JNIEnv - other

void * UNA(newDirectByteBuffer)(void * env, void * self, int64_t address, int64_t length) {
    void * (*newDirectByteBuffer)(void *, void *, int64_t) = JNIFN(229);
    return newDirectByteBuffer(env, (void *) (intptr_t) address, length);
}

int64_t UNA(getDirectByteBufferAddress)(void * env, void * self, void * obj) {
    void * (*getDirectByteBufferAddress)(void *, void *) = JNIFN(230);
    return (int64_t) (intptr_t) getDirectByteBufferAddress(env, obj);
}

