/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import org.eclipse.jdt.annotation.NonNull;

import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Texture;
import gabien.natives.BadGPUEnum.TextureLoadFormat;

/**
 * Here goes nothing.
 * This being the base class of VopeksGrDriver along with IVopeksSurfaceHolder is done so that 
 *
 * Created 7th June, 2023.
 */
public class VopeksImage implements IVopeksSurfaceHolder {
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
     * Texture dimensions.
     */
    public final int width, height;

    /**
     * Creates a new VopeksImage.
     */
    public VopeksImage(Vopeks vopeks, int w, int h, int[] init) {
        this.vopeks = vopeks;
        vopeks.putTask((instance) -> {
            // DO NOT REMOVE BadGPU.TextureFlags.HasAlpha
            // NOT HAVING ALPHA KILLS PERF. ON ANDROID FOR SOME REASON.
            texture = instance.newTexture(w, h, BadGPU.TextureLoadFormat.ARGBI32, init, 0);
        });
        width = w;
        height = h;
    }

    @Override
    public void batchFlush() {
        // never written to by default.
    }

    @Override
    public void batchReference(IVopeksSurfaceHolder other) {
        // etc.
    }

    @Override
    public void batchUnreference(IVopeksSurfaceHolder caller) {
        // and so forth...
    }

    @Override
    public void getPixelsAsync(@NonNull int[] buffer, @NonNull Runnable onDone) {
        batchFlush();
        vopeks.putTask((instance) -> {
            texture.readPixels(0, 0, width, height, TextureLoadFormat.ARGBI32, buffer, 0);
            vopeks.putCallback(onDone);
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

    @Override
    public Texture getTextureFromTask() {
        return texture;
    }

    @Override
    protected void finalize() {
        dispose();
    }

    public synchronized void dispose() {
        if (!wasDisposed) {
            wasDisposed = true;
            batchFlush();
            vopeks.putTask((instance) -> {
                texture.dispose();
            });
        }
    }
}
