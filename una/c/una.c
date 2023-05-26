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

#define ZXARGS \
    float f0, float f1, float f2, float f3, float f4, float f5, float f6, float f7, \
    void* a0, void* a1, void* a2, void* a3, void* a4, void* a5, void* a6, void* a7, \
    void* a8, void* a9, void* aA, void* aB, void* aC, void* aD, void* aE, void* aF
#define F_TO_L(x) (*((int32_t*) &(x)))

static int64_t abiProbeA0(ZXARGS) { return J_PTR(a0); }
static int64_t abiProbeA1(ZXARGS) { return J_PTR(a1); }
static int64_t abiProbeA2(ZXARGS) { return J_PTR(a2); }
static int64_t abiProbeA3(ZXARGS) { return J_PTR(a3); }
static int64_t abiProbeA4(ZXARGS) { return J_PTR(a4); }
static int64_t abiProbeA5(ZXARGS) { return J_PTR(a5); }
static int64_t abiProbeA6(ZXARGS) { return J_PTR(a6); }
static int64_t abiProbeA7(ZXARGS) { return J_PTR(a7); }
static int64_t abiProbeA8(ZXARGS) { return J_PTR(a8); }
static int64_t abiProbeA9(ZXARGS) { return J_PTR(a9); }
static int64_t abiProbeAA(ZXARGS) { return J_PTR(aA); }
static int64_t abiProbeAB(ZXARGS) { return J_PTR(aB); }
static int64_t abiProbeAC(ZXARGS) { return J_PTR(aC); }
static int64_t abiProbeAD(ZXARGS) { return J_PTR(aD); }
static int64_t abiProbeAE(ZXARGS) { return J_PTR(aE); }
static int64_t abiProbeAF(ZXARGS) { return J_PTR(aF); }
static int64_t abiProbeF0(ZXARGS) { return F_TO_L(f0); }
static int64_t abiProbeF1(ZXARGS) { return F_TO_L(f1); }
static int64_t abiProbeF2(ZXARGS) { return F_TO_L(f2); }
static int64_t abiProbeF3(ZXARGS) { return F_TO_L(f3); }
static int64_t abiProbeF4(ZXARGS) { return F_TO_L(f4); }
static int64_t abiProbeF5(ZXARGS) { return F_TO_L(f5); }
static int64_t abiProbeF6(ZXARGS) { return F_TO_L(f6); }
static int64_t abiProbeF7(ZXARGS) { return F_TO_L(f7); }

int64_t UNA(getABIProbe)(void * env, void * self, int32_t idx) {
    switch (idx) {
    case 0:  return J_PTR(abiProbeA0);
    case 1:  return J_PTR(abiProbeA1);
    case 2:  return J_PTR(abiProbeA2);
    case 3:  return J_PTR(abiProbeA3);
    case 4:  return J_PTR(abiProbeA4);
    case 5:  return J_PTR(abiProbeA5);
    case 6:  return J_PTR(abiProbeA6);
    case 7:  return J_PTR(abiProbeA7);
    case 8:  return J_PTR(abiProbeA8);
    case 9:  return J_PTR(abiProbeA9);
    case 10: return J_PTR(abiProbeAA);
    case 11: return J_PTR(abiProbeAB);
    case 12: return J_PTR(abiProbeAC);
    case 13: return J_PTR(abiProbeAD);
    case 14: return J_PTR(abiProbeAE);
    case 15: return J_PTR(abiProbeAF);
    case 16: return J_PTR(abiProbeF0);
    case 17: return J_PTR(abiProbeF1);
    case 18: return J_PTR(abiProbeF2);
    case 19: return J_PTR(abiProbeF3);
    case 20: return J_PTR(abiProbeF4);
    case 21: return J_PTR(abiProbeF5);
    case 22: return J_PTR(abiProbeF6);
    case 23: return J_PTR(abiProbeF7);
    }
    return 0;
}

