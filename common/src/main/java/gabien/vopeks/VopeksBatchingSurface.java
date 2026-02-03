/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Texture;
import gabien.natives.BadGPUUnsafe;
import gabien.render.IGrDriver;
import gabien.render.IImage;
import gabien.render.ITexRegion;
import gabien.render.IImgRegion;

/**
 * IGrDriver implements nice wrappers on top of these core operations.
 * BEWARE: The batching methods are unsynchronized, except batchFlush (because it's externally called).
 * Use them in synchronized blocks or something, please.
 *
 * Created 7th June, 2023.
 */
public final class VopeksBatchingSurface extends IGrDriver {
    /**
     * The parent instance.
     */
    public final Vopeks vopeks;

    private volatile boolean wasDisposed;

    private final VopeksBatchPool batchPool;
    private @Nullable VopeksBatch currentBatch = null;
    private final int maxVerticesInBatch;
    private final float[] stagingV;
    private final float[] stagingC;
    private final float[] stagingT;
    private final float halfWF, halfHF;

    /**
     * State of the crop registers for the group that is being prepared right now.
     * Use only in sync.
     */
    private int upcomingCropL, upcomingCropU, upcomingCropR, upcomingCropD;

    /**
     * Creates a new texture for rendering, and possibly initializes it.
     */
    public VopeksBatchingSurface(Vopeks vopeks, @Nullable String id, int w, int h, @Nullable int[] init, int maxVerticesInBatch) {
        super(id, w, h);
        this.vopeks = vopeks;
        batchPool = new VopeksBatchPool(vopeks, this, 1);
        if (maxVerticesInBatch < 6)
            throw new RuntimeException("To function properly, there must be at least 6 vertices supported per batch.");
        this.maxVerticesInBatch = maxVerticesInBatch;
        stagingV = new float[maxVerticesInBatch * 4];
        stagingC = new float[maxVerticesInBatch * 4];
        stagingT = new float[maxVerticesInBatch * 4];
        vopeks.putTask((instance) -> {
            texture = instance.newTexture(w, h, BadGPU.TextureLoadFormat.ARGBI32_SA, init, 0);
        });
        halfWF = w / 2.0f;
        halfHF = h / 2.0f;
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull int[] data, int dataOfs, @NonNull Runnable onDone) {
        VopeksImage.getPixelsAsync(vopeks, this, x, y, w, h, format, data, dataOfs, onDone);
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull byte[] data, int dataOfs, @NonNull Runnable onDone) {
        VopeksImage.getPixelsAsync(vopeks, this, x, y, w, h, format, data, dataOfs, onDone);
    }

    @Override
    public synchronized void clearAll(int r, int g, int b, int a) {
        batchFlush();
        int scL = scissor[0], scU = scissor[1], scR = scissor[2], scD = scissor[3];
        int cropW = scR - scL;
        int cropH = scD - scU;
        batchReferenceBarrier();
        vopeks.putTask((instance) -> {
            Texture tx = texture;
            if (tx != null)
                BadGPUUnsafe.drawClear(tx.pointer, 0,
                        BadGPU.SessionFlags.MaskRGBA | BadGPU.SessionFlags.Scissor, scL, scU, cropW, cropH,
                        r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f, 0, 0);
        });
    }

    @Override
    public synchronized void generateMipmap() {
        batchFlush();
        batchReferenceBarrier();
        vopeks.putTask((instance) -> {
            Texture tx = texture;
            if (tx != null)
                BadGPUUnsafe.generateMipmap(tx.pointer);
        });
    }

    @Override
    public synchronized void shutdown() {
        if (!wasDisposed) {
            wasDisposed = true;
            // We're about to dispose, so clean up references
            batchReferenceBarrier();
            // This is important! Otherwise, we leak batch resources.
            batchFlush();
            vopeks.putTask((instance) -> {
                if (texture != null) {
                    texture.dispose();
                    texture = null;
                }
            });
        }
    }

    @Override
    protected void finalize() {
        shutdown();
    }

    /**
     * Initializes crop registers and returns true to mean invalid.
     */
    private final boolean setupAndCheckCrop(boolean cropEssential, int cropL, int cropU, int cropR, int cropD) {
        cropL = cropL < 0 ? 0 : (cropL > width ? width : cropL);
        cropR = cropR < 0 ? 0 : (cropR > width ? width : cropR);
        cropU = cropU < 0 ? 0 : (cropU > height ? height : cropU);
        cropD = cropD < 0 ? 0 : (cropD > height ? height : cropD);
        if (cropEssential && (cropR <= cropL || cropD <= cropU))
            return true;
        upcomingCropL = cropL;
        upcomingCropU = cropU;
        upcomingCropR = cropR;
        upcomingCropD = cropD;
        return false;
    }

    /**
     * Batches an uncoloured, textured triangle.
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    @Override
    public final synchronized void rawBatchXYST(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, int drawFlagsEx, @Nullable ITexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2) {
        if (setupAndCheckCrop(cropEssential, cropL, cropU, cropR, cropD))
            return;
        IImgRegion tex = batchStartGroup(3, false, cropEssential, blendMode, drawFlagsEx, iU);
        batchWriteXYSTRGBA(x0, y0, s0, t0, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x1, y1, s1, t1, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, 1, 1, 1, 1, tex);
    }

    /**
     * Batches an uncoloured, textured quad (012023).
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    @Override
    public final synchronized void rawBatchXYST(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, int drawFlagsEx, @Nullable ITexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2, float x3, float y3, float s3, float t3) {
        if (setupAndCheckCrop(cropEssential, cropL, cropU, cropR, cropD))
            return;
        IImgRegion tex = batchStartGroup(6, false, cropEssential, blendMode, drawFlagsEx, iU);
        batchWriteXYSTRGBA(x0, y0, s0, t0, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x1, y1, s1, t1, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x0, y0, s0, t0, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x3, y3, s3, t3, 1, 1, 1, 1, tex);
    }

    /**
     * Batches a coloured, textured triangle.
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    @Override
    public final synchronized void rawBatchXYSTRGBA(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, int drawFlagsEx, @Nullable ITexRegion iU, float x0, float y0, float s0, float t0, float r0, float g0, float b0, float a0, float x1, float y1, float s1, float t1, float r1, float g1, float b1, float a1, float x2, float y2, float s2, float t2, float r2, float g2, float b2, float a2) {
        if (setupAndCheckCrop(cropEssential, cropL, cropU, cropR, cropD))
            return;
        IImgRegion tex = batchStartGroup(3, true, cropEssential, blendMode, drawFlagsEx, iU);
        batchWriteXYSTRGBA(x0, y0, s0, t0, r0, g0, b0, a0, tex);
        batchWriteXYSTRGBA(x1, y1, s1, t1, r1, g1, b1, a1, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, r2, g2, b2, a2, tex);
    }

    /**
     * Batches a coloured, textured quad (012023).
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    @Override
    public final synchronized void rawBatchXYSTRGBA(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, int drawFlagsEx, @Nullable ITexRegion iU, float x0, float y0, float s0, float t0, float r0, float g0, float b0, float a0, float x1, float y1, float s1, float t1, float r1, float g1, float b1, float a1, float x2, float y2, float s2, float t2, float r2, float g2, float b2, float a2, float x3, float y3, float s3, float t3, float r3, float g3, float b3, float a3) {
        if (setupAndCheckCrop(cropEssential, cropL, cropU, cropR, cropD))
            return;
        IImgRegion tex = batchStartGroup(6, true, cropEssential, blendMode, drawFlagsEx, iU);
        batchWriteXYSTRGBA(x0, y0, s0, t0, r0, g0, b0, a0, tex);
        batchWriteXYSTRGBA(x1, y1, s1, t1, r1, g1, b1, a1, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, r2, g2, b2, a2, tex);
        batchWriteXYSTRGBA(x0, y0, s0, t0, r0, g0, b0, a0, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, r2, g2, b2, a2, tex);
        batchWriteXYSTRGBA(x3, y3, s3, t3, r3, g3, b3, a3, tex);
    }

    /**
     * Ensures the batcher is in the right state to accept the given geometry.
     * This will actually begin a new batch, so make sure you're sure!
     * cropEssential being false implies that the scissor bounds can't be more cropped than this, but can be less.
     * This function returns the batching-optimal ITexRegion picked if any.
     */
    private @Nullable IImgRegion batchStartGroup(int vertices, boolean hasColours, boolean cropEssential, int blendMode, int drawFlagsEx, @Nullable ITexRegion iU) {
        // Presumably, other user calls to other surfaces may have been made between groups.
        // We can assume that as long as we remain internally consistent:
        // Other threads aren't a concern in terms of the reference timeline.
        // (Whenever we consider them to have been referenced is a time they could theoretically have hit.)
        // But we need to ensure that we split batches if, say:
        //  SURFACE A/GROUP A -> SURFACE B/GROUP A -> SURFACE A/GROUP B -> SURFACE A/FLUSH
        //  happens and surface-B-group-A depends on surface-A-group-A but not surface-A-group-B.
        // Therefore, we have to reference barrier when starting a group.
        // If we delayed until the flush, then surface-A-flush would be the point where surface-B-group-A is notified,
        //  and by that point it's too late to split the two groups.
        batchReferenceBarrier();
        VopeksBatch batch = currentBatch;
        if (batch != null)
            if ((batch.vertexCount + vertices) > maxVerticesInBatch) {
                batchFlush();
                batch = null;
            }
        // ok, so now that the current batch is dealt with, do the pick here
        IImgRegion tex = null;
        IImage srf = null;
        if (iU != null)
            tex = iU.pickImgRegion(batch != null ? batch.tex : null);
        if (tex != null)
            srf = tex.getSurface();
        // calculate this here so that it can be pushed forward if necessary
        // in particular matchesState may be happier if this is pushed all the way...
        batch = currentBatch;
        if (batch == null || !batch.matchesState(cropEssential, upcomingCropL, upcomingCropU, upcomingCropR, upcomingCropD, blendMode, drawFlagsEx, srf)) {
            batchFlush();
            batch = null;
            // Setup the reference.
            // Note that we only have to worry about this at the start of a batch.
            // If something happens, it'll reference-barrier, which will flush us, so we'll re-reference next group.
            if (srf != null)
                srf.batchReference(this);
            batch = currentBatch = batchPool.get();
            batch.hasColours = hasColours;
            batch.cropEssential = cropEssential;
            if (cropEssential) {
                batch.cropL = upcomingCropL;
                batch.cropU = upcomingCropU;
                batch.cropR = upcomingCropR;
                batch.cropD = upcomingCropD;
            } else {
                batch.cropL = 0;
                batch.cropU = 0;
                batch.cropR = width;
                batch.cropD = height;
            }
            batch.blendMode = blendMode;
            batch.drawFlagsEx = drawFlagsEx;
            batch.tex = srf;
        }
        if (hasColours && !batch.hasColours) {
            // upgrade batch to having colours
            Arrays.fill(stagingC, 0, batch.vertexCount * 4, 1.0f);
            batch.hasColours = true;
        }
        return tex;
    }

    @Override
    public synchronized void batchFlush() {
        // Now actually do the batching thing
        VopeksBatch batch = currentBatch;
        currentBatch = null;
        if (batch == null)
            return;
        IImage batchTex = batch.tex;
        // Sizes
        int groupVLen = batch.vertexCount * 2;
        int groupCLen = batch.vertexCount * 4;
        int groupTLen = batchTex != null ? (batch.vertexCount * 2) : 0;
        int groupTotalLen = groupVLen + groupCLen + groupTLen;

        // Layout
        float[] megabuffer = vopeks.floatPool.get(groupTotalLen);

        int groupVOfs = 0;
        int groupCOfs = groupVOfs + groupVLen;
        int groupTOfs = groupCOfs + groupCLen;

        batch.megabuffer = megabuffer;
        batch.verticesOfs = groupVOfs;
        batch.coloursOfs = groupCOfs;
        batch.texCoordsOfs = groupTOfs;

        // Copy
        System.arraycopy(stagingV, 0, megabuffer, groupVOfs, groupVLen);
        System.arraycopy(stagingC, 0, megabuffer, groupCOfs, groupCLen);
        if (batchTex != null)
            System.arraycopy(stagingT, 0, megabuffer, groupTOfs, groupTLen);

        // Put
        vopeks.putTask(batch);

        // And that's the deadline hit...
        if (batchTex != null)
            batchTex.batchUnreference(this);
    }

    /**
     * Writes a vertex to the batcher.
     * For ease of use, X/Y coordinates are converted to the -1 to 1 representation here.
     */
    private void batchWriteXYSTRGBA(float x, float y, float s, float t, float r, float g, float b, float a, @Nullable IImgRegion tf) {
        @SuppressWarnings("null")
        @NonNull VopeksBatch batch = currentBatch;
        int vertexBase2 = batch.vertexCount * 2;
        int vertexBase4 = batch.vertexCount * 4;
        stagingV[vertexBase2] = (x - halfWF) / halfWF;
        stagingV[vertexBase2 + 1] = (y - halfHF) / halfHF;
        if (tf != null) {
            float nS = tf.getS(s, t);
            float nT = tf.getT(s, t);
            stagingT[vertexBase2] = nS;
            stagingT[vertexBase2 + 1] = nT;
        }
        if (batch.hasColours) {
            stagingC[vertexBase4] = r;
            stagingC[vertexBase4 + 1] = g;
            stagingC[vertexBase4 + 2] = b;
            stagingC[vertexBase4 + 3] = a;
        }
        batch.vertexCount++;
    }
}
