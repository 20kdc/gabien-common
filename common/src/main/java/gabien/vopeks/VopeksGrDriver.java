/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import gabien.IGrDriver;
import gabien.IImage;
import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;

/**
 * Here goes nothing.
 *
 * Created 7th June, 2023.
 */
public class VopeksGrDriver extends VopeksBatchingSurface implements IGrDriver {
    public final int[] localST = new int[6];

    /**
     * Creates a new texture for rendering, and possibly initializes it.
     */
    public VopeksGrDriver(Vopeks vopeks, int w, int h, int[] init) {
        super(vopeks, w, h, init);
        localST[4] = width;
        localST[5] = height;
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {
        blitImage(srcx, srcy, srcw, srch, x, y, i, TilingMode.None, BlendMode.Normal);
    }

    @Override
    public void blitTiledImage(int x, int y, int w, int h, IImage cachedTile) {
        blitImage(0, 0, w, h, x, y, cachedTile, TilingMode.XY, BlendMode.Normal);
    }

    public synchronized void blitImage(int srcx, int srcy, int w, int h, int x, int y, IImage i, TilingMode tiling, BlendMode blendSub) {
        x += localST[0];
        y += localST[1];
        // CPU scissor
        int cR = x + w;
        int cD = y + h;
        int srcR = srcx + w;
        int srcD = srcy + h;
        int scL = localST[2], scU = localST[3], scR = localST[4], scD = localST[5];
        if (x < scL) {
            srcx += scL - x;
            x = scL;
        }
        if (y < scU) {
            srcy += scU - y;
            y = scU;
        }
        if (cR > scR) {
            srcR -= cR - scR;
            cR = scR;
        }
        if (cD > scD) {
            srcD -= cD - scD;
            cD = scD;
        }
        if ((cR <= x) || (cD <= y))
            return;
        // The rest
        float srcWF = i.getWidth();
        float srcHF = i.getHeight();
        float s0 = srcx / srcWF;
        float s1 = srcR / srcWF;
        float t0 = srcy / srcHF;
        float t1 = srcD / srcHF;
        batchStartGroupScA(false, 6, blendSub, tiling, i);
        batchWrite(x , y , s0, t0, 1, 1, 1, 1);
        batchWrite(cR, y , s1, t0, 1, 1, 1, 1);
        batchWrite(cR, cD, s1, t1, 1, 1, 1, 1);
        batchWrite(x , y , s0, t0, 1, 1, 1, 1);
        batchWrite(cR, cD, s1, t1, 1, 1, 1, 1);
        batchWrite(x , cD, s0, t1, 1, 1, 1, 1);
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i, TilingMode.None, BlendMode.Normal);
    }

    public synchronized void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int w, int h, IImage i, TilingMode tiling, BlendMode blendSub) {
        if (srcw == w && srch == h) {
            blitImage(srcx, srcy, srcw, srch, x, y, i, tiling, blendSub);
            return;
        }
        x += localST[0];
        y += localST[1];
        // Do the CPU scissor dance, but only to work out if cropping is essential.
        // It usually isn't, and we save a ton of batches by making use of this.
        boolean isCropEssential = false;
        int cR = x + w;
        int cD = y + h;
        int srcR = srcx + srcw;
        int srcD = srcy + srch;
        int scL = localST[2], scU = localST[3], scR = localST[4], scD = localST[5];
        if (x < scL)
            isCropEssential = true;
        else if (y < scU)
            isCropEssential = true;
        else if (cR > scR)
            isCropEssential = true;
        else if (cD > scD)
            isCropEssential = true;
        // End
        float srcWF = i.getWidth();
        float srcHF = i.getHeight();
        float s0 = srcx / srcWF;
        float s1 = srcR / srcWF;
        float t0 = srcy / srcHF;
        float t1 = srcD / srcHF;
        batchStartGroupScA(isCropEssential, 6, blendSub, tiling, i);
        batchWrite(x , y , s0, t0, 1, 1, 1, 1);
        batchWrite(cR, y , s1, t0, 1, 1, 1, 1);
        batchWrite(cR, cD, s1, t1, 1, 1, 1, 1);
        batchWrite(x , y , s0, t0, 1, 1, 1, 1);
        batchWrite(cR, cD, s1, t1, 1, 1, 1, 1);
        batchWrite(x , cD, s0, t1, 1, 1, 1, 1);
    }

    @Override
    public void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i) {
        blendRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, BlendMode.Normal);
    }

    @Override
    public void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean blendSub) {
        blendRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, blendSub ? BlendMode.Subtractive : BlendMode.Additive);
    }

    public synchronized void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, BlendMode blendSub) {
        if (angle == 0) {
            blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i, TilingMode.None, blendSub);
            return;
        }
        x += localST[0];
        y += localST[1];
        // Calculate texture coordinates
        float srcWF = i.getWidth();
        float srcHF = i.getHeight();
        float s0 = srcx / srcWF;
        float s1 = (srcx + srcw) / srcWF;
        float t0 = srcy / srcHF;
        float t1 = (srcy + srch) / srcHF;
        // Calculate regular coordinates
        // Note the change of the sign. This was tested against the R48 graphics test sheet.
        double angleInRadians = Math.toRadians(-angle);
        // Sine. Can be considered xBasis.y.
        double sin = Math.sin(angleInRadians);
        // Cosine. Can be considered xBasis.x.
        double cos = Math.cos(angleInRadians);
        // Calculate basics
        float acw2 = acw / 2f;
        float ach2 = ach / 2f;
        float centreX = x + acw2;
        float centreY = y + ach2;
        float xBasisX = (float) (cos * acw2);
        float xBasisY = (float) (sin * acw2);
        float yBasisX = (float) (-sin * ach2);
        float yBasisY = (float) (cos * ach2);
        // Calculate points
        float p00X = centreX - (xBasisX + yBasisX);
        float p00Y = centreY - (xBasisY + yBasisY);
        float p10X = (centreX + xBasisX) - yBasisX;
        float p10Y = (centreY + xBasisY) - yBasisY;
        float p11X = centreX + xBasisX + yBasisX;
        float p11Y = centreY + xBasisY + yBasisY;
        float p01X = (centreX + yBasisX) - xBasisX;
        float p01Y = (centreY + yBasisY) - xBasisY;
        // Y basis is X basis rotated 90 degrees and reduced.
        batchStartGroupScA(true, 6, blendSub, TilingMode.None, i);
        batchWrite(p00X, p00Y, s0, t0, 1, 1, 1, 1);
        batchWrite(p10X, p10Y, s1, t0, 1, 1, 1, 1);
        batchWrite(p11X, p11Y, s1, t1, 1, 1, 1, 1);
        batchWrite(p00X, p00Y, s0, t0, 1, 1, 1, 1);
        batchWrite(p11X, p11Y, s1, t1, 1, 1, 1, 1);
        batchWrite(p01X, p01Y, s0, t1, 1, 1, 1, 1);
    }

    @Override
    public synchronized void clearAll(int i, int i0, int i1) {
        batchFlush();
        int cropL = localST[2];
        int cropU = localST[3];
        int cropR = localST[4];
        int cropD = localST[5];
        int cropW = cropR - cropL;
        int cropH = cropD - cropU;
        batchReferenceBarrier();
        vopeks.putTask((instance) -> {
            otrLock();
            BadGPUUnsafe.drawClear(texture.pointer, 0,
                    BadGPU.SessionFlags.MaskAll | BadGPU.SessionFlags.Scissor, cropL, cropU, cropW, cropH,
                    i / 255.0f, i0 / 255.0f, i1 / 255.0f, 1, 0, 0);
            otrUnlock();
        });
    }

    @Override
    public synchronized void clearRectAlpha(int r, int g, int b, int a, int x, int y, int w, int h) {
        x += localST[0];
        y += localST[1];
        // CPU scissor
        int cR = x + w;
        int cD = y + h;
        int scL = localST[2], scU = localST[3], scR = localST[4], scD = localST[5];
        if (x < scL)
            x = scL;
        if (y < scU)
            y = scU;
        if (cR > scR)
            cR = scR;
        if (cD > scD)
            cD = scD;
        if ((cR <= x) || (cD <= y))
            return;

        // Continue...
        float rF = r / 255f;
        float gF = g / 255f;
        float bF = b / 255f;
        float aF = a / 255f;
        batchStartGroupScA(false, 6, BlendMode.Normal, TilingMode.None, null);
        batchWrite(x , y , 0, 0, rF, gF, bF, aF);
        batchWrite(cR, y , 0, 0, rF, gF, bF, aF);
        batchWrite(cR, cD, 0, 0, rF, gF, bF, aF);
        batchWrite(x , y , 0, 0, rF, gF, bF, aF);
        batchWrite(cR, cD, 0, 0, rF, gF, bF, aF);
        batchWrite(x , cD, 0, 0, rF, gF, bF, aF);
    }

    /**
     * batchStartGroup but aware of scissoring
     */
    public void batchStartGroupScA(boolean essential, int vertices, BlendMode blendMode, TilingMode tilingMode, IImage tex) {
        int scL = localST[2], scU = localST[3], scR = localST[4], scD = localST[5];
        batchStartGroup(vertices, essential, scL, scU, scR - scL, scD - scU, blendMode, tilingMode, tex);
    }

    @Override
    public void shutdown() {
        dispose();
    }

    @Override
    public int[] getLocalST() {
        return localST;
    }

    @Override
    public void updateST() {
    }
}
