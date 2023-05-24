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

#define UNA(x) Java_gabien_una_UNA_ ## x
#define JNIFN(idx) ((*((void***) env))[idx])
#define C_PTR(l) ((void *) (intptr_t) (l))
#define J_PTR(l) ((int64_t) (intptr_t) (l))

#define JNI_NewStringUTF ((void * (*)(void *, void *)) JNIFN(167))

// Core

int64_t UNA(getSizeofPtr)(void * env, void * self) {
    return (int64_t) sizeof(void *);
}

static int64_t endianCheck = 1;

// This is "Windows" as opposed to "everything else".
// This is because Windows is the only OS that does a lot of stupid things.
#define SYSFLAG_W32 1
#define SYSFLAG_BE 2
#define SYSFLAG_32 4

int64_t UNA(getSysFlags)(void * env, void * self) {
    int64_t flags = 0;
#ifdef WIN32
    flags |= SYSFLAG_W32;
#endif
    if ((*(int32_t*)&endianCheck) != 1)
        flags |= SYSFLAG_BE;
    if (sizeof(void *) == 4)
        flags |= SYSFLAG_32;
    return flags;
}

void * UNA(getArchOS)(void * env, void * self) {
    return JNI_NewStringUTF(env, UNA_ARCHOS);
}

int64_t UNA(getTestStringRaw)(void * env, void * self) {
    return J_PTR("This is a test string to be retrieved.");
}

// DL

void * dlopen(const char * fn, int flags);
void * dlsym(void * module, const char * symbol);
int dlclose(void * module);

int64_t UNA(dlOpen)(void * env, void * self, int64_t str) {
#ifdef WIN32
    return J_PTR(LoadLibraryA(C_PTR(str)));
#else
    return J_PTR(dlopen(C_PTR(str), 2));
#endif
}

int64_t UNA(dlSym)(void * env, void * self, int64_t module, int64_t str) {
#ifdef WIN32
    return J_PTR(GetProcAddress(C_PTR(module), C_PTR(str)));
#else
    return J_PTR(dlsym(C_PTR(module), C_PTR(str)));
#endif
}

void UNA(dlClose)(void * env, void * self, int64_t module) {
#ifdef WIN32
    FreeLibrary(C_PTR(module));
#else
    dlclose(C_PTR(module));
#endif
}

// libc - string

size_t strlen(const char * mem);
char * strdup(const char * mem);
void * memcpy(void * dst, const void * src, size_t len);
int memcmp(const void * a, const void * b, size_t len);

int64_t UNA(strlen)(void * env, void * self, int64_t str) {
    return strlen(C_PTR(str));
}

int64_t UNA(strdup)(void * env, void * self, int64_t str) {
    return J_PTR(strdup(C_PTR(str)));
}

int64_t UNA(memcpy)(void * env, void * self, int64_t dst, int64_t src, int64_t len) {
    return J_PTR(memcpy(C_PTR(dst), C_PTR(src), (size_t) len));
}

int32_t UNA(memcmp)(void * env, void * self, int64_t a, int64_t b, int64_t len) {
    return (int32_t) memcmp(C_PTR(a), C_PTR(b), (size_t) len);
}

// libc - malloc

void * malloc(size_t sz);
void free(void * mem);
void * realloc(void * mem, size_t sz);

int64_t UNA(malloc)(void * env, void * self, int64_t sz) {
    return J_PTR(malloc((size_t) sz));
}

void UNA(free)(void * env, void * self, int64_t address) {
    free(C_PTR(address));
}

int64_t UNA(realloc)(void * env, void * self, int64_t address, int64_t size) {
    return J_PTR(realloc(C_PTR(address), (size_t) size));
}

// JIT

int getpagesize();
void * mmap(void *, size_t, int, int, int, size_t);
int munmap(void *, size_t);

int64_t UNA(getPageSize)(void * env, void * self) {
#ifdef WIN32
    return 0x1000;
#else
    return getpagesize();
#endif
}

int64_t UNA(rwxAlloc)(void * env, void * self, int64_t size) {
#ifdef WIN32
    return J_PTR(VirtualAlloc(NULL, (size_t) size, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE));
#else
    return J_PTR(mmap(NULL, (size_t) size, 7, 0x22, -1, 0));
#endif
}

void UNA(rwxFree)(void * env, void * self, int64_t address, int64_t size) {
#ifdef WIN32
    VirtualFree(C_PTR(address), 0, MEM_RELEASE);
#else
    munmap(C_PTR(address), (size_t) size);
#endif
}

// Peek/Poke

#define PEEKPOKE(char, type, typej) typej UNA(get ## char)(void * env, void * self, int64_t address) {\
    return *((typej *) (intptr_t) address);\
}\
void UNA(set ## char)(void * env, void * self, int64_t address, typej value) {\
    *((type *) (intptr_t) address) = (type) value;\
}

PEEKPOKE(B, int8_t, int8_t)
PEEKPOKE(S, int16_t, int16_t)
PEEKPOKE(I, int32_t, int32_t)
PEEKPOKE(J, int64_t, int64_t)
PEEKPOKE(F, float, float)
PEEKPOKE(D, double, double)
PEEKPOKE(Ptr, void *, int64_t)

// JNIEnv - base
// see jnifns.c to get indices

// JNIEnv - set/get

/*
 * BEWARE! These names have the opposite polarity to the JNIEnv function names.
 * THIS IS ON PURPOSE!!! From Java's perspective these do the opposite.
 */

#define JNIGR(n, idx) void UNA(n)(void * env, void * self, int64_t address, int64_t length, void * array, int64_t index) {\
    void * (*fn)(void *, void *, size_t, size_t, void *) = JNIFN(idx);\
    fn(env, array, (size_t) index, (size_t) length, C_PTR(address));\
}

JNIGR(setAZ, 199)
JNIGR(setAB, 200)
JNIGR(setAC, 201)
JNIGR(setAS, 202)
JNIGR(setAI, 203)
JNIGR(setAJ, 204)
JNIGR(setAF, 205)
JNIGR(setAD, 206)

JNIGR(getAZ, 207)
JNIGR(getAB, 208)
JNIGR(getAC, 209)
JNIGR(getAS, 210)
JNIGR(getAI, 211)
JNIGR(getAJ, 212)
JNIGR(getAF, 213)
JNIGR(getAD, 214)

// JNIEnv - other

void * UNA(newStringUTF)(void * env, void * self, int64_t address) {
    return JNI_NewStringUTF(env, (void *) (intptr_t) address);
}

void * UNA(newDirectByteBuffer)(void * env, void * self, int64_t address, int64_t length) {
    void * (*newDirectByteBuffer)(void *, void *, int64_t) = JNIFN(229);
    return newDirectByteBuffer(env, (void *) (intptr_t) address, length);
}

int64_t UNA(getDirectByteBufferAddress)(void * env, void * self, void * obj) {
    void * (*getDirectByteBufferAddress)(void *, void *) = JNIFN(230);
    return J_PTR(getDirectByteBufferAddress(env, obj));
}

// Invoke - Special

int64_t UNA(LcIIIIIIP)(void * env, void * self, int32_t a0, int32_t a1, int32_t a2, int32_t a3, int32_t a4, int32_t a5, int64_t a6, int64_t code) {
    int64_t (*fn)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, void *) = C_PTR(code);
    return fn(a0, a1, a2, a3, a4, a5, C_PTR(a6));
}

int64_t UNA(LcIIIIIIIP)(void * env, void * self, int32_t a0, int32_t a1, int32_t a2, int32_t a3, int32_t a4, int32_t a5, int32_t a6, int64_t a7, int64_t code) {
    int64_t (*fn)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, void *) = C_PTR(code);
    return fn(a0, a1, a2, a3, a4, a5, a6, C_PTR(a7));
}

int64_t UNA(LcIIIIIIIIP)(void * env, void * self, int32_t a0, int32_t a1, int32_t a2, int32_t a3, int32_t a4, int32_t a5, int32_t a6, int32_t a7, int64_t a8, int64_t code) {
    int64_t (*fn)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, void *) = C_PTR(code);
    return fn(a0, a1, a2, a3, a4, a5, a6, a7, C_PTR(a8));
}

int64_t UNA(LcIIIIIIII)(void * env, void * self, int32_t a0, int32_t a1, int32_t a2, int32_t a3, int32_t a4, int32_t a5, int32_t a6, int32_t a7, int64_t code) {
    int64_t (*fn)(int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t) = C_PTR(code);
    return fn(a0, a1, a2, a3, a4, a5, a6, a7);
}

