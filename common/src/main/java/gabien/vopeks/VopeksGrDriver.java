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
import gabien.backendhelp.INativeImageHolder;
import gabien.natives.BadGPU;
import gabien.natives.BadGPUEnum.TextureLoadFormat;

/**
 * Here goes nothing.
 *
 * Created 7th June, 2023.
 */
public class VopeksGrDriver implements IGrDriver, INativeImageHolder {
    public final Vopeks vopeks;
    // Only guaranteed to exist on Vopeks thread!!!
    private BadGPU.Texture texture;
    public final int width;
    public final int height;

    public final int[] localST = new int[6];

    public VopeksGrDriver(Vopeks vopeks, int w, int h, boolean alpha, int[] init) {
        this.vopeks = vopeks;
        vopeks.taskQueue.add((instance) -> {
            texture = instance.newTexture(alpha ? BadGPU.TextureFlags.HasAlpha : 0, w, h, BadGPU.TextureLoadFormat.ARGBI32, init, 0);
        });
        width = w;
        height = h;
        localST[4] = width;
        localST[5] = height;
    }

    public VopeksGrDriver(Vopeks vopeks, BadGPU.Texture texture, int w, int h) {
        this.vopeks = vopeks;
        this.texture = texture;
        width = w;
        height = h;
        localST[4] = width;
        localST[5] = height;
    }

    @Override
    public void getPixelsAsync(@NonNull int[] buffer, @NonNull Runnable onDone) {
        vopeks.taskQueue.add((instance) -> {
            texture.readPixels(0, 0, width, height, TextureLoadFormat.ARGBI32, buffer, 0);
            onDone.run();
        });
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
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
        final VopeksGrDriver iV = (i != null && i instanceof INativeImageHolder) ? ((INativeImageHolder) i).getNative() : null;
        final int adjX = x + localST[0];
        final int adjY = y + localST[1];
        int cropL = localST[2];
        int cropU = localST[3];
        int cropR = localST[4];
        int cropD = localST[5];
        int cropW = cropR - cropL;
        int cropH = cropD - cropU;
        vopeks.taskQueue.add((instance) -> {
            BadGPU.Texture tx = iV != null ? iV.texture : null;
            int drawFlags = BadGPU.DrawFlags.FreezeColour;
            if (shouldBlend)
                drawFlags |= BadGPU.DrawFlags.Blend;
            if (isTiling)
                drawFlags |= BadGPU.DrawFlags.WrapS | BadGPU.DrawFlags.WrapT;
            reusedCol[0] = r;
            reusedCol[1] = g;
            reusedCol[2] = b;
            reusedCol[3] = a;
            if (i != null) {
                // set base & scale
                float iwf = iV.width;
                float ihf = iV.height;
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
                    BadGPU.BlendWeight.SrcA, BadGPU.BlendWeight.InvertSrcA, BadGPU.BlendEquation.Add);
        });
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {
        if (i instanceof INativeImageHolder)
            standardBlit(true, false, x, y, srcw, srch, 1, 1, 1, 1, i, srcx, srcy, srcw, srch);
    }

    @Override
    public void blitTiledImage(int x, int y, int w, int h, IImage cachedTile) {
        if (cachedTile instanceof INativeImageHolder)
            standardBlit(true, true, x, y, w, h, 1, 1, 1, 1, cachedTile, 0, 0, w, h);
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        if (i instanceof INativeImageHolder)
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
        vopeks.taskQueue.add((instance) -> {
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
        // uhhhh just don't for now
    }

    @Override
    public int[] getLocalST() {
        return localST;
    }

    @Override
    public void updateST() {
    }

    @Override
    public VopeksGrDriver getNative() {
        return this;
    }
}
