/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.natives.BadGPU;
import gabien.render.IImage;
import gabien.uslx.append.TimeLogger;

/**
 * Here goes nothing.
 * This being the base class of VopeksGrDriver along with IVopeksSurfaceHolder is done so that 
 *
 * Created 7th June, 2023.
 */
public final class VopeksImage extends IImage {
    /**
     * The parent instance.
     */
    public final Vopeks vopeks;

    private volatile boolean wasDisposed;

    /**
     * Creates a new VopeksImage.
     */
    public VopeksImage(@NonNull Vopeks vopeks, @Nullable String id, int w, int h, BadGPU.TextureLoadFormat tlf, int[] init) {
        super(id, w, h);
        this.vopeks = vopeks;
        vopeks.putTask((instance) -> {
            texture = instance.newTexture(w, h, tlf, init, 0);
        });
    }

    /**
     * Creates a new VopeksImage.
     */
    public VopeksImage(@NonNull Vopeks vopeks, @Nullable String id, int w, int h, BadGPU.TextureLoadFormat tlf, byte[] init) {
        super(id, w, h);
        this.vopeks = vopeks;
        vopeks.putTask((instance) -> {
            texture = instance.newTexture(w, h, tlf, init, 0);
        });
    }

    /**
     * This promises that a Vopeks task will be created on behalf of the image.
     */
    public VopeksImage(Vopeks vopeks, @Nullable String id, int w, int h, Consumer<Consumer<BadGPU.Texture>> grabber) {
        super(id, w, h);
        this.vopeks = vopeks;
        grabber.accept((res) -> texture = res);
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
    protected void finalize() {
        shutdown();
    }

    public synchronized void shutdown() {
        if (!wasDisposed) {
            wasDisposed = true;
            vopeks.putTask((instance) -> {
                if (texture != null)
                    texture.dispose();
            });
        }
    }
}
