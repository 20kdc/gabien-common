/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

/**
 * Mutable, but fixed-size, CPU-side image.
 * Copied from IImage 7th June 2023.
 */
public final class CPUImage {
    /**
     * 0xAARRGGBB. The buffer is safe to edit.
     */
    public final int[] colours;
    public final int width;
    public final int height;

    public CPUImage(int[] colours, int width, int height) {
        if (colours.length != (width * height))
            throw new IllegalArgumentException("CPUImage colours buffer must match width/height");
        this.colours = colours;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a PNG file.
     */
    public byte[] createPNG() {
        return GaBIEn.createPNG(colours, width, height);
    }
}
