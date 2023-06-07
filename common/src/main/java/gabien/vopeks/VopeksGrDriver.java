/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import org.eclipse.jdt.annotation.NonNull;

import gabien.IGrDriver;
import gabien.IImage;
import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Instance;
import gabien.uslx.append.ObjectPool;
import gabien.vopeks.Vopeks.ITask;

/**
 * Here goes nothing.
 *
 * Created 7th June, 2023.
 */
public class VopeksGrDriver extends VopeksImage implements IGrDriver {
    public final int[] localST = new int[6];

    public BlitCommandPool blitPool = new BlitCommandPool(256);

    /**
     * Creates a new texture for rendering, and possibly initializes it.
     */
    public VopeksGrDriver(Vopeks vopeks, int w, int h, boolean alpha, int[] init) {
        super(vopeks, w, h, alpha, init);
        localST[4] = width;
        localST[5] = height;
    }

    private final static float[] STANDARD_TC = new float[] {
            0, 0, 0, 1,
            1, 0, 0, 1,
            1, 1, 0, 1,
            0, 1, 0, 1
    };

    private final static float[] STANDARD_VT = new float[] {
           -1,-1, 0, 1,
            1,-1, 0, 1,
            1, 1, 0, 1,
           -1, 1, 0, 1
    };

    private final float[] reusedCol = new float[4];
    private final float[] reusedTM = new float[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };
    private final static short[] STANDARD_INDICES = new short[] {0, 1, 2, 0, 3, 2};

    private void standardBlit(boolean shouldBlend, boolean isTiling, int x, int y, int w, int h, float r, float g, float b, float a, IImage i, int srcx, int srcy, int srcw, int srch) {
        BlitCommand bc = blitPool.get();
        bc.shouldBlend = shouldBlend;
        bc.isTiling = isTiling;

        bc.cropL = localST[2];
        bc.cropU = localST[3];
        int cropR = localST[4];
        int cropD = localST[5];
        bc.cropW = cropR - bc.cropL;
        bc.cropH = cropD - bc.cropU;

        bc.adjX = x + localST[0];
        bc.adjY = y + localST[1];
        bc.w = w;
        bc.h = h;

        bc.r = r;
        bc.g = g;
        bc.b = b;
        bc.a = a;

        bc.tex = (i != null && i instanceof IVopeksSurfaceHolder) ? ((IVopeksSurfaceHolder) i) : null;
        bc.srcx = srcx;
        bc.srcy = srcy;
        bc.srcw = srcw;
        bc.srch = srch;

        vopeks.putTask(bc);
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {
        if (i instanceof IVopeksSurfaceHolder)
            standardBlit(true, false, x, y, srcw, srch, 1, 1, 1, 1, i, srcx, srcy, srcw, srch);
    }

    @Override
    public void blitTiledImage(int x, int y, int w, int h, IImage cachedTile) {
        if (cachedTile instanceof IVopeksSurfaceHolder)
            standardBlit(true, true, x, y, w, h, 1, 1, 1, 1, cachedTile, 0, 0, w, h);
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        if (i instanceof IVopeksSurfaceHolder)
            standardBlit(true, false, x, y, acw, ach, 1, 1, 1, 1, i, srcx, srcy, srcw, srch);
    }

    @Override
    public void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean blendSub) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void clearAll(int i, int i0, int i1) {
        int cropL = localST[2];
        int cropU = localST[3];
        int cropR = localST[4];
        int cropD = localST[5];
        int cropW = cropR - cropL;
        int cropH = cropD - cropU;
        vopeks.putTask((instance) -> {
            BadGPU.drawClear(texture, null,
                    BadGPU.SessionFlags.MaskRGBA | BadGPU.SessionFlags.Scissor, cropL, cropU, cropW, cropH,
                    i / 255.0f, i0 / 255.0f, i1 / 255.0f, 1, 0, 0);
        });
    }

    @Override
    public void clearRectAlpha(int r, int g, int b, int a, int x, int y, int w, int h) {
        standardBlit(true, false, x, y, w, h, r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f, null, 0, 0, 0, 0);
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

    private class BlitCommandPool extends ObjectPool<BlitCommand> {
        public BlitCommandPool(int expandChunkSize) {
            super(expandChunkSize);
        }

        @Override
        protected @NonNull BlitCommand gen() {
            return new BlitCommand();
        }
        @Override
        public void reset(@NonNull BlitCommand element) {
            element.r = 0;
            element.g = 0;
            element.b = 0;
            element.a = 0;
            element.srcx = 0;
            element.srcy = 0;
            element.srcw = 0;
            element.srch = 0;
            element.cropL = 0;
            element.cropU = 0;
            element.cropW = 0;
            element.cropH = 0;
            element.adjX = 0;
            element.adjY = 0;
            element.w = 0;
            element.h = 0;
            element.shouldBlend = false;
            element.isTiling = false;
            element.tex = null;
        }
    }

    private class BlitCommand implements ITask {
        float r, g, b, a;
        int srcx, srcy, srcw, srch;
        int cropL, cropU, cropW, cropH;
        int adjX, adjY, w, h;
        boolean shouldBlend;
        boolean isTiling;
        IVopeksSurfaceHolder tex;

        @Override
        public void run(Instance instance) {
            BadGPU.Texture tx = tex != null ? tex.getTextureFromTask() : null;
            int drawFlags = BadGPU.DrawFlags.FreezeColour;
            if (shouldBlend)
                drawFlags |= BadGPU.DrawFlags.Blend;
            if (isTiling)
                drawFlags |= BadGPU.DrawFlags.WrapS | BadGPU.DrawFlags.WrapT;
            reusedCol[0] = r;
            reusedCol[1] = g;
            reusedCol[2] = b;
            reusedCol[3] = a;
            if (tex != null) {
                // set base & scale
                float iwf = tex.getWidth();
                float ihf = tex.getHeight();
                reusedTM[12] = srcx / iwf;
                reusedTM[13] = srcy / ihf;
                reusedTM[0] = srcw / iwf;
                reusedTM[5] = srch / ihf;
            }
            BadGPU.drawGeomNoDS(texture, BadGPU.SessionFlags.MaskRGBA | BadGPU.SessionFlags.Scissor,
                    cropL, cropU, cropW, cropH,
                    drawFlags,
                    STANDARD_VT, 0, reusedCol, 0, STANDARD_TC, 0,
                    BadGPU.PrimitiveType.Triangles, 1,
                    0, 6, STANDARD_INDICES, 0,
                    null, 0, null, 0,
                    adjX, adjY, w, h,
                    tx, reusedTM, 0,
                    BadGPU.BlendWeight.SrcA, BadGPU.BlendWeight.InvertSrcA, BadGPU.BlendEquation.Add,
                    BadGPU.BlendWeight.One, BadGPU.BlendWeight.InvertSrcA, BadGPU.BlendEquation.Add);
            blitPool.finish(this);
        }
    }
}
