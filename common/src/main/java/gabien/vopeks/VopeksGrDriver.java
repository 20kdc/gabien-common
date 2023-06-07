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
    public VopeksGrDriver(Vopeks vopeks, int w, int h, boolean alpha, int[] init) {
        super(vopeks, w, h, alpha, init);
        localST[4] = width;
        localST[5] = height;
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {
        blitScaledImage(srcx, srcy, srcw, srch, x, y, srcw, srch, i, TilingMode.None);
    }

    @Override
    public void blitTiledImage(int x, int y, int w, int h, IImage cachedTile) {
        blitScaledImage(0, 0, w, h, x, y, w, h, cachedTile, TilingMode.XY);
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i, TilingMode.None);
    }

    public synchronized void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int w, int h, IImage i, TilingMode tiling) {
        x += localST[0];
        y += localST[1];
        float srcWF = i.getWidth();
        float srcHF = i.getHeight();
        float s0 = srcx / srcWF;
        float s1 = (srcx + srcw) / srcWF;
        float t0 = srcy / srcHF;
        float t1 = (srcy + srch) / srcHF;
        IVopeksSurfaceHolder vsh = i instanceof IVopeksSurfaceHolder ? (IVopeksSurfaceHolder) i : null;
        batchEnsureRoom(6);
        batchInStateScA(BlendMode.Normal, TilingMode.None, vsh);
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
                    BadGPU.SessionFlags.MaskRGBA | BadGPU.SessionFlags.Scissor, cropL, cropU, cropW, cropH,
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
        batchEnsureRoom(6);
        batchInStateScA(BlendMode.Normal, TilingMode.None, null);
        batchWrite(x    , y    , 0, 0, rF, gF, bF, aF);
        batchWrite(x + w, y    , 0, 0, rF, gF, bF, aF);
        batchWrite(x + w, y + h, 0, 0, rF, gF, bF, aF);
        batchWrite(x    , y    , 0, 0, rF, gF, bF, aF);
        batchWrite(x + w, y + h, 0, 0, rF, gF, bF, aF);
        batchWrite(x    , y + h, 0, 0, rF, gF, bF, aF);
    }

    /**
     * batchInState but aware of scissoring
     */
    public void batchInStateScA(BlendMode blendMode, TilingMode tilingMode, IVopeksSurfaceHolder tex) {
        batchInState(localST[2], localST[3], localST[4] - localST[2], localST[5] - localST[3], blendMode, tilingMode, tex);
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
