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
import gabien.natives.BadGPU.Texture;
import gabien.render.IImage;
import gabien.uslx.append.IConsumer;
import gabien.uslx.append.TimeLogger;

/**
 * Here goes nothing.
 * This being the base class of VopeksGrDriver along with IVopeksSurfaceHolder is done so that 
 *
 * Created 7th June, 2023.
 */
public class VopeksImage extends IImage {
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

    /**
     * Creates a new VopeksImage.
     */
    public VopeksImage(@NonNull Vopeks vopeks, @Nullable String id, int w, int h, int[] init) {
        super(w, h);
        this.vopeks = vopeks;
        debugId = id == null ? super.toString() : (super.toString() + ":" + id);
        vopeks.putTask((instance) -> {
            // DO NOT REMOVE BadGPU.TextureFlags.HasAlpha
            // NOT HAVING ALPHA KILLS PERF. ON ANDROID FOR SOME REASON.
            texture = instance.newTexture(w, h, BadGPU.TextureLoadFormat.ARGBI32, init, 0);
        });
    }

    /**
     * This promises that a Vopeks task will be created on behalf of the image.
     */
    public VopeksImage(Vopeks vopeks, @Nullable String id, int w, int h, IConsumer<IConsumer<BadGPU.Texture>> grabber) {
        super(w, h);
        this.vopeks = vopeks;
        debugId = id == null ? super.toString() : (super.toString() + ":" + id);
        grabber.accept((res) -> texture = res);
    }

    @Override
    public String toString() {
        return debugId;
    }

    @Override
    public void batchFlush() {
        // never written to by default.
    }

    @Override
    public void batchReference(IImage other) {
        // etc.
    }

    @Override
    public void batchUnreference(IImage caller) {
        // and so forth...
    }

    public static void getPixelsAsync(Vopeks vopeks, IImage image, int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull int[] data, int dataOfs, @NonNull Runnable onDone) {
        vopeks.putTask((instance) -> {
            try (TimeLogger.Source src = TimeLogger.open(vopeks.timeLoggerReadPixelsTask)) {
                BadGPU.Texture texture = image.getTextureFromTask();
                if (texture != null)
                    texture.readPixels(x, y, w, h, format, data, dataOfs);
            }
            vopeks.putCallback(onDone);
        });
    }

    public static void getPixelsAsync(Vopeks vopeks, IImage image, int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull byte[] data, int dataOfs, @NonNull Runnable onDone) {
        vopeks.putTask((instance) -> {
            try (TimeLogger.Source src = TimeLogger.open(vopeks.timeLoggerReadPixelsTask)) {
                BadGPU.Texture texture = image.getTextureFromTask();
                if (texture != null)
                    texture.readPixels(x, y, w, h, format, data, dataOfs);
            }
            vopeks.putCallback(onDone);
        });
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull int[] data, int dataOfs, @NonNull Runnable onDone) {
        getPixelsAsync(vopeks, this, x, y, w, h, format, data, dataOfs, onDone);
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull byte[] data, int dataOfs, @NonNull Runnable onDone) {
        getPixelsAsync(vopeks, this, x, y, w, h, format, data, dataOfs, onDone);
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
            // This is important! Otherwise, we leak batch resources.
            batchFlush();
            vopeks.putTask((instance) -> {
                if (texture != null)
                    texture.dispose();
            });
        }
    }
}
