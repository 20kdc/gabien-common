/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.IGrDriver;
import gabien.IImage;
import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;
import gabien.render.ITexRegion;

/**
 * Here goes nothing.
 *
 * Created 7th June, 2023.
 */
public class VopeksGrDriver extends VopeksBatchingSurface implements IGrDriver {
    public final int[] scissor = new int[4];
    public final float[] trs = new float[4];

    /**
     * Creates a new texture for rendering, and possibly initializes it.
     */
    public VopeksGrDriver(@NonNull Vopeks vopeks, @Nullable String id, int w, int h, int[] init) {
        super(vopeks, id, w, h, init);
        trs[2] = 1;
        trs[3] = 1;
        scissor[2] = width;
        scissor[3] = height;
    }

    @Override
    public void blitImage(float srcx, float srcy, float srcw, float srch, float x, float y, ITexRegion i) {
        blitImage(srcx, srcy, srcw, srch, x, y, i, TilingMode.None, BLEND_NORMAL);
    }

    @Override
    public void blitTiledImage(float x, float y, float w, float h, IImage cachedTile) {
        blitImage(0, 0, w, h, x, y, cachedTile, TilingMode.XY, BLEND_NORMAL);
    }

    public synchronized void blitImage(float srcx, float srcy, float w, float h, float x, float y, ITexRegion i, TilingMode tiling, int blendSub) {
        if ((trs[2] != 1) || (trs[3] != 1)) {
            // scaling is in use, slowpath this
            blitScaledImageForced(srcx, srcy, w, h, x, y, w, h, i, tiling, blendSub);
            return;
        }
        // scaling not in use, so don't apply it
        x += trs[0]; y += trs[1];
        // CPU scissor
        float cR = x + w;
        float cD = y + h;
        float srcR = srcx + w;
        float srcD = srcy + h;
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
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
        float s00 = i.getS(srcx, srcy), t00 = i.getT(srcx, srcy);
        float s01 = i.getS(srcx, srcD), t01 = i.getT(srcx, srcD);
        float s10 = i.getS(srcR, srcy), t10 = i.getT(srcR, srcy);
        float s11 = i.getS(srcR, srcD), t11 = i.getT(srcR, srcD);
        batchStartGroupScA(false, false, 6, blendSub, tiling, i.getSurface());
        batchWriteXYST(x , y , s00, t00);
        batchWriteXYST(cR, y , s10, t10);
        batchWriteXYST(cR, cD, s11, t11);
        batchWriteXYST(x , y , s00, t00);
        batchWriteXYST(cR, cD, s11, t11);
        batchWriteXYST(x , cD, s01, t01);
    }

    @Override
    public void blitScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, ITexRegion i) {
        blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i, TilingMode.None, BLEND_NORMAL);
    }

    public synchronized void blitScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float w, float h, ITexRegion i, TilingMode tiling, int blendSub) {
        if (srcw == w && srch == h) {
            blitImage(srcx, srcy, srcw, srch, x, y, i, tiling, blendSub);
            return;
        }
        blitScaledImageForced(srcx, srcy, srcw, srch, x, y, w, h, i, tiling, blendSub);
    }

    private final float trsX(float x) {
        return trs[0] + (x * trs[2]);
    }

    private final float trsY(float y) {
        return trs[1] + (y * trs[3]);
    }

    public synchronized void blitScaledImageForced(float srcx, float srcy, float srcw, float srch, float x, float y, float w, float h, ITexRegion i, TilingMode tiling, int blendSub) {
        // Translate coordinates
        x = trsX(x); w *= trs[2]; y = trsY(y); h *= trs[3];
        // Do the CPU scissor dance, but only to work out if cropping is essential.
        // It usually isn't, and we save a ton of batches by making use of this.
        boolean isCropEssential = false;
        float cR = x + w;
        float cD = y + h;
        float srcR = srcx + srcw;
        float srcD = srcy + srch;
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        if (x < scL)
            isCropEssential = true;
        else if (y < scU)
            isCropEssential = true;
        else if (cR > scR)
            isCropEssential = true;
        else if (cD > scD)
            isCropEssential = true;
        // End
        float s00 = i.getS(srcx, srcy), t00 = i.getT(srcx, srcy);
        float s01 = i.getS(srcx, srcD), t01 = i.getT(srcx, srcD);
        float s10 = i.getS(srcR, srcy), t10 = i.getT(srcR, srcy);
        float s11 = i.getS(srcR, srcD), t11 = i.getT(srcR, srcD);
        batchStartGroupScA(false, isCropEssential, 6, blendSub, tiling, i.getSurface());
        batchWriteXYST(x , y , s00, t00);
        batchWriteXYST(cR, y , s10, t10);
        batchWriteXYST(cR, cD, s11, t11);
        batchWriteXYST(x , y , s00, t00);
        batchWriteXYST(cR, cD, s11, t11);
        batchWriteXYST(x , cD, s01, t01);
    }

    @Override
    public synchronized void blitRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, ITexRegion i, int blendSub) {
        if (angle == 0) {
            blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i, TilingMode.None, blendSub);
            return;
        }
        // We don't bother with regular coordinate translation here, because it wouldn't work for scaling.
        // Instead coordinate translation is done during final point calculation.
        // Calculate texture coordinates
        float srcD = srcy + srch;
        float srcR = srcx + srch;
        float s00 = i.getS(srcx, srcy), t00 = i.getT(srcx, srcy);
        float s01 = i.getS(srcx, srcD), t01 = i.getT(srcx, srcD);
        float s10 = i.getS(srcR, srcy), t10 = i.getT(srcR, srcy);
        float s11 = i.getS(srcR, srcD), t11 = i.getT(srcR, srcD);
        // Calculate regular coordinates
        // Note the change of the sign. This was tested against the R48 graphics test sheet.
        double angleInRadians = Math.toRadians(-angle);
        // Sine. Can be considered xBasis.y.
        double sin = Math.sin(angleInRadians);
        // Cosine. Can be considered xBasis.x.
        double cos = Math.cos(angleInRadians);
        // Calculate basics. Coordinate translation happens here.
        float acw2 = acw / 2f;
        float ach2 = ach / 2f;
        float centreX = trsX(x + acw2);
        float centreY = trsY(y + ach2);
        float xBasisX = (float) (cos * acw2 * trs[2]);
        float xBasisY = (float) (sin * acw2 * trs[3]);
        float yBasisX = (float) (-sin * ach2 * trs[2]);
        float yBasisY = (float) (cos * ach2 * trs[3]);
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
        batchStartGroupScA(false, true, 6, blendSub, TilingMode.None, i.getSurface());
        batchWriteXYST(p00X, p00Y, s00, t00);
        batchWriteXYST(p10X, p10Y, s10, t10);
        batchWriteXYST(p11X, p11Y, s11, t11);
        batchWriteXYST(p00X, p00Y, s00, t00);
        batchWriteXYST(p11X, p11Y, s11, t11);
        batchWriteXYST(p01X, p01Y, s01, t01);
    }

    @Override
    public synchronized void clearAll(int i, int i0, int i1) {
        batchFlush();
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        int cropW = scR - scL;
        int cropH = scD - scU;
        batchReferenceBarrier();
        vopeks.putTask((instance) -> {
            BadGPUUnsafe.drawClear(texture.pointer, 0,
                    BadGPU.SessionFlags.MaskAll | BadGPU.SessionFlags.Scissor, scL, scU, cropW, cropH,
                    i / 255.0f, i0 / 255.0f, i1 / 255.0f, 1, 0, 0);
        });
    }

    @Override
    public synchronized void clearRectAlpha(int r, int g, int b, int a, float x, float y, float w, float h) {
        // Translate coordinates
        x = trsX(x); w *= trs[2]; y = trsY(y); h *= trs[3];
        // CPU scissor
        float cR = x + w;
        float cD = y + h;
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
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
        batchStartGroupScA(true, false, 6, BLEND_NORMAL, TilingMode.None, null);
        batchWriteXYRGBA(x , y , rF, gF, bF, aF);
        batchWriteXYRGBA(cR, y , rF, gF, bF, aF);
        batchWriteXYRGBA(cR, cD, rF, gF, bF, aF);
        batchWriteXYRGBA(x , y , rF, gF, bF, aF);
        batchWriteXYRGBA(cR, cD, rF, gF, bF, aF);
        batchWriteXYRGBA(x , cD, rF, gF, bF, aF);
    }

    /**
     * batchStartGroup but aware of scissoring
     */
    public void batchStartGroupScA(boolean hasColours, boolean cropEssential, int vertices, int blendMode, TilingMode tilingMode, IImage tex) {
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        batchStartGroup(vertices, hasColours, cropEssential, scL, scU, scR - scL, scD - scU, blendMode, tilingMode, tex);
    }

    @Override
    public void shutdown() {
        dispose();
    }

    @Override
    @NonNull
    public float[] getTRS() {
        return trs;
    }

    @Override
    @NonNull
    public int[] getScissor() {
        return scissor;
    }

    @Override
    @Nullable
    public BadGPU.Texture releaseTextureCustodyFromTask() {
        BadGPU.Texture tex = texture;
        texture = null;
        return tex;
    }

    @Override
    @NonNull
    public synchronized IImage convertToImmutable(@Nullable String debugId) {
        batchFlush();
        VopeksImage res = new VopeksImage(GaBIEn.vopeks, debugId, getWidth(), getHeight(), (consumer) -> {
            GaBIEn.vopeks.putTask((instance) -> {
                consumer.accept(releaseTextureCustodyFromTask());
            });
        });
        shutdown();
        return res;
    }
}
