/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import org.eclipse.jdt.annotation.NonNull;

import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Instance;
import gabien.uslx.append.ObjectPool;
import gabien.vopeks.Vopeks.ITask;

/**
 * This is a parent of VopeksGrDriver to separate out the batching code.
 * At some point it might be nice for IGrDriver to use default methods on top of this.
 *
 * Created 7th June, 2023.
 */
public class VopeksBatchingSurface extends VopeksImage {
    public BatchPool batchPool = new BatchPool(1);

    /**
     * Creates a new texture for rendering, and possibly initializes it.
     */
    public VopeksBatchingSurface(Vopeks vopeks, int w, int h, boolean alpha, int[] init) {
        super(vopeks, w, h, alpha, init);
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
            element.shouldBlend = false;
            element.isTiling = false;
            element.tex = null;
        }
    }

    private class Batch implements ITask {
        int cropL, cropU, cropW, cropH;
        int vertexCount;
        boolean shouldBlend;
        boolean isTiling;
        IVopeksSurfaceHolder tex;
        final float[] vertices = new float[65536 * 4];
        final float[] colours = new float[65536 * 4];
        final float[] texCoords = new float[65536 * 4];

        @Override
        public void run(Instance instance) {
            BadGPU.Texture tx = tex != null ? tex.getTextureFromTask() : null;
            int drawFlags = BadGPU.DrawFlags.FreezeColour;
            if (shouldBlend)
                drawFlags |= BadGPU.DrawFlags.Blend;
            if (isTiling)
                drawFlags |= BadGPU.DrawFlags.WrapS | BadGPU.DrawFlags.WrapT;
            BadGPU.drawGeomNoDS(texture, BadGPU.SessionFlags.MaskRGBA | BadGPU.SessionFlags.Scissor,
                    cropL, cropU, cropW, cropH,
                    drawFlags,
                    vertices, 0, colours, 0, texCoords, 0,
                    BadGPU.PrimitiveType.Triangles, 1,
                    0, vertexCount, null, 0,
                    null, 0, null, 0,
                    0, 0, width, height,
                    tx, null, 0,
                    BadGPU.BlendWeight.SrcA, BadGPU.BlendWeight.InvertSrcA, BadGPU.BlendEquation.Add,
                    BadGPU.BlendWeight.SrcA, BadGPU.BlendWeight.InvertSrcA, BadGPU.BlendEquation.Add);
            batchPool.finish(this);
        }
    }
}
