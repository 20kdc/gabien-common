/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "una.h"

// Peek/Poke

#define PEEKPOKE(char, type, typej) typej UNA(get ## char)(void * env, void * self, int64_t address) {\
    return *((typej *) (intptr_t) address);\
}\
void UNA(set ## char)(void * env, void * self, int64_t address, typej value) {\
    *((type *) (intptr_t) address) = (type) value;\
}

PEEKPOKE(Z, uint8_t, uint8_t)
PEEKPOKE(B, int8_t, int8_t)
PEEKPOKE(C, uint16_t, uint16_t)
PEEKPOKE(S, int16_t, int16_t)
PEEKPOKE(I, int32_t, int32_t)
PEEKPOKE(J, int64_t, int64_t)
PEEKPOKE(F, float, float)
PEEKPOKE(D, double, double)
PEEKPOKE(Ptr, void *, int64_t)

// Bulk peek/poke

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

