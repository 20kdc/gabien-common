/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.util.concurrent.Semaphore;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;
import gabien.natives.BadGPUEnum.TextureLoadFormat;
import gabien.render.ITexRegion;
import gabien.render.IWSIImage;

/**
 * An image. All IImages that are not IGrDrivers must be immutable.
 * In theory, this was created on 11/08/17.
 * In practice, this was an inner interface of IGraphicsDriver back in the very beginning.
 * The earliest version on record is from August 3rd, 2014.
 * By the end of 2016, which was when R48 started, it was still part of IGrInDriver.
 * R48 0.7.1 finally separated out the interface. (The class compile date is August 11th, 2017.)
 * IAWTImageLike was introduced sometime around June 9th, 2017.
 * It then became INativeImageHolder sometime around August 21st, 2017.
 * On the 7th June, 2023, it became IVopeksSurfaceHolder.
 * Now it's 9th June 2023. Vopeks is pretty much certain; we're not going back, so everything was merged.
 */
public interface IImage extends ITexRegion {
    /**
     * Gets the width of the image.
     */
    int getWidth();

    /**
     * Gets the height of the image.
     */
    int getHeight();

    @Override
    default float getRegionWidth() {
        return getWidth();
    }

    @Override
    default float getRegionHeight() {
        return getHeight();
    }

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
        getSurface().batchFlush();
        Semaphore sm = new Semaphore(1);
        sm.acquireUninterruptibly();
        getPixelsAsync(buffer, () -> sm.release());
        sm.acquireUninterruptibly();
    }

    /**
     * 0xAARRGGBB. The buffer is safe to edit, but changes do not propagate back.
     */
    default void getPixelsAsync(@NonNull int[] buffer, @NonNull Runnable onDone) {
        final int w = getWidth();
        final int h = getHeight();
        getPixelsAsync(0, 0, w, h, TextureLoadFormat.RGBA8888, buffer, 0, () -> {
            // If it gets here, it's in-bounds.
            BadGPUUnsafe.pixelsConvertRGBA8888ToARGBI32InPlaceI(w, h, buffer, 0);
            onDone.run();
        });
    }

    void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull int[] data, int dataOfs, @NonNull Runnable onDone);
    void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull byte[] data, int dataOfs, @NonNull Runnable onDone);

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

    /**
     * Flushes any queued operations to the Vopeks task queue.
     * This ensures that any operations you subsequently schedule run after the preceding operations.
     */
    void batchFlush();

    /**
     * This is used when the caller is writing this image into its own batch.
     * Ensures that any changes to this image first flush the batch of the caller.
     * This ensures that the ordering remains correct.
     */
    void batchReference(IImage caller);

    /**
     * This is used when the caller has flushed a batch that was using this image.
     * It cleans up the previously established reference, preventing a memory leak.
     */
    void batchUnreference(IImage caller);

    /**
     * Gets a texture. Run from Vopeks task code.
     * This can return null if the texture is empty (0 width, 0 height).
     */
    @Nullable BadGPU.Texture getTextureFromTask();

    @Override
    default float getS(float x, float y) {
        return x / getWidth();
    }

    @Override
    default float getT(float x, float y) {
        return y / getHeight();
    }

    @Override
    @NonNull
    default IImage getSurface() {
        return this;
    }
}
