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

import gabien.GaBIEn;
import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;
import gabien.natives.BadGPU.Instance;
import gabien.natives.BadGPU.Texture;
import gabien.render.IGrDriver;
import gabien.render.IImage;
import gabien.render.IReplicatedTexRegion;
import gabien.render.ITexRegion;
import gabien.uslx.append.ObjectPool;
import gabien.vopeks.Vopeks.ITask;

/**
 * This is a parent of VopeksGrDriver to separate out the batching code.
 * At some point it might be nice for IGrDriver to use default methods on top of this.
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

    /**
     * The texture.
     * This is only guaranteed to exist on the instance thread.
     */
    protected BadGPU.Texture texture;

    /**
     * ID for debugging.
     */
    public final @NonNull String debugId;

    private static final int MAX_VERTICES_IN_BATCH = 65536;
    private final BatchPool batchPool = new BatchPool(1);
    private Batch currentBatch = null;
    private final float[] stagingV = new float[MAX_VERTICES_IN_BATCH * 4];
    private final float[] stagingC = new float[MAX_VERTICES_IN_BATCH * 4];
    private final float[] stagingT = new float[MAX_VERTICES_IN_BATCH * 4];
    private final float halfWF, halfHF;

    /**
     * Creates a new texture for rendering, and possibly initializes it.
     */
    public VopeksBatchingSurface(@NonNull Vopeks vopeks, @Nullable String id, int w, int h, int[] init) {
        super(w, h);
        this.vopeks = vopeks;
        debugId = id == null ? super.toString() : (super.toString() + ":" + id);
        vopeks.putTask((instance) -> {
            // DO NOT REMOVE BadGPU.TextureFlags.HasAlpha
            // NOT HAVING ALPHA KILLS PERF. ON ANDROID FOR SOME REASON.
            texture = instance.newTexture(w, h, BadGPU.TextureLoadFormat.ARGBI32, init, 0);
        });
        halfWF = w / 2.0f;
        halfHF = h / 2.0f;
    }

    @Override
    public String toString() {
        return debugId;
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
    public Texture getTextureFromTask() {
        return texture;
    }

    @Override
    public void shutdown() {
        dispose();
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

    @Override
    protected void finalize() {
        dispose();
    }

    public synchronized void dispose() {
        if (!wasDisposed) {
            wasDisposed = true;
            // We're about to dispose, so clean up references
            batchReferenceBarrier();
            // This is important! Otherwise, we leak batch resources.
            batchFlush();
            vopeks.putTask((instance) -> {
                if (texture != null)
                    texture.dispose();
            });
        }
    }

    /**
     * Batches an uncoloured, textured triangle.
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    public final synchronized void batchXYST(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2) {
        ITexRegion tex = batchStartGroup(3, false, cropEssential, cropL, cropU, cropW, cropH, blendMode, tilingMode, iU);
        batchWriteXYSTRGBA(x0, y0, s0, t0, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x1, y1, s1, t1, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, 1, 1, 1, 1, tex);
    }

    /**
     * Batches an uncoloured, textured quad (012023).
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    public final synchronized void batchXYST(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2, float x3, float y3, float s3, float t3) {
        ITexRegion tex = batchStartGroup(6, false, cropEssential, cropL, cropU, cropW, cropH, blendMode, tilingMode, iU);
        batchWriteXYSTRGBA(x0, y0, s0, t0, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x1, y1, s1, t1, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x0, y0, s0, t0, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, 1, 1, 1, 1, tex);
        batchWriteXYSTRGBA(x3, y3, s3, t3, 1, 1, 1, 1, tex);
    }

    /**
     * Batches a coloured, untextured triangle.
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    public final void batchXYRGBA(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float r0, float g0, float b0, float a0, float x1, float y1, float r1, float g1, float b1, float a1, float x2, float y2, float r2, float g2, float b2, float a2) {
        batchXYSTRGBA(cropEssential, cropL, cropU, cropW, cropH, blendMode, tilingMode, iU, x0, y0, 0, 0, r0, g0, b0, a0, x1, y1, 0, 0, r1, g1, b1, a1, x2, y2, 0, 0, r2, g2, b2, a2);
    }

    /**
     * Batches a coloured, untextured quad.
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    public final void batchXYRGBA(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float r0, float g0, float b0, float a0, float x1, float y1, float r1, float g1, float b1, float a1, float x2, float y2, float r2, float g2, float b2, float a2, float x3, float y3, float r3, float g3, float b3, float a3) {
        batchXYSTRGBA(cropEssential, cropL, cropU, cropW, cropH, blendMode, tilingMode, iU, x0, y0, 0, 0, r0, g0, b0, a0, x1, y1, 0, 0, r1, g1, b1, a1, x2, y2, 0, 0, r2, g2, b2, a2, x3, y3, 0, 0, r3, g3, b3, a3);
    }

    /**
     * Batches a coloured, textured triangle.
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    public final synchronized void batchXYSTRGBA(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float r0, float g0, float b0, float a0, float x1, float y1, float s1, float t1, float r1, float g1, float b1, float a1, float x2, float y2, float s2, float t2, float r2, float g2, float b2, float a2) {
        ITexRegion tex = batchStartGroup(3, true, cropEssential, cropL, cropU, cropW, cropH, blendMode, tilingMode, iU);
        batchWriteXYSTRGBA(x0, y0, s0, t0, r0, g0, b0, a0, tex);
        batchWriteXYSTRGBA(x1, y1, s1, t1, r1, g1, b1, a1, tex);
        batchWriteXYSTRGBA(x2, y2, s2, t2, r2, g2, b2, a2, tex);
    }

    /**
     * Batches a coloured, textured quad (012023).
     * cropEssential being false implies that the scissor bounds can't be more cropped than what is given, but can be less.
     */
    public final synchronized void batchXYSTRGBA(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float r0, float g0, float b0, float a0, float x1, float y1, float s1, float t1, float r1, float g1, float b1, float a1, float x2, float y2, float s2, float t2, float r2, float g2, float b2, float a2, float x3, float y3, float s3, float t3, float r3, float g3, float b3, float a3) {
        ITexRegion tex = batchStartGroup(6, true, cropEssential, cropL, cropU, cropW, cropH, blendMode, tilingMode, iU);
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
    private @Nullable ITexRegion batchStartGroup(int vertices, boolean hasColours, boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, IReplicatedTexRegion iU) {
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
        if (currentBatch != null)
            if ((currentBatch.vertexCount + vertices) > MAX_VERTICES_IN_BATCH)
                batchFlush();
        // ok, so now that the current batch is dealt with, do the pick here
        ITexRegion tex = null;
        IImage srf = null;
        if (iU != null)
            tex = iU.pickTexRegion(currentBatch != null ? currentBatch.tex : null);
        if (tex != null)
            srf = tex.getSurface();
        if (currentBatch == null || !currentBatch.matchesState(cropEssential, cropL, cropU, cropW, cropH, blendMode, tilingMode, srf)) {
            batchFlush();
            // Setup the reference.
            // Note that we only have to worry about this at the start of a batch.
            // If something happens, it'll reference-barrier, which will flush us, so we'll re-reference next group.
            if (srf != null)
                srf.batchReference(this);
            currentBatch = batchPool.get();
            currentBatch.hasColours = hasColours;
            currentBatch.cropEssential = cropEssential;
            if (cropEssential) {
                currentBatch.cropL = cropL;
                currentBatch.cropU = cropU;
                currentBatch.cropW = cropW;
                currentBatch.cropH = cropH;
            } else {
                currentBatch.cropL = 0;
                currentBatch.cropU = 0;
                currentBatch.cropW = width;
                currentBatch.cropH = height;
            }
            currentBatch.blendMode = blendMode;
            currentBatch.tilingMode = tilingMode;
            currentBatch.tex = srf;
        }
        if (hasColours && !currentBatch.hasColours) {
            // upgrade batch to having colours
            Arrays.fill(stagingC, 0, currentBatch.vertexCount * 4, 1.0f);
            currentBatch.hasColours = true;
        }
        return tex;
    }

    @Override
    public synchronized void batchFlush() {
        // Now actually do the batching thing
        if (currentBatch != null) {
            // Sizes
            int groupVLen = currentBatch.vertexCount * 2;
            int groupCLen = currentBatch.vertexCount * 4;
            int groupTLen = currentBatch.tex != null ? (currentBatch.vertexCount * 2) : 0;
            int groupTotalLen = groupVLen + groupCLen + groupTLen;

            // Layout
            float[] megabuffer = vopeks.floatPool.get(groupTotalLen);

            int groupVOfs = 0;
            int groupCOfs = groupVOfs + groupVLen;
            int groupTOfs = groupCOfs + groupCLen;

            currentBatch.megabuffer = megabuffer;
            currentBatch.verticesOfs = groupVOfs;
            currentBatch.coloursOfs = groupCOfs;
            currentBatch.texCoordsOfs = groupTOfs;

            // Copy
            System.arraycopy(stagingV, 0, megabuffer, groupVOfs, groupVLen);
            System.arraycopy(stagingC, 0, megabuffer, groupCOfs, groupCLen);
            if (currentBatch.tex != null) {
                System.arraycopy(stagingT, 0, megabuffer, groupTOfs, groupTLen);
                // And that's the deadline hit...
                currentBatch.tex.batchUnreference(this);
            }

            // Put
            vopeks.putTask(currentBatch);
        }
        currentBatch = null;
    }

    /**
     * Writes a vertex to the batcher.
     * For ease of use, X/Y coordinates are converted to the -1 to 1 representation here.
     */
    private void batchWriteXYSTRGBA(float x, float y, float s, float t, float r, float g, float b, float a, @Nullable ITexRegion tf) {
        int vertexBase2 = currentBatch.vertexCount * 2;
        int vertexBase4 = currentBatch.vertexCount * 4;
        stagingV[vertexBase2] = (x - halfWF) / halfWF;
        stagingV[vertexBase2 + 1] = (y - halfHF) / halfHF;
        if (tf != null) {
            float nS = tf.getS(s, t);
            float nT = tf.getT(s, t);
            stagingT[vertexBase2] = nS;
            stagingT[vertexBase2 + 1] = nT;
        }
        if (currentBatch.hasColours) {
            stagingC[vertexBase4] = r;
            stagingC[vertexBase4 + 1] = g;
            stagingC[vertexBase4 + 2] = b;
            stagingC[vertexBase4 + 3] = a;
        }
        currentBatch.vertexCount++;
    }

    private class BatchPool extends ObjectPool<Batch> {
        public BatchPool(int expandChunkSize) {
            super(expandChunkSize);
        }

        @Override
        protected @NonNull Batch gen() {
            return new Batch();
        }
        @Override
        public void reset(@NonNull Batch element) {
            element.cropL = 0;
            element.cropU = 0;
            element.cropW = 0;
            element.cropH = 0;
            element.cropEssential = false;
            element.vertexCount = 0;
            element.blendMode = IGrDriver.BLEND_NONE;
            element.tilingMode = TilingMode.None;
            element.tex = null;
            element.megabuffer = null;
            element.verticesOfs = 0;
            element.coloursOfs = 0;
            element.texCoordsOfs = 0;
            element.hasColours = false;
        }
    }

    private class Batch implements ITask {
        int cropL, cropU, cropW, cropH;
        int vertexCount;
        int blendMode = IGrDriver.BLEND_NONE;
        TilingMode tilingMode = TilingMode.None;
        IImage tex;
        float[] megabuffer; int verticesOfs, coloursOfs, texCoordsOfs;
        boolean hasColours;
        boolean cropEssential;

        @Override
        public void run(Instance instance) {
            if (texture == null) {
                System.err.println("VopeksBatchingSurface: Texture disappeared from " + VopeksBatchingSurface.this + ". Someone try something silly?");
                return;
            }

            BadGPU.Texture tx = tex != null ? tex.getTextureFromTask() : null;
            long tx2 = tx != null ? tx.pointer : 0;
            int alphaComp = BadGPU.Compare.Always.value;
            int drawFlags = 0;
            if (blendMode != IGrDriver.BLEND_NONE)
                drawFlags |= BadGPU.DrawFlags.Blend;
            // In the normal blend mode, an alpha of 0 leads to a NOP, so discard those pixels.
            if (blendMode == IGrDriver.BLEND_NORMAL)
                alphaComp = BadGPU.Compare.Greater.value;

            drawFlags |= tilingMode.badgpuValue;

            BadGPUUnsafe.drawGeomNoDS(texture.pointer, BadGPU.SessionFlags.MaskAll | BadGPU.SessionFlags.Scissor,
                    cropL, cropU, cropW, cropH,
                    drawFlags,
                    2, megabuffer, verticesOfs, hasColours ? megabuffer : null, coloursOfs, 2, tx == null ? null : megabuffer, texCoordsOfs,
                    BadGPU.PrimitiveType.Triangles.value, 1,
                    0, vertexCount, null, 0,
                    null, 0,
                    0, 0, width, height,
                    tx2, null, 0,
                    null, 0, alphaComp, 0,
                    blendMode);
            vopeks.floatPool.finish(megabuffer);
            batchPool.finish(this);
        }

        public boolean matchesState(boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, IImage tex) {
            if (cropEssential) {
                if (cropL != this.cropL || cropU != this.cropU || cropW != this.cropW || cropH != this.cropH) {
                    // System.out.println("break batch: SCO " + cropL + "," + cropU + "," + cropW + "," + cropH + " -> " + this.cropL + "," + this.cropU + "," + this.cropW + "," + this.cropH);
                    return false;
                }
            } else if (this.cropEssential) {
                int cropR = cropL + cropW;
                int cropD = cropU + cropH;
                int tCropR = this.cropL + this.cropW;
                int tCropD = this.cropU + this.cropH;
                if (cropL > this.cropL || cropU > this.cropU || cropR < tCropR || cropD < tCropD) {
                    // System.out.println("break batch: SCO on a non-essential crop");
                    return false;
                }
            }
            if (tex != this.tex) {
                // System.out.println("break batch: tex: " + tex + " -> " + this.tex);
                return false;
            }
            if (blendMode != this.blendMode) {
                // System.out.println("break batch: blendMode: " + blendMode + " -> " + this.blendMode);
                return false;
            }
            if (tilingMode != this.tilingMode) {
                // System.out.println("break batch: tilingMode: " + tilingMode + " -> " + this.tilingMode);
                return false;
            }
            return true;
        }
    }
}
