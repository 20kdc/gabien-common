/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu_internal.h"
#include <string.h>

static BADGPUDynLib dlOpen1(const char * loc) {
#ifdef WIN32
    return (void *) LoadLibraryA(loc);
#else
    return dlopen(loc, 2);
#endif
}

char * getenv(const char *);

BADGPUDynLib badgpu_dlOpen(const char ** locations, const char * env) {
    const char * override = getenv(env);
    if (override) {
        void * res = dlOpen1(override);
        if (res)
            return res;
    }
    while (*locations) {
        void * res = dlOpen1(*locations);
        if (res)
            return res;
        locations++;
    }
    return NULL;
}

void * badgpu_dlSym(BADGPUDynLib lib, const char * sym) {
#ifdef WIN32
    return GetProcAddress((void *) lib, sym);
#else
    return dlsym((void *) lib, sym);
#endif
}

void * badgpu_dlSym2(BADGPUDynLib lib, const char * sym1, const char * sym2) {
    void * v = badgpu_dlSym(lib, sym1);
    if (!v)
        v = badgpu_dlSym(lib, sym2);
    return v;
}

void * badgpu_dlSym4(BADGPUDynLib lib, const char * sym1, const char * sym2, const char * sym3, const char * sym4) {
    void * v = badgpu_dlSym2(lib, sym1, sym2);
    if (!v)
        v = badgpu_dlSym2(lib, sym3, sym4);
    return v;
}

void badgpu_dlClose(BADGPUDynLib lib) {
#ifdef WIN32
    FreeLibrary((void *) lib);
#else
    dlclose((void *) lib);
#endif
}

BADGPUBool badgpu_getEnvFlag(const char * flag) {
    const char * res = getenv(flag);
    if (!res)
        return 0;
    return !strcmp(res, "1") ? 1 : 0;
}
