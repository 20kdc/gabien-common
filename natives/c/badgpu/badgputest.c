/*
* gabien-common - Cross-platform game and UI framework
* Written starting in 2016 by contributors (see CREDITS.txt)
* To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
* A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
*/

#include "badgpu.h"
#include <stdio.h>
#include <string.h>

void writeTGAFromTex(const char * name, BADGPUTexture tex, uint32_t w, uint32_t h);

#define T_WIDTH 320
#define T_HEIGHT 200

#define L_WIDTH 8
#define L_HEIGHT 8

void renderFlagMain(BADGPUTexture tex, int w, int h);

void renderTex2Tex(BADGPUTexture texDst, BADGPUTexture texSrc, int w, int h);

void render3DTest(BADGPUTexture tex, BADGPUDSBuffer dsb, BADGPUTexture texSrc, const float * clipPlane);

void renderDemoTriangle(BADGPUTexture texDst, int w, int h);

int main(int argc, char ** argv) {
    if (argc != 3) {
        printf("test VARIANT VARIANT_ID\n");
        return 1;
    }

    int flags = BADGPUNewInstanceFlags_CanPrintf | BADGPUNewInstanceFlags_BackendCheck;
    const char * varidx = argv[2];
    char fmtbuf[32];

    if (!strcmp(argv[1], "egl")) {
        flags |= BADGPUNewInstanceFlags_PreferEGL;
    } else if (!strcmp(argv[1], "sw")) {
        flags |= BADGPUNewInstanceFlags_ForceInternalRasterizer;
    } else if (!strcmp(argv[1], "z")) {
    } else {
        printf("Unknown arg %s\n", argv[1]);
        return 1;
    }

    const char * error;
    BADGPUInstance bi = badgpuNewInstance(flags, &error);
    if (!bi) {
        puts(error);
        return 1;
    }

    printf("Vendor: %s\n", badgpuGetMetaInfo(bi, BADGPUMetaInfoType_Vendor));
    printf("Renderer: %s\n", badgpuGetMetaInfo(bi, BADGPUMetaInfoType_Renderer));
    printf("Version: %s\n", badgpuGetMetaInfo(bi, BADGPUMetaInfoType_Version));
    // printf("Extensions: %s\n", badgpuGetMetaInfo(bi, 0x1F03));

    // Make a little texture to render to!
    BADGPUTexture tex = badgpuNewTexture(bi, L_WIDTH, L_HEIGHT, BADGPUTextureLoadFormat_RGBA8888, NULL);

    renderFlagMain(tex, L_WIDTH, L_HEIGHT);

    // Make a SECOND texture to render to!
    BADGPUTexture tex2 = badgpuNewTexture(bi, T_WIDTH, T_HEIGHT, BADGPUTextureLoadFormat_RGBA8888, NULL);

    // And render to that.
    renderFlagMain(tex2, T_WIDTH, T_HEIGHT);

    renderTex2Tex(tex2, tex, T_WIDTH / 2, T_HEIGHT / 2);

    renderDemoTriangle(tex2, T_WIDTH, T_HEIGHT);

    // Test restrained clear.
    badgpuDrawClear(tex2, NULL, BADGPUSessionFlags_MaskAll | BADGPUSessionFlags_Scissor, T_WIDTH - 32, 0, 32, 32,
    1, 0, 0, 1, 0, 0);
    badgpuDrawClear(tex2, NULL, BADGPUSessionFlags_MaskAll | BADGPUSessionFlags_Scissor, T_WIDTH - 40, 8, 32, 32,
    0, 1, 0, 1, 0, 0);
    badgpuDrawClear(tex2, NULL, BADGPUSessionFlags_MaskAll | BADGPUSessionFlags_Scissor, T_WIDTH - 48, 16, 32, 32,
    0, 0, 1, 1, 0, 0);

    // Save the output.
    sprintf(fmtbuf, "%s.1.tga", varidx);
    writeTGAFromTex(fmtbuf, tex2, T_WIDTH, T_HEIGHT);

    // 3D scene test
    BADGPUDSBuffer dsb = badgpuNewDSBuffer(bi, T_WIDTH, T_HEIGHT);

    // Make a third texture (for checkerboard)
    int chk[16] = { -1, 0xFF000000, -1, 0xFF000000, 0xFF000000, -1, 0xFF000000, -1, -1, 0xFF000000, -1, 0xFF000000, 0xFF000000, -1, 0xFF000000, -1 };
    BADGPUTexture tex3 = badgpuNewTexture(bi, 4, 4, BADGPUTextureLoadFormat_ARGBI32, chk);

    render3DTest(tex2, dsb, tex3, NULL);

    sprintf(fmtbuf, "%s.2.tga", varidx);
    writeTGAFromTex(fmtbuf, tex2, T_WIDTH, T_HEIGHT);

    // ???
    float clipPlane[4] = {-1.0f, 0, 0.5f, 0.0f};

    render3DTest(tex2, dsb, tex3, clipPlane);

    sprintf(fmtbuf, "%s.3.tga", varidx);
    writeTGAFromTex(fmtbuf, tex2, T_WIDTH, T_HEIGHT);

    if (!badgpuUnref(tex)) {
        puts("hanging references: BADGPUTexture 1");
        return 1;
    }
    if (!badgpuUnref(tex2)) {
        puts("hanging references: BADGPUTexture 2");
        return 1;
    }
    if (!badgpuUnref(tex3)) {
        puts("hanging references: BADGPUTexture 3");
        return 1;
    }
    if (!badgpuUnref(dsb)) {
        puts("hanging references: BADGPUDSBuffer");
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
    float pos[] = {
        -1,  1, 0, 1,
        1,  1, 0, 1,
        1,  M, 0, 1,
        -1,  M, 0, 1,

        -1,  M, 0, 1,
        1,  M, 0, 1,
        1, -M, 0, 1,
        -1, -M, 0, 1,

        -1, -M, 0, 1,
        1, -M, 0, 1,
        1, -1, 0, 1,
        -1, -1, 0, 1
    };
    float col[] = {
        COL(0xFFFF218C),
        COL(0xFFFF218C),
        COL(0xFFFF218C),
        COL(0xFFFF218C),

        COL(0xFFFFD800),
        COL(0xFFFFD800),
        COL(0xFFFFD800),
        COL(0xFFFFD800),

        COL(0xFF21B1FF),
        COL(0xFF21B1FF),
        COL(0xFF21B1FF),
        COL(0xFF21B1FF)
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
        4, pos, col, 2, NULL,
        BADGPUPrimitiveType_Triangles, 1,
        0, 18, indices,
        // Vertex Shader
        &matrix,
        // Viewport
        0, 0, w, h,
        // Fragment Shader
        NULL, NULL,
        NULL, BADGPUCompare_Always, 0,
        // Blending
        0
    );
}

void renderTex2Tex(BADGPUTexture texDst, BADGPUTexture texSrc, int w, int h) {
    // Mesh to test texture coordinates and colour+texture combo
    float pos[] = {
        -1,  1,
        1,  1,
        1, -1,
        -1,  1,
        1, -1,
        -1, -1
    };
    float col[] = {
        COL(0xFF80FF80),
        COL(0xFF8080FF),
        COL(0xFF808080),
        COL(0xFF80FF80),
        COL(0xFF808080),
        COL(0xFFFF8080)
    };
    float tc[] = {
        0, 0,
        1, 0,
        1, 1,
        0, 0,
        1, 1,
        0, 1
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
        2, pos, col, 2, tc,
        BADGPUPrimitiveType_Triangles, 1,
        0, 6, NULL,
        // Vertex Shader
        NULL,
        // Viewport
        0, 0, w, h,
        // Fragment Shader
        texSrc, &matrix,
        NULL, BADGPUCompare_Always, 0,
        // Blending
        0
    );
}

void renderDemoTriangle(BADGPUTexture texDst, int w, int h) {
    float pos[] = {
        0, 0.5,
        -0.5, -0.5,
        0.5, -0.5
    };
    float col[] = {
        1, 0, 0, 1,
        0, 1, 0, 1,
        0, 0, 1, 1
    };
    badgpuDrawGeomNoDS(
        texDst, BADGPUSessionFlags_MaskAll, 0, 0, 0, 0,
        0,
        // Vertex Loader
        2, pos, col, 2, NULL,
        BADGPUPrimitiveType_Triangles, 1,
        0, 3, NULL,
        // Vertex Shader
        NULL,
        // Viewport
        0, 0, w, h,
        // Fragment Shader
        NULL, NULL,
        NULL, BADGPUCompare_Always, 0,
        // Blending
        0
    );
}

void render3DTestSpecific(BADGPUTexture tex2, BADGPUDSBuffer dsb, BADGPUTexture texSrc, const BADGPUMatrix * matrix, const float * clipPlane) {
    // Mesh to test 3D
    float pos[] = {
        -1, 1, 1,
        1, 1, 1,
        1, 1, 0,
        -1, 1, 1,
        1, 1, 0,
        -1, 1, 0
    };
    float tc[] = {
        0, 0,
        1, 0,
        1, 1,
        0, 0,
        1, 1,
        0, 1
    };
    // Matrix to test 3D.
    badgpuDrawGeom(tex2, dsb, BADGPUSessionFlags_MaskAll, 0, 0, 0, 0,
        BADGPUDrawFlags_StencilTest,
        // Vertex Loader
        3, pos, NULL, 2, tc,
        BADGPUPrimitiveType_Triangles, 1,
        0, 6, NULL,
        // Vertex Shader
        matrix,
        // Viewport
        0, 0, T_WIDTH, T_HEIGHT,
        // Fragment Shader
        texSrc, NULL,
        clipPlane, BADGPUCompare_Always, 0,
        // Stencil Test
        BADGPUCompare_Always, 0, 0xFF,
        BADGPUStencilOp_Keep, BADGPUStencilOp_Keep, BADGPUStencilOp_Inc,
        // Depth Test
        BADGPUCompare_LEqual, 0, 1, 0, 0,
        // Blending
        0
    );
}

void render3DTest(BADGPUTexture tex2, BADGPUDSBuffer dsb, BADGPUTexture texSrc, const float * clipPlane) {
    badgpuDrawClear(tex2, dsb, BADGPUSessionFlags_MaskAll, 0, 0, T_WIDTH, T_HEIGHT, 0, 0, 0, 0, 1, 0);
    // First obj
    BADGPUMatrix matrix1 = {
        { 1,  0,  0,  0},
        { 0,  1,  0,  0},
        { 0,  0,  1,  1},
        { 0,  0,  0,  1}
    };
    // Second obj, should be partially occluded by object 1
    BADGPUMatrix matrix2 = {
        { 0.5,  0,  0.5,  0.5},
        {   0,  0,    1,    1},
        {   0,  2,    0,    0},
        {   0,  0, -0.5,  0.5}
    };
    render3DTestSpecific(tex2, dsb, texSrc, &matrix1, clipPlane);
    render3DTestSpecific(tex2, dsb, texSrc, &matrix2, clipPlane);
    // Mesh to test 3D
    float pos[] = {
        -1,  1,
        1,  1,
        1, -1,
        -1,  1,
        1, -1,
        -1, -1,
    };
    // Overdraw2 indicator
    float col[] = {1, 0, 1, 1};
    badgpuDrawGeom(tex2, dsb, BADGPUSessionFlags_MaskAll, 0, 0, 0, 0,
        BADGPUDrawFlags_FreezeColour | BADGPUDrawFlags_StencilTest,
        // Vertex Loader
        2, pos, col, 2, NULL,
        BADGPUPrimitiveType_Triangles, 1,
        0, 6, NULL,
        // Vertex Shader
        NULL,
        // Viewport
        0, 0, T_WIDTH, T_HEIGHT,
        // Fragment Shader
        NULL, NULL,
        NULL, BADGPUCompare_Always, 0,
        // Stencil Test
        BADGPUCompare_Equal, 2, 0xFF,
        BADGPUStencilOp_Keep, BADGPUStencilOp_Keep, BADGPUStencilOp_Keep,
        // Depth Test
        BADGPUCompare_Always, 0, 1, 0, 0,
        // Blending
        0
    );
}

void awfultgawriter(const char * name, uint32_t w, uint32_t h, const uint8_t * rgba);

void writeTGAFromTex(const char * name, BADGPUTexture tex, uint32_t w, uint32_t h) {
    uint8_t imgData[((size_t) w) * ((size_t) h) * 4];
    badgpuReadPixels(tex, 0, 0, w, h, BADGPUTextureLoadFormat_RGBA8888, imgData);
    awfultgawriter(name, w, h, imgData);
}

// This is not how you're supposed to write PNG files.
// However, it's also the simplest image format to dump due to not having a lot
//  of legacy stuff nobody cares about in favour of simplicity.
// (Hilariously, despite this, it also actually has a reasonable approach to
//  the whole sRGB/linear light debate. What a world.)
void awfultgawriter(const char * name, uint32_t w, uint32_t h, const uint8_t * rgba) {
    FILE * f = fopen(name, "wb");
    putc(0, f); // id
    putc(0, f); // cmap1
    putc(2, f); // image

    for (int i = 0; i < 5; i++)
        putc(0, f); // cmap2

    // 0x08

    putc(0, f); // xo
    putc(0, f);

    putc(h, f);
    putc(h >> 8, f); // yo

    putc(w, f);
    putc(w >> 8, f); // xs

    putc(h, f);
    putc(h >> 8, f); // ys

    // 0x10

    putc(0x20, f); // bit depth
    putc(0x28, f); // image descriptor

    size_t total = ((size_t) w) * ((size_t) h);
    while (total > 0) {
        putc(rgba[2], f);
        putc(rgba[1], f);
        putc(rgba[0], f);
        putc(rgba[3], f);
        rgba += 4;
        total--;
    }

    // footer

    for (int i = 0; i < 8; i++)
        putc(0, f);

    fputs("TRUEVISION-XFILE.", f);
    putc(0, f);

    fclose(f);
}

