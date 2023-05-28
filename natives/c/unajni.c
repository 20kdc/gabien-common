/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "una.h"

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

