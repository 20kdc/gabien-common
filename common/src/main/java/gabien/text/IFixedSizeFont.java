/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Fixed-size font interface.
 * This can be, and is intentionally allowed to be, implemented both by userspace and by the gabien backend.
 * Created 16th February 2023.
 */
public interface IFixedSizeFont {
    /**
     * Gets the inter-line height in pixels.
     */
    int getLineHeight();

    /**
     * Gets the height of content in this font.
     * This is distinct from the line height.
     */
    int getContentHeight();

    /**
     * Measures the horizontal width of the given text.
     * The text shouldn't contain newlines.
     */
    int measureLine(@NonNull String text, boolean withLastAdvance);

    /**
     * Measures the horizontal width of the given text.
     * The text shouldn't contain newlines.
     */
    int measureLine(@NonNull char[] text, int index, int length, boolean withLastAdvance);

    /**
     * The fancy new way to render text that's more GPU-aware.
     */
    RenderedTextChunk renderLine(@NonNull String text, boolean textBlack);

    /**
     * The fancy new way to render text that's more GPU-aware.
     */
    RenderedTextChunk renderLine(@NonNull char[] text, int index, int length, boolean textBlack);
}
