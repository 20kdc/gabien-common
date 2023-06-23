/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;
import gabien.natives.BadGPU.Instance;
import gabien.render.IGrDriver;
import gabien.render.IImage;
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
public class VopeksBatchingSurface extends VopeksImage {
    private static final int MAX_VERTICES_IN_BATCH = 65536;
    private final BatchPool batchPool = new BatchPool(1);
    private Batch currentBatch = null;
    private final float[] stagingV = new float[MAX_VERTICES_IN_BATCH * 4];
    private final float[] stagingC = new float[MAX_VERTICES_IN_BATCH * 4];
    private final float[] stagingT = new float[MAX_VERTICES_IN_BATCH * 4];
    private final float halfWF, halfHF;
    private final ArrayList<IImage> referencedBy = new ArrayList<>();

    /**
     * Creates a new texture for rendering, and possibly initializes it.
     */
    public VopeksBatchingSurface(@NonNull Vopeks vopeks, @Nullable String id, int w, int h, int[] init) {
        super(vopeks, id, w, h, init);
        halfWF = w / 2.0f;
        halfHF = h / 2.0f;
    }

    /**
     * Returns some approximation of the last surface for dealing with IReplicatedTexRegion.
     */
    public @Nullable IImage batchGetLastSurface() {
        if (currentBatch != null)
            return currentBatch.tex;
        return null;
    }

    /**
     * Ensures the batcher is in the right state to accept the given geometry.
     * This will actually begin a new batch, so make sure you're sure!
     * cropEssential being false implies that the scissor bounds can't be more cropped than this, but can be less.
     */
    public void batchStartGroup(int vertices, boolean hasColours, boolean cropEssential, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, IImage tex) {
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
        if (currentBatch == null || !currentBatch.matchesState(hasColours, cropEssential, cropL, cropU, cropW, cropH, blendMode, tilingMode, tex)) {
            batchFlush();
            // Setup the reference.
            // Note that we only have to worry about this at the start of a batch.
            // If something happens, it'll reference-barrier, which will flush us, so we'll re-reference next group.
            if (tex != null)
                tex.batchReference(this);
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
            currentBatch.tex = tex;
        }
    }

    /**
     * Flushes batches of things that have batches attached to this surface.
     * Call immediately before any call to putTask that writes to this surface.
     * Will be internally called before batchStartGroup.
     */
    public synchronized void batchReferenceBarrier() {
        while (!referencedBy.isEmpty())
            referencedBy.remove(referencedBy.size() - 1).batchFlush();
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

    @Override
    public synchronized void batchReference(IImage caller) {
        // If this wasn't here, the caller could refer to an unfinished batch.
        // Where this becomes a problem is that the caller could submit the task, and the batch might still not be submitted.
        // Since any changes we do make would require a flush (because we have a reference), just flush now,
        //  rather than flushing on unreference or something.
        // (Also, we could be holding a reference to caller, which implies the ability to create reference loops.)
        batchFlush();
        referencedBy.add(caller);
    }

    @Override
    public void batchUnreference(IImage caller) {
        referencedBy.remove(caller);
    }

    /**
     * Writes a texCoorded vertex to the batcher.
     * For ease of use, X/Y coordinates are converted to the -1 to 1 representation here.
     * ST would be converted but might be useful to have the ability to introduce epsilon margins.
     */
    public void batchWriteXYST(float x, float y, float s, float t) {
        int vertexBase2 = currentBatch.vertexCount * 2;
        stagingV[vertexBase2] = (x - halfWF) / halfWF;
        stagingV[vertexBase2 + 1] = (y - halfHF) / halfHF;
        if (currentBatch.tex != null) {
            stagingT[vertexBase2] = s;
            stagingT[vertexBase2 + 1] = t;
        }
        currentBatch.vertexCount++;
    }

    /**
     * Writes a vertex to the batcher.
     * For ease of use, X/Y coordinates are converted to the -1 to 1 representation here.
     */
    public void batchWriteXYRGBA(float x, float y, float r, float g, float b, float a) {
        int vertexBase2 = currentBatch.vertexCount * 2;
        int vertexBase4 = currentBatch.vertexCount * 4;
        stagingV[vertexBase2] = (x - halfWF) / halfWF;
        stagingV[vertexBase2 + 1] = (y - halfHF) / halfHF;
        stagingC[vertexBase4] = r;
        stagingC[vertexBase4 + 1] = g;
        stagingC[vertexBase4 + 2] = b;
        stagingC[vertexBase4 + 3] = a;
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

    public enum TilingMode {
        None(0),
        X(BadGPU.DrawFlags.WrapS),
        Y(BadGPU.DrawFlags.WrapT),
        XY(BadGPU.DrawFlags.WrapS | BadGPU.DrawFlags.WrapT);

        private final int value;

        TilingMode(int v) {
            value = v;
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

            drawFlags |= tilingMode.value;

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

        public boolean matchesState(boolean cropEssential, boolean hasColours, int cropL, int cropU, int cropW, int cropH, int blendMode, TilingMode tilingMode, IImage tex) {
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
            if (hasColours != this.hasColours) {
                // System.out.println("break batch: hasColours: " + hasColours + " -> " + this.hasColours);
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
            if (tex != this.tex) {
                // System.out.println("break batch: tex: " + tex + " -> " + this.tex);
                return false;
            }
            return true;
        }
    }
}
