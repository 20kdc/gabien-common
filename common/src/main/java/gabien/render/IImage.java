/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.render;

import java.util.concurrent.Semaphore;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;
import gabien.natives.BadGPUEnum.TextureLoadFormat;

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
 * 9th July 2023: Made into an abstract class.
 */
public abstract class IImage implements IImgRegion {
    /**
     * Texture dimensions.
     */
    public final int width, height;

    /**
     * The texture.
     * This is only guaranteed to exist on the instance thread.
     */
    protected @Nullable BadGPU.Texture texture;

    /**
     * ID for debugging.
     */
    public final String debugId;

    public IImage(@Nullable String id, int w, int h) {
        width = w;
        height = h;
        debugId = id == null ? super.toString() : (super.toString() + ":" + id);
    }

    @Override
    public final String toString() {
        return debugId;
    }

    /**
     * Gets the width of the image.
     */
    public final int getWidth() {
        return width;
    }

    /**
     * Gets the height of the image.
     */
    public final int getHeight() {
        return height;
    }

    @Override
    public final float getRegionWidth() {
        return width;
    }

    @Override
    public final float getRegionHeight() {
        return height;
    }

    /**
     * 0xAARRGGBB. The buffer is safe to edit, but changes do not propagate back.
     * This may be slow, because processing may not have finished on the image yet.
     * Also because this allocates a new buffer.
     */
    public final int[] getPixels() {
        int[] res = new int[width * height];
        getPixels(res);
        return res;
    }

    /**
     * Same as the other form, but with a provided buffer.
     * This may be slow, because processing may not have finished on the image yet.
     */
    public final void getPixels(int[] buffer) {
        getSurface().batchFlush();
        Semaphore sm = new Semaphore(1);
        sm.acquireUninterruptibly();
        getPixelsAsync(buffer, () -> sm.release());
        sm.acquireUninterruptibly();
    }

    /**
     * 0xAARRGGBB. The buffer is safe to edit, but changes do not propagate back.
     */
    public final void getPixelsAsync(int[] buffer, Runnable onDone) {
        getPixelsAsync(0, 0, width, height, TextureLoadFormat.RGBA8888, buffer, 0, () -> {
            // If it gets here, it's in-bounds.
            BadGPUUnsafe.pixelsConvertRGBA8888ToARGBI32InPlaceI(width, height, buffer, 0);
            BadGPUUnsafe.pixelsConvertARGBI32PremultipliedToStraightInPlaceI(width, height, buffer, 0);
            onDone.run();
        });
    }

    public abstract void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, int[] data, int dataOfs, Runnable onDone);
    public abstract void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, byte[] data, int dataOfs, Runnable onDone);

    /**
     * Downloads an IImage (presumably from the GPU) to the CPU as an IWSIImage.
     * This may be slow, similarly to performing getPixels.
     * This is a convenience method; for repetitive downloads it's better to reuse buffers.
     */
    public final WSIImage.RW download() {
        return GaBIEn.createWSIImage(getPixels(), width, height);
    }

    // Creates a PNG file.
    public final byte[] createPNG() {
        return download().createPNG();
    }

    /**
     * Flushes any queued operations to the Vopeks task queue.
     * This ensures that any operations you subsequently schedule run after the preceding operations.
     */
    public abstract void batchFlush();

    /**
     * This is used when the caller is writing this image into its own batch.
     * Ensures that any changes to this image first flush the batch of the caller.
     * This ensures that the ordering remains correct.
     */
    public abstract void batchReference(IImage caller);

    /**
     * This is used when the caller has flushed a batch that was using this image.
     * It cleans up the previously established reference, preventing a memory leak.
     */
    public abstract void batchUnreference(IImage caller);

    /**
     * Gets a texture. Run from Vopeks task code.
     * This can return null if the texture is empty (0 width, 0 height).
     */
    public final @Nullable BadGPU.Texture getTextureFromTask() {
        return texture;
    }

    @Override
    public final float getS(float x, float y) {
        return x / width;
    }

    @Override
    public final float getT(float x, float y) {
        return y / height;
    }

    @Override
    @NonNull
    public final IImage getSurface() {
        return this;
    }

    @Override
    @NonNull
    public final IImgRegion subRegion(float x, float y, float w, float h) {
        return new ImageTexRegion(this, x, y, w, h, width, height);
    }

    /**
     * Determines if the contents of this IImage are completely zero-alpha.
     * Expensive but can save rendering time in the long run.
     */
    public boolean areContentsZeroAlpha() {
        int[] gp = getPixels();
        for (int i = 0; i < gp.length; i++)
            if ((gp[i] & 0xFF000000) != 0)
                return false;
        return true;
    }
}
