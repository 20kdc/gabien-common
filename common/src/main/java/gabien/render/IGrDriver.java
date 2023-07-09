/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.render;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.natives.BadGPU;
import gabien.natives.BadGPUEnum.BlendOp;
import gabien.natives.BadGPUEnum.BlendWeight;

/**
 * Represents a buffer that can be drawn to.
 * Created on 04/06/17. Made into an abstract class 9th July, 2023.
 */
public abstract class IGrDriver extends IImage {
    /**
     * Scissor control.
     */
    public final int[] scissor = new int[4];

    /**
     * Translate/scale control.
     */
    public final float[] trs = new float[4];

    /**
     * Reference list.
     */
    private final ArrayList<IImage> referencedBy = new ArrayList<>();

    public static final int BLEND_NONE = BadGPU.blendProgram(
        BlendWeight.One, BlendWeight.Zero, BlendOp.Add,
        BlendWeight.One, BlendWeight.Zero, BlendOp.Add
    );
    public static final int BLEND_NORMAL = BadGPU.blendProgram(
        BlendWeight.SrcA, BlendWeight.InvertSrcA, BlendOp.Add,
        BlendWeight.SrcA, BlendWeight.InvertSrcA, BlendOp.Add
    );
    public static final int BLEND_ADD = BadGPU.blendProgram(
        BlendWeight.One, BlendWeight.One, BlendOp.Add,
        BlendWeight.Zero, BlendWeight.One, BlendOp.Add
    );
    public static final int BLEND_SUB = BadGPU.blendProgram(
        BlendWeight.One, BlendWeight.One, BlendOp.ReverseSub,
        BlendWeight.Zero, BlendWeight.One, BlendOp.Add
    );

    // Image stuff

    public IGrDriver(int w, int h) {
        super(w, h);
        trs[2] = 1;
        trs[3] = 1;
        scissor[2] = w;
        scissor[3] = h;
    }

    /**
     * Flushes batches of things that have batches attached to this surface.
     * Call immediately before any call to putTask that writes to this surface.
     * Will be internally called before batchStartGroup.
     */
    protected final synchronized void batchReferenceBarrier() {
        while (!referencedBy.isEmpty())
            referencedBy.remove(referencedBy.size() - 1).batchFlush();
    }

    @Override
    public final synchronized void batchReference(IImage caller) {
        // If this wasn't here, the caller could refer to an unfinished batch.
        // Where this becomes a problem is that the caller could submit the task, and the batch might still not be submitted.
        // Since any changes we do make would require a flush (because we have a reference), just flush now,
        //  rather than flushing on unreference or something.
        // (Also, we could be holding a reference to caller, which implies the ability to create reference loops.)
        batchFlush();
        referencedBy.add(caller);
    }

    @Override
    public final synchronized void batchUnreference(IImage caller) {
        referencedBy.remove(caller);
    }

    // Universal interface for doing everything

    /**
     * Batches an uncoloured, textured triangle.
     * This does NOT account for scissor/translate/scale.
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     * Note that the tiling mode won't work if the STs are out of range and the region doesn't cover the whole surface. 
     */
    public abstract void batchXYST(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2);

    /**
     * Batches an uncoloured, textured quad (012023).
     * This does NOT account for scissor/translate/scale.
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     * Note that the tiling mode won't work if the STs are out of range and the region doesn't cover the whole surface. 
     */
    public abstract void batchXYST(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2, float x3, float y3, float s3, float t3);

    /**
     * Batches a coloured, untextured quad.
     * This does NOT account for scissor/translate/scale.
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    public abstract void batchXYRGBA(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float r0, float g0, float b0, float a0, float x1, float y1, float r1, float g1, float b1, float a1, float x2, float y2, float r2, float g2, float b2, float a2, float x3, float y3, float r3, float g3, float b3, float a3);

    // Universal interface, accounting for scissoring

    /**
     * batchXYST-3 but auto-fills crop fields based on scissoring.
     */
    public final synchronized void batchXYSTScA(boolean cropEssential, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion tex, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2) {
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        batchXYST(cropEssential, scL, scU, scR - scL, scD - scU, blendMode, tilingMode, tex, x0, y0, s0, t0, x1, y1, s1, t1, x2, y2, s2, t2);
    }

    /**
     * batchXYST-4 but auto-fills crop fields based on scissoring.
     */
    public final synchronized void batchXYSTScA(boolean cropEssential, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion tex, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2, float x3, float y3, float s3, float t3) {
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        batchXYST(cropEssential, scL, scU, scR - scL, scD - scU, blendMode, tilingMode, tex, x0, y0, s0, t0, x1, y1, s1, t1, x2, y2, s2, t2, x3, y3, s3, t3);
    }

    /**
     * batchXYRGBA-4 but auto-fills crop fields based on scissoring.
     */
    public final synchronized void batchXYRGBAScA(boolean cropEssential, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion tex, float x0, float y0, float r0, float g0, float b0, float a0, float x1, float y1, float r1, float g1, float b1, float a1, float x2, float y2, float r2, float g2, float b2, float a2, float x3, float y3, float r3, float g3, float b3, float a3) {
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        batchXYRGBA(cropEssential, scL, scU, scR - scL, scD - scU, blendMode, tilingMode, tex, x0, y0, r0, g0, b0, a0, x1, y1, r1, g1, b1, a1, x2, y2, r2, g2, b2, a2, x3, y3, r3, g3, b3, a3);
    }

    // Basic blit operations.
    public final void blitImage(float srcx, float srcy, float srcw, float srch, float x, float y, IReplicatedTexRegion i) {
        blitImage(srcx, srcy, srcw, srch, x, y, i, TilingMode.None, BLEND_NORMAL);
    }

    public final void blitTiledImage(float x, float y, float w, float h, IImage cachedTile) {
        blitImage(0, 0, w, h, x, y, cachedTile, TilingMode.XY, BLEND_NORMAL);
    }

    public final synchronized void blitImage(float srcx, float srcy, float w, float h, float x, float y, IReplicatedTexRegion iU, TilingMode tiling, int blendSub) {
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

    public final void blitScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, IReplicatedTexRegion i) {
        blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i, TilingMode.None, BLEND_NORMAL);
    }

    public final synchronized void blitScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float w, float h, IReplicatedTexRegion i, TilingMode tiling, int blendSub) {
        if (srcw == w && srch == h) {
            blitImage(srcx, srcy, srcw, srch, x, y, i, tiling, blendSub);
            return;
        }
        blitScaledImageForced(srcx, srcy, srcw, srch, x, y, w, h, i, tiling, blendSub);
    }

    protected final float trsX(float x) {
        return trs[0] + (x * trs[2]);
    }

    protected final float trsY(float y) {
        return trs[1] + (y * trs[3]);
    }

    public final synchronized void blitScaledImageForced(float srcx, float srcy, float srcw, float srch, float x, float y, float w, float h, IReplicatedTexRegion iU, TilingMode tiling, int blendSub) {
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

    /**
     * This operation can be considered the logical basis of all the blit-series operations in IGrDriver.
     * (This isn't how it actually works ever since Vopeks, but it's close. Mainly, tiled acts differently.)
     * Importantly, here's how rotation is applied:
     * Firstly, the image is placed as if no rotation were involved.
     * Then the destination is rotated anticlockwise by angle degrees.
     */
    public final synchronized void blitRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, IReplicatedTexRegion iU, int blendSub) {
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

    public final void blitScaledImage(float x, float y, float acw, float ach, IReplicatedTexRegion i) {
        blitScaledImage(0, 0, i.getRegionWidth(), i.getRegionHeight(), x, y, acw, ach, i);
    }

    /**
     * Legacy interface. This is just inherently awkward.
     */
    public final void blendRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, IReplicatedTexRegion i, boolean blendSub) {
        blitRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, blendSub ? BLEND_SUB : BLEND_ADD);
    }

    public final void blitRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, IReplicatedTexRegion i) {
        blitRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, BLEND_NORMAL);
    }

    public abstract void clearAll(int i, int i0, int i1);

    public final void clearRect(int r, int g, int b, float x, float y, float width, float height) {
        clearRectAlpha(r, g, b, 255, x, y, width, height);
    }

    public final synchronized void clearRectAlpha(int r, int g, int b, int a, float x, float y, float w, float h) {
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
     * Stop all drawing operations. Makes an OsbDriver unusable.
     */
    public abstract void shutdown();

    /**
     * Gets the local scissor buffer, in the order: cropL, cropU, cropR, cropD
     * The crop coordinates are independent of the translation coordinates.
     */
    public final @NonNull int[] getScissor() {
        return scissor;
    }

    /**
     * Gets the local translate/scale buffer, in the order: tX, tY, sX, sY
     * The translation is independent of the scale.
     */
    public final @NonNull float[] getTRS() {
        return trs;
    }

    /**
     * Translates TRS X, dependent on scale.
     * Returns old value.
     */
    public final float trsTXS(float x) {
        float old = trs[0];
        trs[0] += x * trs[2];
        return old;
    }

    /**
     * Translates TRS Y, dependent on scale.
     * Returns old value.
     */
    public final float trsTYS(float y) {
        float old = trs[1];
        trs[1] += y * trs[3];
        return old;
    }

    /**
     * Restores TRS X.
     */
    public final void trsTXE(float x) {
        trs[0] = x;
    }

    /**
     * Restores TRS Y.
     */
    public final void trsTYE(float y) {
        trs[1] = y;
    }

    /**
     * Restores TRS Y.
     */
    public final void trsTXYE(float x, float y) {
        trs[0] = x;
        trs[1] = y;
    }

    /**
     * Ok, so this is a particularly evil little call.
     * This is like getTextureFromTask, but it releases custody of the texture.
     * This irrevocably alters the image (to being non-existent).
     * As such, you can only do this on an IGrDriver.
     */
    public abstract @Nullable BadGPU.Texture releaseTextureCustodyFromTask();

    /**
     * This converts an IGrDriver into an immutable image.
     * Data-wise, this is an in-place operation and shuts down the IGrDriver.
     */
    public abstract @NonNull IImage convertToImmutable(@Nullable String debugId);

    public enum TilingMode {
        None(0),
        X(BadGPU.DrawFlags.WrapS),
        Y(BadGPU.DrawFlags.WrapT),
        XY(BadGPU.DrawFlags.WrapS | BadGPU.DrawFlags.WrapT);

        public final int badgpuValue;

        TilingMode(int v) {
            badgpuValue = v;
        }
    }
}
