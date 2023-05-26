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

#define ZCARGS \
    float f0, float f1, float f2, float f3, float f4, float f5, float f6, float f7, \
    void* a0, void* a1, void* a2, void* a3, void* a4, void* a5, void* a6, void* a7, \
    void* a8, void* a9, void* aA, void* aB, void* aC, void* aD, void* aE, void* aF

static int64_t zeroCounter0(ZCARGS) { return J_PTR(a0); }
static int64_t zeroCounter1(ZCARGS) { return J_PTR(a1); }
static int64_t zeroCounter2(ZCARGS) { return J_PTR(a2); }
static int64_t zeroCounter3(ZCARGS) { return J_PTR(a3); }
static int64_t zeroCounter4(ZCARGS) { return J_PTR(a4); }
static int64_t zeroCounter5(ZCARGS) { return J_PTR(a5); }
static int64_t zeroCounter6(ZCARGS) { return J_PTR(a6); }
static int64_t zeroCounter7(ZCARGS) { return J_PTR(a7); }
static int64_t zeroCounter8(ZCARGS) { return J_PTR(a8); }
static int64_t zeroCounter9(ZCARGS) { return J_PTR(a9); }
static int64_t zeroCounterA(ZCARGS) { return J_PTR(aA); }
static int64_t zeroCounterB(ZCARGS) { return J_PTR(aB); }
static int64_t zeroCounterC(ZCARGS) { return J_PTR(aC); }
static int64_t zeroCounterD(ZCARGS) { return J_PTR(aD); }
static int64_t zeroCounterE(ZCARGS) { return J_PTR(aE); }
static int64_t zeroCounterF(ZCARGS) { return J_PTR(aF); }
#define F_TO_L(x) (*((int32_t*) &(x)))
static int64_t zeroCounterF0(ZCARGS) { return F_TO_L(f0); }
static int64_t zeroCounterF1(ZCARGS) { return F_TO_L(f1); }
static int64_t zeroCounterF2(ZCARGS) { return F_TO_L(f2); }
static int64_t zeroCounterF3(ZCARGS) { return F_TO_L(f3); }
static int64_t zeroCounterF4(ZCARGS) { return F_TO_L(f4); }
static int64_t zeroCounterF5(ZCARGS) { return F_TO_L(f5); }
static int64_t zeroCounterF6(ZCARGS) { return F_TO_L(f6); }
static int64_t zeroCounterF7(ZCARGS) { return F_TO_L(f7); }

int64_t UNA(getZeroCounter)(void * env, void * self, int32_t idx) {
    switch (idx) {
    case 0: return J_PTR(zeroCounter0);
    case 1: return J_PTR(zeroCounter1);
    case 2: return J_PTR(zeroCounter2);
    case 3: return J_PTR(zeroCounter3);
    case 4: return J_PTR(zeroCounter4);
    case 5: return J_PTR(zeroCounter5);
    case 6: return J_PTR(zeroCounter6);
    case 7: return J_PTR(zeroCounter7);
    case 8: return J_PTR(zeroCounter8);
    case 9: return J_PTR(zeroCounter9);
    case 10: return J_PTR(zeroCounterA);
    case 11: return J_PTR(zeroCounterB);
    case 12: return J_PTR(zeroCounterC);
    case 13: return J_PTR(zeroCounterD);
    case 14: return J_PTR(zeroCounterE);
    case 15: return J_PTR(zeroCounterF);
    case 16: return J_PTR(zeroCounterF0);
    case 17: return J_PTR(zeroCounterF1);
    case 18: return J_PTR(zeroCounterF2);
    case 19: return J_PTR(zeroCounterF3);
    case 20: return J_PTR(zeroCounterF4);
    case 21: return J_PTR(zeroCounterF5);
    case 22: return J_PTR(zeroCounterF6);
    case 23: return J_PTR(zeroCounterF7);
    }
    return 0;
}

