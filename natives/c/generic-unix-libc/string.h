/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#pragma once

#include <stddef.h>

int strcmp(const char * a, const char * b);
char * strstr(const char * h, const char * n);
void * memset(void * mem, int c, size_t len);
void * memcpy(void * dst, const void * src, size_t len);

