/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import org.eclipse.jdt.annotation.NonNull;

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
     * Creates a PNG file.
     */
    @NonNull byte[] createPNG();

    /**
     * Uploads an image to IImage (to be backed by BadGPU in future)
     */
    default @NonNull IImage upload() {
        int[] tmp = new int[getWidth() * getHeight()];
        getPixels(tmp);
        return GaBIEn.createImage(tmp, getWidth(), getHeight());
    }

    interface RW extends IWSIImage {
        /**
         * Updates the pixels in the image.
         */
        void setPixels(@NonNull int[] colours);
    }
}
