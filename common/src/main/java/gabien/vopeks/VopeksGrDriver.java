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
        blitScaledImage(srcx, srcy, srcw, srch, x, y, srcw, srch, i, TilingMode.None, BlendMode.Normal);
    }

    @Override
    public void blitTiledImage(int x, int y, int w, int h, IImage cachedTile) {
        blitScaledImage(0, 0, w, h, x, y, w, h, cachedTile, TilingMode.XY, BlendMode.Normal);
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i, TilingMode.None, BlendMode.Normal);
    }

    public synchronized void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int w, int h, IImage i, TilingMode tiling, BlendMode blendSub) {
        x += localST[0];
        y += localST[1];
        float srcWF = i.getWidth();
        float srcHF = i.getHeight();
        float s0 = srcx / srcWF;
        float s1 = (srcx + srcw) / srcWF;
        float t0 = srcy / srcHF;
        float t1 = (srcy + srch) / srcHF;
        batchStartGroupScA(6, blendSub, TilingMode.None, i);
        batchWrite(x    , y    , s0, t0, 1, 1, 1, 1);
        batchWrite(x + w, y    , s1, t0, 1, 1, 1, 1);
        batchWrite(x + w, y + h, s1, t1, 1, 1, 1, 1);
        batchWrite(x    , y    , s0, t0, 1, 1, 1, 1);
        batchWrite(x + w, y + h, s1, t1, 1, 1, 1, 1);
        batchWrite(x    , y + h, s0, t1, 1, 1, 1, 1);
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
        batchStartGroupScA(6, blendSub, TilingMode.None, i);
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
            BadGPU.drawClear(texture, null,
                    BadGPU.SessionFlags.MaskAll | BadGPU.SessionFlags.Scissor, cropL, cropU, cropW, cropH,
                    i / 255.0f, i0 / 255.0f, i1 / 255.0f, 1, 0, 0);
        });
    }

    @Override
    public synchronized void clearRectAlpha(int r, int g, int b, int a, int x, int y, int w, int h) {
        x += localST[0];
        y += localST[1];
        float rF = r / 255f;
        float gF = g / 255f;
        float bF = b / 255f;
        float aF = a / 255f;
        batchStartGroupScA(6, BlendMode.Normal, TilingMode.None, null);
        batchWrite(x    , y    , 0, 0, rF, gF, bF, aF);
        batchWrite(x + w, y    , 0, 0, rF, gF, bF, aF);
        batchWrite(x + w, y + h, 0, 0, rF, gF, bF, aF);
        batchWrite(x    , y    , 0, 0, rF, gF, bF, aF);
        batchWrite(x + w, y + h, 0, 0, rF, gF, bF, aF);
        batchWrite(x    , y + h, 0, 0, rF, gF, bF, aF);
    }

    /**
     * batchStartGroup but aware of scissoring
     */
    public void batchStartGroupScA(int vertices, BlendMode blendMode, TilingMode tilingMode, IImage tex) {
        batchStartGroup(vertices, localST[2], localST[3], localST[4] - localST[2], localST[5] - localST[3], blendMode, tilingMode, tex);
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
