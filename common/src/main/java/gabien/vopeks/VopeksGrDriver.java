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
import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;
import gabien.render.IGrDriver;
import gabien.render.IImage;
import gabien.render.IReplicatedTexRegion;

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
    public void blitImage(float srcx, float srcy, float srcw, float srch, float x, float y, IReplicatedTexRegion i) {
        blitImage(srcx, srcy, srcw, srch, x, y, i, TilingMode.None, BLEND_NORMAL);
    }

    @Override
    public void blitTiledImage(float x, float y, float w, float h, IImage cachedTile) {
        blitImage(0, 0, w, h, x, y, cachedTile, TilingMode.XY, BLEND_NORMAL);
    }

    public synchronized void blitImage(float srcx, float srcy, float w, float h, float x, float y, IReplicatedTexRegion iU, TilingMode tiling, int blendSub) {
        if ((trs[2] != 1) || (trs[3] != 1)) {
            // scaling is in use, slowpath this
            blitScaledImageForced(srcx, srcy, w, h, x, y, w, h, iU, tiling, blendSub);
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
        batchXYSTScA(false, blendSub, tiling, iU,
            x , y , srcx, srcy,
            cR, y , srcR, srcy,
            cR, cD, srcR, srcD,
            x , cD, srcx, srcD
        );
    }

    @Override
    public void blitScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, IReplicatedTexRegion i) {
        blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i, TilingMode.None, BLEND_NORMAL);
    }

    public synchronized void blitScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float w, float h, IReplicatedTexRegion i, TilingMode tiling, int blendSub) {
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

    public synchronized void blitScaledImageForced(float srcx, float srcy, float srcw, float srch, float x, float y, float w, float h, IReplicatedTexRegion iU, TilingMode tiling, int blendSub) {
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
        batchXYSTScA(isCropEssential, blendSub, tiling, iU,
            x , y , srcx, srcy,
            cR, y , srcR, srcy,
            cR, cD, srcR, srcD,
            x , cD, srcx, srcD
        );
    }

    @Override
    public synchronized void blitRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, IReplicatedTexRegion iU, int blendSub) {
        if (angle == 0) {
            blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, iU, TilingMode.None, blendSub);
            return;
        }
        // We don't bother with regular coordinate translation here, because it wouldn't work for scaling.
        // Instead coordinate translation is done during final point calculation.
        // Calculate texture coordinates
        float srcD = srcy + srch;
        float srcR = srcx + srch;
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
        batchXYSTScA(true, blendSub, TilingMode.None, iU,
            p00X, p00Y, srcx, srcy,
            p10X, p10Y, srcR, srcy,
            p11X, p11Y, srcR, srcD,
            p01X, p01Y, srcx, srcD
        );
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
        batchXYRGBAScA(false, BLEND_NORMAL, TilingMode.None, null,
            x , y , rF, gF, bF, aF,
            cR, y , rF, gF, bF, aF,
            cR, cD, rF, gF, bF, aF,
            x , cD, rF, gF, bF, aF
        );
    }

    /**
     * batchXYST-3 but aware of scissoring
     */
    public final synchronized void batchXYSTScA(boolean cropEssential, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion tex, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2) {
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        batchXYST(cropEssential, scL, scU, scR - scL, scD - scU, blendMode, tilingMode, tex, x0, y0, s0, t0, x1, y1, s1, t1, x2, y2, s2, t2);
    }

    /**
     * batchXYST-4 but aware of scissoring
     */
    public final synchronized void batchXYSTScA(boolean cropEssential, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion tex, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2, float x3, float y3, float s3, float t3) {
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        batchXYST(cropEssential, scL, scU, scR - scL, scD - scU, blendMode, tilingMode, tex, x0, y0, s0, t0, x1, y1, s1, t1, x2, y2, s2, t2, x3, y3, s3, t3);
    }

    /**
     * batchXYRGBA-3 but aware of scissoring
     */
    public final synchronized void batchXYRGBAScA(boolean cropEssential, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion tex, float x0, float y0, float r0, float g0, float b0, float a0, float x1, float y1, float r1, float g1, float b1, float a1, float x2, float y2, float r2, float g2, float b2, float a2, float x3, float y3, float r3, float g3, float b3, float a3) {
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        batchXYRGBA(cropEssential, scL, scU, scR - scL, scD - scU, blendMode, tilingMode, tex, x0, y0, r0, g0, b0, a0, x1, y1, r1, g1, b1, a1, x2, y2, r2, g2, b2, a2, x3, y3, r3, g3, b3, a3);
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
