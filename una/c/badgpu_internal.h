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
void * dlopen(const char * fn, int flags);
void * dlsym(void * mod, const char * symbol);
int dlclose(void * mod);
#endif

typedef struct BADGPUWSICtx * BADGPUWSICtx;

// Creates a new WSICtx and automatically makes it current.
BADGPUWSICtx badgpu_newWsiCtx(char ** error);
void badgpu_wsiCtxMakeCurrent(BADGPUWSICtx ctx);
// Warning: Must be made current first!
void * badgpu_wsiCtxGetProcAddress(BADGPUWSICtx ctx, const char * proc);
void badgpu_destroyWsiCtx(BADGPUWSICtx ctx);

#endif

