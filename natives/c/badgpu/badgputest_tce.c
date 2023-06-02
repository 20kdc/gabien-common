/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "badgpu.h"
#include <stdio.h>
#include <assert.h>

uint32_t testTex[] = {
    0x11223344, 0x55667788
};
uint32_t testTexLA[] = {
    0xFF223344, 0xFF667788
};

void testWithFT(BADGPUTextureLoadFormat fF, BADGPUTextureLoadFormat tF, int alphaPreserving) {
    uint8_t tmp1[8] = {0};
    uint8_t tmp2[8] = {0};
    uint32_t tmp3[2] = {0, 0};
    printf("test: %i -> %i (alpha: %i)\n", fF, tF, alphaPreserving);
    badgpuPixelsConvert(BADGPUTextureLoadFormat_ARGBI32, fF, 2, 1, testTex, tmp1);
    badgpuPixelsConvert(fF, tF, 2, 1, tmp1, tmp2);
    badgpuPixelsConvert(tF, BADGPUTextureLoadFormat_ARGBI32, 2, 1, tmp2, tmp3);
    const uint32_t * ref = alphaPreserving ? testTex : testTexLA;
    assert(tmp3[0] == ref[0]);
    assert(tmp3[1] == ref[1]);
    printf(" passed\n");
}

void testWithFrom(BADGPUTextureLoadFormat fF, int alphaPreserving) {
    testWithFT(fF, BADGPUTextureLoadFormat_RGBA8888, alphaPreserving);
    testWithFT(fF, BADGPUTextureLoadFormat_RGB888, 0);
    testWithFT(fF, BADGPUTextureLoadFormat_ARGBI32, alphaPreserving);
}

int main() {
    assert(badgpuPixelsSize(BADGPUTextureLoadFormat_RGBA8888, 4, 2) == 32);
    assert(badgpuPixelsSize(BADGPUTextureLoadFormat_RGB888, 4, 2) == 24);
    assert(badgpuPixelsSize(BADGPUTextureLoadFormat_ARGBI32, 4, 2) == 32);
    testWithFrom(BADGPUTextureLoadFormat_RGBA8888, 1);
    testWithFrom(BADGPUTextureLoadFormat_RGB888, 0);
    testWithFrom(BADGPUTextureLoadFormat_ARGBI32, 1);
    return 0;
}

