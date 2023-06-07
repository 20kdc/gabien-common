/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.util.concurrent.Semaphore;

import org.eclipse.jdt.annotation.NonNull;

/**
 * An image. All IImages that are not IGrDrivers must be immutable.
 * Created on 11/08/17.
 */
public interface IImage {
    /**
     * Gets the width of the image.
     */
    int getWidth();

    /**
     * Gets the height of the image.
     */
    int getHeight();

    /**
     * 0xAARRGGBB. The buffer is safe to edit, but changes do not propagate back.
     * This may be slow, because processing may not have finished on the image yet.
     * Also because this allocates a new buffer.
     */
    default @NonNull int[] getPixels() {
        int[] res = new int[getWidth() * getHeight()];
        getPixels(res);
        return res;
    }

    /**
     * Same as the other form, but with a provided buffer.
     * This may be slow, because processing may not have finished on the image yet.
     */
    default void getPixels(@NonNull int[] buffer) {
        Semaphore sm = new Semaphore(1);
        sm.acquireUninterruptibly();
        getPixelsAsync(buffer, () -> sm.release());
        sm.acquireUninterruptibly();
    }

    /**
     * 0xAARRGGBB. The buffer is safe to edit, but changes do not propagate back.
     */
    void getPixelsAsync(@NonNull int[] buffer, @NonNull Runnable onDone);

    /**
     * Downloads an IImage (presumably from the GPU) to the CPU as an IWSIImage.
     * This may be slow, similarly to performing getPixels.
     * This is a convenience method; for repetitive downloads it's better to reuse buffers.
     */
    default @NonNull IWSIImage.RW download() {
        return GaBIEn.createWSIImage(getPixels(), getWidth(), getHeight());
    }

    // Creates a PNG file.
    default @NonNull byte[] createPNG() {
        return download().createPNG();
    }
}
