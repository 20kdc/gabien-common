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

#define L_WIDTH 8
#define L_HEIGHT 8

void renderFlagMain(BADGPUTexture tex, int w, int h);

void renderTex2Tex(BADGPUTexture texDst, BADGPUTexture texSrc, int w, int h);

int main() {
    const char * error;
    BADGPUInstance bi = badgpuNewInstance(BADGPUNewInstanceFlags_CanPrintf | BADGPUNewInstanceFlags_BackendCheck, &error);
    if (!bi) {
        puts(error);
        return 1;
    }

    printf("Vendor: %s\n", badgpuGetMetaInfo(bi, BADGPUMetaInfoType_Vendor));
    printf("Renderer: %s\n", badgpuGetMetaInfo(bi, BADGPUMetaInfoType_Renderer));
    printf("Version: %s\n", badgpuGetMetaInfo(bi, BADGPUMetaInfoType_Version));
    // printf("Extensions: %s\n", badgpuGetMetaInfo(bi, 0x1F03));

    // Make a little texture to render to!
    BADGPUTexture tex = badgpuNewTexture(bi, 0, L_WIDTH, L_HEIGHT, BADGPUTextureLoadFormat_RGBA8888, NULL);

    renderFlagMain(tex, L_WIDTH, L_HEIGHT);

    // Make a SECOND texture to render to!
    BADGPUTexture tex2 = badgpuNewTexture(bi, 0, T_WIDTH, T_HEIGHT, BADGPUTextureLoadFormat_RGBA8888, NULL);

    // And render to that.
    renderFlagMain(tex2, T_WIDTH, T_HEIGHT);

    renderTex2Tex(tex2, tex, T_WIDTH / 2, T_HEIGHT / 2);

    // Test restrained clear.
    badgpuDrawClear(tex2, NULL, BADGPUSessionFlags_MaskAll | BADGPUSessionFlags_Scissor, T_WIDTH - 32, 0, 32, 32,
    1, 0, 0, 1, 0, 0);
    badgpuDrawClear(tex2, NULL, BADGPUSessionFlags_MaskAll | BADGPUSessionFlags_Scissor, T_WIDTH - 40, 8, 32, 32,
    0, 1, 0, 1, 0, 0);
    badgpuDrawClear(tex2, NULL, BADGPUSessionFlags_MaskAll | BADGPUSessionFlags_Scissor, T_WIDTH - 48, 16, 32, 32,
    0, 0, 1, 1, 0, 0);

    // Save the output.
    writeQOIFromTex(tex2, T_WIDTH, T_HEIGHT);

    if (!badgpuUnref(tex)) {
        puts("hanging references: BADGPUTexture 1");
        return 1;
    }
    if (!badgpuUnref(tex2)) {
        puts("hanging references: BADGPUTexture 2");
        return 1;
    }
    if (!badgpuUnref(bi)) {
        puts("hanging references: BADGPUInstance");
        return 1;
    }
    return 0;
}

void renderFlagMain(BADGPUTexture tex, int w, int h) {
    // Render to it!
    badgpuDrawClear(tex, NULL, BADGPUSessionFlags_MaskAll, 0, 0, 0, 0,
    1, 0, 1, 1, 0, 0);

#define CC(v) ((v) / 255.0)
#define COL(v) CC(((v) >> 16) & 0xFF), CC(((v) >> 8) & 0xFF), CC((v) & 0xFF), CC(((v) >> 24) & 0xFF)
#define M 0.3333
    BADGPUVector pos[] = {
        {-1,  1, 0, 1},
        { 1,  1, 0, 1},
        { 1,  M, 0, 1},
        {-1,  M, 0, 1},

        {-1,  M, 0, 1},
        { 1,  M, 0, 1},
        { 1, -M, 0, 1},
        {-1, -M, 0, 1},

        {-1, -M, 0, 1},
        { 1, -M, 0, 1},
        { 1, -1, 0, 1},
        {-1, -1, 0, 1}
    };
    BADGPUVector col[] = {
        {COL(0xFFFF218C)},
        {COL(0xFFFF218C)},
        {COL(0xFFFF218C)},
        {COL(0xFFFF218C)},

        {COL(0xFFFFD800)},
        {COL(0xFFFFD800)},
        {COL(0xFFFFD800)},
        {COL(0xFFFFD800)},

        {COL(0xFF21B1FF)},
        {COL(0xFF21B1FF)},
        {COL(0xFF21B1FF)},
        {COL(0xFF21B1FF)}
    };
    uint16_t indices[] = {
        0, 1, 2, 0, 2, 3,
        4, 5, 6, 4, 6, 7,
        8, 9, 10, 8, 10, 11
    };
    // Matrix to invert vertically.
    // If this *isn't* active, the result is the wrong way up.
    BADGPUMatrix matrix = {
        {1, 0, 0, 0},
        {0, -1, 0, 0},
        {0, 0, 1, 0},
        {0, 0, 0, 1}
    };
    badgpuDrawGeomNoDS(
        tex, BADGPUSessionFlags_MaskAll, 0, 0, 0, 0,
        0,
        // Vertex Loader
        pos, col, NULL,
        BADGPUPrimitiveType_Triangles, 1,
        0, 18, indices,
        // Vertex Shader
        &matrix, NULL,
        // Viewport
        0, 0, w, h,
        // Fragment Shader
        NULL, NULL,
        // Alpha Test
        0,
        // Blending
        BADGPUBlendWeight_Zero, BADGPUBlendWeight_Zero, BADGPUBlendEquation_Add,
        BADGPUBlendWeight_Zero, BADGPUBlendWeight_Zero, BADGPUBlendEquation_Add
    );
}

void renderTex2Tex(BADGPUTexture texDst, BADGPUTexture texSrc, int w, int h) {
    // Mesh to test texture coordinates and colour+texture combo
    BADGPUVector pos[] = {
        {-1,  1, 0, 1},
        { 1,  1, 0, 1},
        { 1, -1, 0, 1},
        {-1,  1, 0, 1},
        { 1, -1, 0, 1},
        {-1, -1, 0, 1}
    };
    BADGPUVector col[] = {
        {COL(0xFF80FF80)},
        {COL(0xFF8080FF)},
        {COL(0xFF808080)},
        {COL(0xFF80FF80)},
        {COL(0xFF808080)},
        {COL(0xFFFF8080)}
    };
    BADGPUVector tc[] = {
        {0, 0, 0, 1},
        {1, 0, 0, 1},
        {1, 1, 0, 1},
        {0, 0, 0, 1},
        {1, 1, 0, 1},
        {0, 1, 0, 1}
    };
    // Matrix to test texture matrices.
    // If this *isn't* active, the result is the wrong way up and NOT skewed.
    // (It's intentionally skewed)
    BADGPUMatrix matrix = {
        {1, 0.5, 0, 0},
        {0, -1, 0, 0},
        {0, 0, 1, 0},
        {0, 0.75, 0, 1}
    };
    badgpuDrawGeomNoDS(
        texDst, BADGPUSessionFlags_MaskAll, 0, 0, 0, 0,
        BADGPUDrawFlags_MagLinear,
        // Vertex Loader
        pos, col, tc,
        BADGPUPrimitiveType_Triangles, 1,
        0, 6, NULL,
        // Vertex Shader
        NULL, NULL,
        // Viewport
        0, 0, w, h,
        // Fragment Shader
        texSrc, &matrix,
        // Alpha Test
        0,
        // Blending
        BADGPUBlendWeight_Zero, BADGPUBlendWeight_Zero, BADGPUBlendEquation_Add,
        BADGPUBlendWeight_Zero, BADGPUBlendWeight_Zero, BADGPUBlendEquation_Add
    );
}

void awfulqoiwriter(uint32_t w, uint32_t h, const uint8_t * rgba);

void writeQOIFromTex(BADGPUTexture tex, uint32_t w, uint32_t h) {
    uint8_t imgData[((size_t) w) * ((size_t) h) * 4];
    badgpuReadPixels(tex, 0, 0, w, h, BADGPUTextureLoadFormat_RGBA8888, imgData);
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

