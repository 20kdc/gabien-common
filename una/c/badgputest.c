/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu.h"
#include <stdio.h>

void writeQOIFromTex(BADGPUTexture tex, uint32_t w, uint32_t h);

#define T_WIDTH 320
#define T_HEIGHT 200

int main() {
    char * error;
    BADGPUInstance bi = badgpuNewInstance(BADGPUNewInstanceFlags_Debug, &error);
    if (!bi) {
        puts(error);
        return 1;
    }

    // Make a texture to render to!
    BADGPUTexture tex = badgpuNewTexture(bi, 0, BADGPUTextureFormat_RGB, T_WIDTH, T_HEIGHT, NULL);

    // Save the output.
    writeQOIFromTex(tex, T_WIDTH, T_HEIGHT);

    if (!badgpuUnref(tex)) {
        puts("hanging references: BADGPUTexture");
        return 1;
    }
    if (!badgpuUnref(bi)) {
        puts("hanging references: BADGPUInstance");
        return 1;
    }
    return 0;
}

void awfulqoiwriter(uint32_t w, uint32_t h, const uint8_t * rgba);

void writeQOIFromTex(BADGPUTexture tex, uint32_t w, uint32_t h) {
    uint8_t imgData[((size_t) w) * ((size_t) h) * 4];
    badgpuReadPixels(tex, 0, 0, w, h, imgData);
    awfulqoiwriter(w, h, imgData);
}

// This is not how you're supposed to write QOI files.
// However, it's also the simplest image format to dump due to not having a lot
//  of legacy stuff nobody cares about in favour of simplicity.
// (Hilariously, despite this, it also actually has a reasonable approach to
//  the whole sRGB/linear light debate. What a world.)
void awfulqoiwriter(uint32_t w, uint32_t h, const uint8_t * rgba) {
    FILE * f = fopen("tmp.qoi", "wb");
    putc('q', f);
    putc('o', f);
    putc('i', f);
    putc('f', f);
    putc(w >> 24, f);
    putc(w >> 16, f);
    putc(w >> 8, f);
    putc(w, f);
    putc(h >> 24, f);
    putc(h >> 16, f);
    putc(h >> 8, f);
    putc(h, f);
    putc(4, f);
    putc(0, f);
    size_t total = ((size_t) w) * ((size_t) h);
    while (total > 0) {
        putc(255, f);
        putc(*(rgba++), f);
        putc(*(rgba++), f);
        putc(*(rgba++), f);
        putc(*(rgba++), f);
        total--;
    }
    // Fun fact: As far as I can tell, this ending being the way it is, is
    //  the only justification for why the specification insists that you can't
    //  issue multiple index chunks in a row with the same index.
    // It's also kind of pointless because one would assume you'd actually be
    //  decoding the stream if you were reading through for this in the
    //  first place, but whatever.
    putc(0, f);
    putc(0, f);
    putc(0, f);
    putc(0, f);
    putc(0, f);
    putc(0, f);
    putc(0, f);
    putc(1, f);
    fclose(f);
}

