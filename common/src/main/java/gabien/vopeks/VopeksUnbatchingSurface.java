/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.natives.BadGPU;
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
 * Copied from VopeksBatchingSurface 17th October 2023.
 */
public final class VopeksUnbatchingSurface extends IGrDriver {
    /**
     * The parent instance.
     */
    public final Vopeks vopeks;

    private volatile boolean wasDisposed;

    /**
     * State of the crop registers for the group that is being prepared right now.
     * Use only in sync.
     */
    private int upcomingCropL, upcomingCropU, upcomingCropR, upcomingCropD;

    private final float halfWF, halfHF;

    /**
     * Creates a new texture for rendering, and possibly initializes it.
     */
    public VopeksUnbatchingSurface(@NonNull Vopeks vopeks, @Nullable String id, int w, int h, int[] init) {
        super(id, w, h);
        this.vopeks = vopeks;
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
            BadGPUUnsafe.drawClear(texture.pointer, 0,
                    BadGPU.SessionFlags.MaskRGBA | BadGPU.SessionFlags.Scissor, scL, scU, cropW, cropH,
                    r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f, 0, 0);
        });
    }

    @Override
    public synchronized void generateMipmap() {
        batchFlush();
        batchReferenceBarrier();
        vopeks.putTask((instance) -> {
            BadGPUUnsafe.generateMipmap(texture.pointer);
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

    @Override
    public final synchronized void rawBatchXYST(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, int drawFlagsEx, @Nullable ITexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2) {
        if (setupAndCheckCrop(cropEssential, cropL, cropU, cropR, cropD))
            return;
        batchSend(false, cropEssential, blendMode, drawFlagsEx, iU, new float[] {
                x0, y0, x1, y1, x2, y2,
                s0, t0, s1, t1, s2, t2,
        });
    }

    @Override
    public final synchronized void rawBatchXYST(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, int drawFlagsEx, @Nullable ITexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2, float x3, float y3, float s3, float t3) {
        if (setupAndCheckCrop(cropEssential, cropL, cropU, cropR, cropD))
            return;
        batchSend(false, cropEssential, blendMode, drawFlagsEx, iU, new float[] {
                x0, y0, x1, y1, x2, y2, x0, y0, x2, y2, x3, y3,
                s0, t0, s1, t1, s2, t2, s0, t0, s2, t2, s3, t3,
        });
    }

    @Override
    public final synchronized void rawBatchXYSTRGBA(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, int drawFlagsEx, @Nullable ITexRegion iU, float x0, float y0, float s0, float t0, float r0, float g0, float b0, float a0, float x1, float y1, float s1, float t1, float r1, float g1, float b1, float a1, float x2, float y2, float s2, float t2, float r2, float g2, float b2, float a2) {
        if (setupAndCheckCrop(cropEssential, cropL, cropU, cropR, cropD))
            return;
        batchSend(true, cropEssential, blendMode, drawFlagsEx, iU, new float[] {
                x0, y0, x1, y1, x2, y2,
                s0, t0, s1, t1, s2, t2,
                r0, g0, b0, a0, r1, g1, b1, a1, r2, g2, b2, a2,
        });
    }

    @Override
    public final synchronized void rawBatchXYSTRGBA(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, int drawFlagsEx, @Nullable ITexRegion iU, float x0, float y0, float s0, float t0, float r0, float g0, float b0, float a0, float x1, float y1, float s1, float t1, float r1, float g1, float b1, float a1, float x2, float y2, float s2, float t2, float r2, float g2, float b2, float a2, float x3, float y3, float s3, float t3, float r3, float g3, float b3, float a3) {
        if (setupAndCheckCrop(cropEssential, cropL, cropU, cropR, cropD))
            return;
        batchSend(true, cropEssential, blendMode, drawFlagsEx, iU, new float[] {
                x0, y0, x1, y1, x2, y2, x0, y0, x2, y2, x3, y3,
                s0, t0, s1, t1, s2, t2, s0, t0, s2, t2, s3, t3,
                r0, g0, b0, a0, r1, g1, b1, a1, r2, g2, b2, a2, r0, g0, b0, a0, r2, g2, b2, a2, r3, g3, b3, a3,
        });
    }

    private void batchSend(boolean hasColours, boolean cropEssential, int blendMode, int drawFlagsEx, ITexRegion iU, float[] data) {
        batchReferenceBarrier();
        int vertexCount = hasColours ? (data.length / 8) : (data.length / 4);
        // ok, so now that the current batch is dealt with, do the pick here
        IImgRegion tex = null;
        IImage srf = null;
        if (iU != null)
            tex = iU.pickImgRegion(null);
        if (tex != null)
            srf = tex.getSurface();
        VopeksBatch currentBatch = new VopeksBatch(vopeks, this, null);
        currentBatch.hasColours = hasColours;
        currentBatch.vertexCount = vertexCount;
        if (cropEssential) {
            currentBatch.cropL = upcomingCropL;
            currentBatch.cropU = upcomingCropU;
            currentBatch.cropR = upcomingCropR;
            currentBatch.cropD = upcomingCropD;
        } else {
            currentBatch.cropL = 0;
            currentBatch.cropU = 0;
            currentBatch.cropR = width;
            currentBatch.cropD = height;
        }
        currentBatch.blendMode = blendMode;
        currentBatch.drawFlagsEx = drawFlagsEx;
        currentBatch.tex = srf;
        currentBatch.megabuffer = data;
        currentBatch.verticesOfs = 0;
        currentBatch.texCoordsOfs = vertexCount * 2;
        currentBatch.coloursOfs = vertexCount * 4;
        // map coordinates...
        for (int i = 0; i < vertexCount; i++) {
            int posB = i * 2;
            data[posB] = (data[posB] - halfWF) / halfWF;
            data[posB + 1] = (data[posB + 1] - halfHF) / halfHF;
        }
        if (tex != null) {
            for (int i = 0; i < vertexCount; i++) {
                int posT = currentBatch.texCoordsOfs + (i * 2);
                float s = data[posT];
                float t = data[posT + 1];
                data[posT] = tex.getS(s, t);
                data[posT + 1] = tex.getT(s, t);
            }
        }
        // Actually insert the task and do tex ref/unref etc.
        if (srf != null)
            srf.batchReference(this);
        vopeks.putTask(currentBatch);
        if (srf != null)
            srf.batchUnreference(this);
    }

    @Override
    public synchronized void batchFlush() {
    }
}
