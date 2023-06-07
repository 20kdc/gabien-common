/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import org.eclipse.jdt.annotation.NonNull;

import gabien.IGrDriver;

/**
 * Fixed-size font interface.
 * This can be, and is intentionally allowed to be, implemented both by userspace and by the gabien backend.
 * Created 16th February 2023.
 */
public interface IFixedSizeFont {
    /**
     * Gets the inter-line height in pixels.
     */
    int getSize();

    /**
     * Measures the horizontal width of the given text.
     * The text shouldn't contain newlines.
     */
    default int measureLine(@NonNull String text) {
        return measureLine(text.toCharArray(), 0, text.length());
    }

    /**
     * Measures the horizontal width of the given text.
     * The text shouldn't contain newlines.
     */
    int measureLine(@NonNull char[] text, int index, int length);

    default RenderedText renderLine(@NonNull char[] text, int index, int length, boolean textBlack) {
        // nope!
        return null;
    }

    /**
     * Draws a single line in either white or black.
     * The reason for the colour being handled this way presently involves some particularly legacy-ey APIs, and is thus still subject to change.
     * The text shouldn't contain newlines.
     */
    default void drawLine(IGrDriver igd, int x, int y, @NonNull String text, boolean textBlack) {
        drawLine(igd, x, y, text.toCharArray(), 0, text.length(), textBlack);
    }

    /**
     * Draws a single line in either white or black.
     * The reason for the colour being handled this way presently involves some particularly legacy-ey APIs, and is thus still subject to change.
     * The text shouldn't contain newlines.
     */
    void drawLine(IGrDriver igd, int x, int y, @NonNull char[] text, int index, int length, boolean textBlack);
}
