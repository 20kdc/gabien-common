/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "una.h"

// DL

void * dlopen(const char * fn, int flags);
void * dlsym(void * module, const char * symbol);
int dlclose(void * module);

int64_t UNAC(dlopen)(void * env, void * self, int64_t str) {
#ifdef WIN32
    return J_PTR(LoadLibraryA(C_PTR(str)));
#else
    return J_PTR(dlopen(C_PTR(str), 2));
#endif
}

int64_t UNAC(dlsym)(void * env, void * self, int64_t module, int64_t str) {
#ifdef WIN32
    return J_PTR(GetProcAddress(C_PTR(module), C_PTR(str)));
#else
    return J_PTR(dlsym(C_PTR(module), C_PTR(str)));
#endif
}

void UNAC(dlclose)(void * env, void * self, int64_t module) {
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

int64_t UNAC(strlen)(void * env, void * self, int64_t str) {
    return strlen(C_PTR(str));
}

int64_t UNAC(strdup)(void * env, void * self, int64_t str) {
    return J_PTR(strdup(C_PTR(str)));
}

int64_t UNAC(memcpy)(void * env, void * self, int64_t dst, int64_t src, int64_t len) {
    return J_PTR(memcpy(C_PTR(dst), C_PTR(src), (size_t) len));
}

int32_t UNAC(memcmp)(void * env, void * self, int64_t a, int64_t b, int64_t len) {
    return (int32_t) memcmp(C_PTR(a), C_PTR(b), (size_t) len);
}

// libc - malloc

void * malloc(size_t sz);
void free(void * mem);
void * realloc(void * mem, size_t sz);
int getpagesize();

int64_t UNAC(malloc)(void * env, void * self, int64_t sz) {
    return J_PTR(malloc((size_t) sz));
}

void UNAC(free)(void * env, void * self, int64_t address) {
    free(C_PTR(address));
}

int64_t UNAC(realloc)(void * env, void * self, int64_t address, int64_t size) {
    return J_PTR(realloc(C_PTR(address), (size_t) size));
}

int64_t UNAC(getpagesize)(void * env, void * self) {
#ifdef WIN32
    return 0x1000;
#else
    return getpagesize();
#endif
}

// JIT
void * mmap(void *, size_t, int, int, int, size_t);
int munmap(void *, size_t);

int64_t UNAC(rwxAlloc)(void * env, void * self, int64_t size) {
#ifdef WIN32
    return J_PTR(VirtualAlloc(NULL, (size_t) size, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE));
#else
    return J_PTR(mmap(NULL, (size_t) size, 7, 0x22, -1, 0));
#endif
}

void UNAC(rwxFree)(void * env, void * self, int64_t address, int64_t size) {
#ifdef WIN32
    VirtualFree(C_PTR(address), 0, MEM_RELEASE);
#else
    munmap(C_PTR(address), (size_t) size);
#endif
}

