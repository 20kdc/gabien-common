/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.natives.examples.stencilshadows;

import gabien.natives.BadGPU;
import gabien.natives.BadGPUEnum.PrimitiveType;
import gabien.natives.examples.U;

/**
 * I am aware this is literally the worst way you could handle a triangle, like, ever.
 * I know what I'm doing, though; this is meant to be a tech demo!
 * Created 11th June, 2023.
 */
public class STHTriangle {
    public float aX, aY, aZ;
    public float bX, bY, bZ;
    public float cX, cY, cZ;
    // average
    private float dX, dY, dZ;
    // normal
    private float nX, nY, nZ;
    // Vertices: A, B, C, and the volume points
    // We don't try to be clever or efficient with our stencil projection.
    // And we don't try to handle the camera being in the shadow.
    // So don't try fancy, patented, and ultimately dead-and-rotting techniques.
    // (If you know, you know.)
    private float[] internalVertexBuffer = new float[3 * 6];
    private void copyABC() {
        // Copy ABC over.
        internalVertexBuffer[0] = aX; internalVertexBuffer[1] = aY; internalVertexBuffer[2] = aZ;
        internalVertexBuffer[3] = bX; internalVertexBuffer[4] = bY; internalVertexBuffer[5] = bZ;
        internalVertexBuffer[6] = cX; internalVertexBuffer[7] = cY; internalVertexBuffer[8] = cZ;
    }
    //  3
    //  0
    // 2 1
    //5   4
    private short[] stencilVertices = { 
            0, 1, 2, // main

            0, 3, 1,
            1, 3, 4,

            5, 2, 1,
            5, 1, 4,

            5, 3, 2,
            2, 3, 0
    };
    private float[] internalColourBuffer = new float[4];
    // Depth prepass.
    public void drawDepth(STHCameraSetup setup) {
        copyABC();
        BadGPU.drawGeom(setup.backBuffer, setup.dsBuffer,
                BadGPU.SessionFlags.MaskDepth, 0, 0, 0, 0,
                BadGPU.DrawFlags.DepthTest,
                3, internalVertexBuffer, 0, null, 0, 2, null, 0,
                PrimitiveType.Triangles, 0,
                0, 3, null, 0,
                setup.matrix, 0, null, 0,
                0, 0, setup.bW, setup.bH,
                null, null, 0,
                BadGPU.Compare.Always, 0, 0,
                BadGPU.StencilOp.Keep, BadGPU.StencilOp.Keep, BadGPU.StencilOp.Keep,
                BadGPU.Compare.LEqual, 0, 1, 0, 0,
                0);
    }
    public void calcD() {
        // average
        dX = (aX + bX + cX) / 3;
        dY = (aY + bY + cY) / 3;
        dZ = (aZ + bZ + cZ) / 3;
        // normal
        float cAX = bX - aX;
        float cAY = bY - aY;
        float cAZ = bZ - aZ;
        float cBX = cX - aX;
        float cBY = cY - aY;
        float cBZ = cZ - aZ;
        nX = (cAY * cBZ) - (cAZ * cBY);
        nY = (cAZ * cBX) - (cAX * cBZ);
        nZ = (cAX * cBY) - (cAY * cBX);
    }
    // Shadow stencil pass. This "accumulates" in the stencil buffer.
    public void drawShadow(STHCameraSetup setup, float x, float y, float z) {
        // We need to not shadow triangles facing away from the light.
        // Otherwise they cancel out our existing shadow volumes.
        float sign = (nX * (x - dX)) + (nY * (y - dY)) + (nZ * (z - dZ));
        if (sign < 0)
            return;
        // Start preparing...
        copyABC();
        // "infinite light point" should be in opposite direction of light
        // Let's say light is at 2, and D is at 1. Result should be -1.
        // Then amp by a mystery factor....
        // Create projected shadow points from regular points.
        for (int i = 0; i < 3; i++) {
            int base = i * 3;
            float mysteryFactor = 256;
            internalVertexBuffer[base + 9] = (internalVertexBuffer[base + 0] - x) * mysteryFactor;
            internalVertexBuffer[base + 10] = (internalVertexBuffer[base + 1] - y) * mysteryFactor;
            internalVertexBuffer[base + 11] = (internalVertexBuffer[base + 2] - z) * mysteryFactor;
        }
        for (int i = 0; i < 2; i++)
            BadGPU.drawGeom(setup.backBuffer, setup.dsBuffer,
                    BadGPU.SessionFlags.StencilAll, 0, 0, 0, 0,
                    BadGPU.DrawFlags.DepthTest | BadGPU.DrawFlags.StencilTest |
                    BadGPU.DrawFlags.CullFace | (i == 0 ? BadGPU.DrawFlags.CullFaceFront : 0),
                    3, internalVertexBuffer, 0, null, 0, 2, null, 0,
                    PrimitiveType.Triangles, 0,
                    0, stencilVertices.length, stencilVertices, 0,
                    setup.matrix, 0, null, 0,
                    0, 0, setup.bW, setup.bH,
                    null, null, 0,
                    BadGPU.Compare.Always, 0, 0,
                    BadGPU.StencilOp.Keep, BadGPU.StencilOp.Keep, i == 0 ? BadGPU.StencilOp.Dec : BadGPU.StencilOp.Inc,
                    BadGPU.Compare.Less, 0, 1, 0, 0,
                    0);
    }
    // Lighting pass. Modulate R/G/B by any normal shenanigans, then add.
    public void drawLight(STHCameraSetup setup, float x, float y, float z, float r, float g, float b) {
        copyABC();
        float dstX = dX - x;
        dstX *= dstX;
        float dstY = dY - y;
        dstY *= dstY;
        float dstZ = dZ - z;
        dstZ *= dstZ;
        float dst = (float) Math.sqrt(dstX + dstY + dstZ);
        internalColourBuffer[0] = r / dst;
        internalColourBuffer[1] = g / dst;
        internalColourBuffer[2] = b / dst;
        internalColourBuffer[3] = 1;
        BadGPU.drawGeom(setup.backBuffer, setup.dsBuffer,
                BadGPU.SessionFlags.MaskRGBA, 0, 0, 0, 0,
                BadGPU.DrawFlags.DepthTest | BadGPU.DrawFlags.StencilTest |
                BadGPU.DrawFlags.FreezeColour | BadGPU.DrawFlags.Blend,
                3, internalVertexBuffer, 0, internalColourBuffer, 0, 2, null, 0,
                PrimitiveType.Triangles, 0,
                0, 3, null, 0,
                setup.matrix, 0, null, 0,
                0, 0, setup.bW, setup.bH,
                null, null, 0,
                BadGPU.Compare.Equal, 128, 255,
                BadGPU.StencilOp.Keep, BadGPU.StencilOp.Keep, BadGPU.StencilOp.Keep,
                BadGPU.Compare.Equal, 0, 1, 0, 0,
                U.BLEND_ADD);
    }
    public void setPos(int idx, float i, float j, float k) {
        if (idx == 0) {
            aX = i;
            aY = j;
            aZ = k;
        }
        if (idx == 1) {
            bX = i;
            bY = j;
            bZ = k;
        }
        if (idx == 2) {
            cX = i;
            cY = j;
            cZ = k;
        }
    }
}
