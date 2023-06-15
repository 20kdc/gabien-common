/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.render;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.IImage;

/**
 * This image is backed by whatever resource is necessary for the WSI side of gabien.
 * These are always mutable.
 * Copied from IImage 7th June 2023.
 */
public interface IWSIImage {
    /**
     * Gets the width of the image.
     */
    int getWidth();

    /**
     * Gets the height of the image.
     */
    int getHeight();

    /**
     * Writes 0xAARRGGBB data into the given buffer.
     */
    void getPixels(@NonNull int[] data);

    /**
     * Like regular getPixels but allocates a buffer for you.
     */
    default @NonNull int[] getPixels() {
        int[] res = new int[getWidth() * getHeight()];
        getPixels(res);
        return res;
    }

    /**
     * Creates a PNG file.
     */
    @NonNull byte[] createPNG();

    /**
     * Uploads an image to the GPU, creating an IImage.
     */
    default @NonNull IImage upload() {
        return upload(null);
    }

    /**
     * Uploads an image to the GPU, creating an IImage.
     */
    default @NonNull IImage upload(@Nullable String debugId) {
        int[] tmp = new int[getWidth() * getHeight()];
        getPixels(tmp);
        return GaBIEn.createImage(debugId, tmp, getWidth(), getHeight());
    }

    interface RW extends IWSIImage {
        /**
         * Updates the pixels in the image.
         */
        void setPixels(@NonNull int[] colours);
    }
}