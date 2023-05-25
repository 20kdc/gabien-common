/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#pragma once

#include <stdint.h>
#include <stddef.h>
#ifdef WIN32
#include <windows.h>
#endif

#define UNA(x) Java_gabien_una_UNA_ ## x
#define UNAI(x) Java_gabien_una_UNAInvoke_ ## x
#define UNAP(x) Java_gabien_una_UNAPoke_ ## x
#define UNAC(x) Java_gabien_una_UNAC_ ## x

#define C_PTR(l) ((void *) (intptr_t) (l))
#define J_PTR(l) ((int64_t) (intptr_t) (l))

// see jnifns.c to get indices
#define JNIFN(idx) ((*((void***) env))[idx])

// JNI functions that get reused get listed here.
#define JNI_NewStringUTF ((void * (*)(void *, void *)) JNIFN(167))


