/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#ifndef BADGPU_INTERNAL_H_
#define BADGPU_INTERNAL_H_

#include <stddef.h>

#ifdef WIN32
#include <windows.h>
#endif

#include "badgpu.h"

// WSI stuff

#ifdef WIN32
#define KHRABI __stdcall
#else
#define KHRABI
#endif

#ifndef WIN32
// Because Zig can get picky about the standard library, let's define these...
void * malloc(size_t sz);
void free(void * mem);
void * memset(void * mem, int c, size_t len);
void * memcpy(void * dst, const void * src, size_t len);
void * dlopen(const char * fn, int flags);
void * dlsym(void * mod, const char * symbol);
int dlclose(void * mod);
#ifndef ANDROID
int printf(const char * fmt, ...);
#else
#include <stdarg.h>
int __android_log_vprint(int prio, const char * tag, const char * fmt, va_list ap);
static inline int printf(const char * fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    int res = __android_log_vprint(3, "BadGPU", fmt, ap);
    va_end(ap);
    return res;
}
#endif
char * strstr(const char * h, const char * n);
#else
#include <stdio.h>
#endif

// Separate declaration of these so they don't end up in API.
// Keep in sync with badgpu.h!

#define BADGPU_SESSIONFLAGS \
    BADGPUTexture sTexture, BADGPUDSBuffer sDSBuffer, \
    uint32_t sFlags, \
    int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight

#define BADGPU_SESSIONFLAGS_PASSTHROUGH \
    sTexture, sDSBuffer, \
    sFlags, \
    sScX, sScY, sScWidth, sScHeight

// DL

// Given a NULL-terminated location list, and an environment variable, loads a native library.
// Returns NULL on error.
typedef struct BADGPUDynLib * BADGPUDynLib;
BADGPUDynLib badgpu_dlOpen(const char ** locations, const char * env);
void * badgpu_dlSym(BADGPUDynLib lib, const char * sym);
void * badgpu_dlSym2(BADGPUDynLib lib, const char * sym1, const char * sym2);
void badgpu_dlClose(BADGPUDynLib lib);

// WSI

typedef struct BADGPUWSICtx * BADGPUWSICtx;

// Creates a new WSICtx.
// "OTR" is a second context which is created for the off-thread retrieval stuff.
BADGPUWSICtx badgpu_newWsiCtx(const char ** error, int * expectDesktopExtensions, void ** eglDisplay, void ** eglContext, void ** eglConfig);
BADGPUBool badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx);
void badgpu_wsiCtxStopCurrent(BADGPUWSICtx ctx);
// Warning: Must be made current first!
void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc);
// Attempting to destroy a context generally clears the current context.
void badgpu_destroyWsiCtx(BADGPUWSICtx ctx);

#endif

