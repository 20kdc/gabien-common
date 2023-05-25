/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "una.h"

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

