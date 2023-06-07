/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import java.util.HashSet;

import org.eclipse.jdt.annotation.NonNull;

import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Instance;
import gabien.natives.BadGPUEnum.BlendEquation;
import gabien.natives.BadGPUEnum.BlendWeight;
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
    private final HashSet<IVopeksSurfaceHolder> referencedBy = new HashSet<>();

    /**
     * Creates a new texture for rendering, and possibly initializes it.
     */
    public VopeksBatchingSurface(Vopeks vopeks, int w, int h, int[] init) {
        super(vopeks, w, h, init);
        halfWF = w / 2.0f;
        halfHF = h / 2.0f;
    }

    /**
     * Ensures the batcher has room for this many vertices, or flushes it.
     */
    public void batchEnsureRoom(int vertices) {
        if (currentBatch != null)
            if ((currentBatch.vertexCount + vertices) >= MAX_VERTICES_IN_BATCH)
                batchFlush();
    }

    /**
     * Ensures the batcher is in the right state to accept the given geometry.
     * This will actually begin a new batch, so make sure you're sure!
     */
    public void batchInState(int cropL, int cropU, int cropW, int cropH, BlendMode blendMode, TilingMode tilingMode, IVopeksSurfaceHolder tex) {
        if (currentBatch == null || !currentBatch.matchesState(cropL, cropU, cropW, cropH, blendMode, tilingMode, tex)) {
            batchFlush();
            if (tex != null)
                tex.batchReference(this);
            currentBatch = batchPool.get();
            currentBatch.cropL = cropL;
            currentBatch.cropU = cropU;
            currentBatch.cropW = cropW;
            currentBatch.cropH = cropH;
            currentBatch.blendMode = blendMode;
            currentBatch.tilingMode = tilingMode;
            currentBatch.tex = tex;
        }
    }

    /**
     * Flushes batches of things that have batches attached to this surface.
     * Call immediately before any call to putTask that writes to this surface.
     */
    public synchronized void batchReferenceBarrier() {
        for (IVopeksSurfaceHolder ref : referencedBy)
            ref.batchFlush();
        referencedBy.clear();
    }

    @Override
    public synchronized void batchFlush() {
        // Now actually do the batching thing
        if (currentBatch != null) {
            batchReferenceBarrier();
            int groupCount = currentBatch.tex != null ? 3 : 2;
            int groupLen = currentBatch.vertexCount * 4;
            float[] megabuffer = vopeks.floatPool.get(groupLen * groupCount);
            currentBatch.megabuffer = megabuffer;
            currentBatch.megabufferOfs = 0;
            int writeOfs = currentBatch.megabufferOfs;
            System.arraycopy(stagingV, 0, megabuffer, writeOfs, groupLen);
            writeOfs += groupLen;
            System.arraycopy(stagingC, 0, megabuffer, writeOfs, groupLen);
            if (currentBatch.tex != null) {
                writeOfs += groupLen;
                System.arraycopy(stagingT, 0, megabuffer, writeOfs, groupLen);
            }
            vopeks.putTask(currentBatch);
        }
        currentBatch = null;
    }

    @Override
    public synchronized void batchReference(IVopeksSurfaceHolder other) {
        batchFlush();
        referencedBy.add(other);
    }

    /**
     * Writes a vertex to the batcher.
     * For ease of use, X/Y coordinates are converted to the -1 to 1 representation here.
     * ST would be converted but might be useful to have the ability to introduce epsilon margins.
     */
    public void batchWrite(float x, float y, float s, float t, float r, float g, float b, float a) {
        int vertexBase = (currentBatch.vertexCount++) * 4;
        stagingV[vertexBase] = (x - halfWF) / halfWF;
        stagingV[vertexBase + 1] = (y - halfHF) / halfHF;
        stagingV[vertexBase + 2] = 0;
        stagingV[vertexBase + 3] = 1;
        stagingC[vertexBase] = r;
        stagingC[vertexBase + 1] = g;
        stagingC[vertexBase + 2] = b;
        stagingC[vertexBase + 3] = a;
        if (currentBatch.tex != null) {
            stagingT[vertexBase] = s;
            stagingT[vertexBase + 1] = t;
            stagingT[vertexBase + 2] = 0;
            stagingT[vertexBase + 3] = 1;
        }
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
            element.vertexCount = 0;
            element.blendMode = BlendMode.None;
            element.tilingMode = TilingMode.None;
            element.tex = null;
            element.megabuffer = null;
            element.megabufferOfs = 0;
        }
    }

    public enum BlendMode {
        None,
        Normal,
        Additive,
        Subtractive
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
        BlendMode blendMode = BlendMode.None;
        TilingMode tilingMode = TilingMode.None;
        IVopeksSurfaceHolder tex;
        float[] megabuffer; int megabufferOfs;

        @Override
        public void run(Instance instance) {
            BadGPU.Texture tx = tex != null ? tex.getTextureFromTask() : null;
            int drawFlags = BadGPU.DrawFlags.FreezeColour;
            if (blendMode != BlendMode.None)
                drawFlags |= BadGPU.DrawFlags.Blend;

            BlendWeight bwRGBS = BlendWeight.SrcA, bwRGBD = BlendWeight.InvertSrcA;
            BlendWeight bwAS = BlendWeight.SrcA, bwAD = BlendWeight.InvertSrcA;
            BlendEquation beRGB = BlendEquation.Add, beA = BlendEquation.Add;

            if (blendMode == BlendMode.Additive || blendMode == BlendMode.Subtractive) {
                bwAS = BlendWeight.Zero;
                bwAD = BlendWeight.One;
                bwRGBS = BlendWeight.One;
                bwRGBD = BlendWeight.One;
                if (blendMode == BlendMode.Subtractive)
                    beRGB = BlendEquation.ReverseSub;
            }

            drawFlags |= tilingMode.value;

            int verticesOfs = megabufferOfs;
            int groupLen = vertexCount * 4;
            int coloursOfs = verticesOfs + groupLen;
            int texCoordsOfs = coloursOfs + groupLen;

            BadGPU.drawGeomNoDS(texture, BadGPU.SessionFlags.MaskRGBA | BadGPU.SessionFlags.Scissor,
                    cropL, cropU, cropW, cropH,
                    drawFlags,
                    megabuffer, verticesOfs, megabuffer, coloursOfs, tx == null ? null : megabuffer, texCoordsOfs,
                    BadGPU.PrimitiveType.Triangles, 1,
                    0, vertexCount, null, 0,
                    null, 0, null, 0,
                    0, 0, width, height,
                    tx, null, 0,
                    bwRGBS, bwRGBD, beRGB,
                    bwAS, bwAD, beA);
            vopeks.floatPool.finish(megabuffer);
            batchPool.finish(this);
        }

        public boolean matchesState(int cropL, int cropU, int cropW, int cropH, BlendMode blendMode, TilingMode tilingMode, IVopeksSurfaceHolder tex) {
            if (cropL != this.cropL || cropU != this.cropU || cropW != this.cropW || cropH != this.cropH)
                return false;
            if (blendMode != this.blendMode)
                return false;
            if (tilingMode != this.tilingMode)
                return false;
            if (tex != this.tex)
                return false;
            return true;
        }
    }
}
