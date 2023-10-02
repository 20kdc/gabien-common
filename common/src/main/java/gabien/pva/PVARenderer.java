/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.pva;

import gabien.GaBIEn;
import gabien.natives.BadGPU;
import gabien.render.IGrDriver;
import gabien.render.IImage;
import gabien.vopeks.VopeksImage;

/**
 * Covers common functionality for any GaBIEn PVA renderer.
 * Specifically, image/texture creation/setup.
 * Created 2nd October 2023.
 */
public class PVARenderer {
    public final PVAFile pvaFile;
    public final VopeksImage[] images;

    public PVARenderer(PVAFile pva) {
        pvaFile = pva;
        images = new VopeksImage[pva.imageHeaders.length];
        for (int i = 0; i < images.length; i++) {
            images[i] = new VopeksImage(GaBIEn.vopeks, "PVAImage" + i, pva.imageHeaders[i].w, pva.imageHeaders[i].h, BadGPU.TextureLoadFormat.RGBA8888, pva.imageDatas[i]);
        }
    }

    /**
     * The inline PVA renderer renders via IGrDriver.
     * Thus, repetitive batching and such is possible.
     * However, it has no depth buffer and no perspective correction.
     */
    public void renderInline(PVAFile.FrameElm[] frame, IGrDriver driver, float x, float y, float width, float height) {
        float wd2 = width / 2;
        float hd2 = height / 2;
        float txs = driver.trsTXS(x + wd2);
        float tys = driver.trsTYS(y + hd2);
        float sxs = driver.trsSXS(wd2);
        float sys = driver.trsSYS(hd2);
        {
            // Within this block, drawing uses the -1 to 1 coordinate system.
            for (PVAFile.FrameElm fe : frame) {
                PVAFile.Matrix mtx = pvaFile.matrices[fe.mtxIndex];
                PVAFile.Triangle tri = pvaFile.triangles[fe.triIndex];
                PVAFile.Loop loopA = pvaFile.loops[tri.aIndex];
                PVAFile.Loop loopB = pvaFile.loops[tri.bIndex];
                PVAFile.Loop loopC = pvaFile.loops[tri.cIndex];
                PVAFile.Vertex vA = pvaFile.vertices[loopA.vtxIndex];
                PVAFile.Vertex vB = pvaFile.vertices[loopB.vtxIndex];
                PVAFile.Vertex vC = pvaFile.vertices[loopC.vtxIndex];
                // vertex transform 0
                float w0 = mtx.transformVertexW(vA);
                if (w0 <= 0)
                    continue;
                float x0 = mtx.transformVertexX(vA) / w0;
                float y0 = mtx.transformVertexY(vA) / w0;
                float z0 = mtx.transformVertexZ(vA) / w0;
                if (z0 < -1)
                    continue;
                // vertex transform 1
                float w1 = mtx.transformVertexW(vB);
                if (w1 <= 0)
                    continue;
                float x1 = mtx.transformVertexX(vB) / w1;
                float y1 = mtx.transformVertexY(vB) / w1;
                float z1 = mtx.transformVertexZ(vB) / w1;
                if (z1 < -1)
                    continue;
                // vertex transform 2
                float w2 = mtx.transformVertexW(vC);
                if (w2 <= 0)
                    continue;
                float x2 = mtx.transformVertexX(vC) / w2;
                float y2 = mtx.transformVertexY(vC) / w2;
                float z2 = mtx.transformVertexZ(vC) / w2;
                if (z2 < -1)
                    continue;
                // triangle in range, prep final details
                PVAFile.PaletteElm pA = pvaFile.palette[loopA.palIndex];
                PVAFile.PaletteElm pB = pvaFile.palette[loopB.palIndex];
                PVAFile.PaletteElm pC = pvaFile.palette[loopC.palIndex];
                IImage img = null;
                int blendMode = IGrDriver.BLEND_NONE;//IGrDriver.BLEND_NORMAL;
                int drawFlagsEx = 0;
                float sM = 1;
                float tM = 1;
                if (tri.texIndex != -1) {
                    PVAFile.Texture tex = pvaFile.textures[tri.texIndex];
                    img = images[tex.imgIndex];
                    if ((tex.mode & PVAFile.Texture.MODE_FILTER) != 0)
                        drawFlagsEx |= BadGPU.DrawFlags.MagLinear | BadGPU.DrawFlags.MinLinear;
                    if ((tex.mode & PVAFile.Texture.MODE_REPEAT) != 0)
                        drawFlagsEx |= BadGPU.DrawFlags.WrapS | BadGPU.DrawFlags.WrapT;
                    sM *= img.width;
                    tM *= img.height;
                }
                // draw
                driver.drawXYSTRGBA(blendMode, drawFlagsEx, img,
                        x0, -y0, loopA.u * sM, loopA.v * tM, pA.r, pA.g, pA.b, pA.a,
                        x1, -y1, loopB.u * sM, loopB.v * tM, pB.r, pB.g, pB.b, pB.a,
                        x2, -y2, loopC.u * sM, loopC.v * tM, pC.r, pC.g, pC.b, pC.a);
            }
        }
        driver.trsSXYE(sxs, sys);
        driver.trsTXYE(txs, tys);
    }
}
