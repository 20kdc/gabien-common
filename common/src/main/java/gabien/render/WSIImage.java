/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.render;

import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.backend.IGaBIEn;

/**
 * This image is backed by whatever resource is necessary for the WSI side of gabien.
 * These are always mutable.
 * Copied from IImage 7th June 2023.
 */
public abstract class WSIImage {
    /**
     * The size of the image.
     */
    public final int width, height;

    /**
     * Creates a WSIImage. This is only callable by the backend.
     */
    public WSIImage(IGaBIEn backend, int w, int h) {
        GaBIEn.verify(backend);
        width = w;
        height = h;
    }

    /**
     * Writes 0xAARRGGBB data into the given buffer.
     */
    public abstract void getPixels(int[] data);

    /**
     * Like regular getPixels but allocates a buffer for you.
     */
    public final int[] getPixels() {
        int[] res = new int[width * height];
        getPixels(res);
        return res;
    }

    /**
     * Creates a PNG file.
     */
    public abstract byte[] createPNG();

    /**
     * Uploads an image to the GPU, creating an IImage.
     */
    public final IImage upload() {
        return upload(null);
    }

    /**
     * Uploads an image to the GPU, creating an IImage.
     */
    public final IImage upload(@Nullable String debugId) {
        int[] tmp = new int[width * height];
        getPixels(tmp);
        return GaBIEn.createImage(debugId, tmp, width, height);
    }

    public abstract static class RW extends WSIImage {
        public RW(IGaBIEn backend, int w, int h) {
            super(backend, w, h);
        }

        /**
         * Updates the pixels in the image.
         */
        public abstract void setPixels(int[] colours);
    }
}
