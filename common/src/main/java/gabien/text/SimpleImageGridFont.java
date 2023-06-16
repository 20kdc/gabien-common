/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import org.eclipse.jdt.annotation.NonNull;

import gabien.GaBIEn;
import gabien.render.IGrDriver;
import gabien.render.IImage;

/**
 * Implementation of a simple image-grid font.
 * This is intended to be high-performance blitting, so this does as little fancy stuff as possible.
 * Created 16th February 2023.
 */
public class SimpleImageGridFont implements IFixedSizeFont {
    public final IImage fontWhite, fontBlack;
    public final int charWidth, charHeight, charsPerRow, advance, size;

    public SimpleImageGridFont(IImage base, int charWidth, int charHeight, int charsPerRow, int advance, int size) {
        fontWhite = base;
        int[] px = base.getPixels();
        for (int i = 0; i < px.length; i++)
            px[i] &= 0xFF000000;
        fontBlack = GaBIEn.createImage(px, base.getWidth(), base.getHeight());
        this.charWidth = charWidth;
        this.charHeight = charHeight;
        this.charsPerRow = charsPerRow;
        this.advance = advance;
        this.size = size;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int measureLine(@NonNull String text) {
        if (text.length() == 0)
            return 0;
        return ((text.length() - 1) * advance) + charWidth;
    }

    @Override
    public int measureLine(@NonNull char[] text, int index, int length) {
        if (length == 0)
            return 0;
        return ((length - 1) * advance) + charWidth;
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull String text, boolean textBlack) {
        int width = measureLine(text);
        IGrDriver igd = GaBIEn.makeOffscreenBuffer(width, charHeight);
        // :(
        IImage font = textBlack ? fontBlack : fontWhite;
        int x = 0;
        int l = text.length();
        for (int p = 0; p < l; p++) {
            int cc = text.charAt(p);
            if (cc < 256) {
                igd.blitImage((cc % charsPerRow) * charWidth, (cc / charsPerRow) * charHeight, charWidth, charHeight, x, 0, font);
            } else {
                igd.blitImage(0, 0, charWidth, charHeight, x, 0, font);
            }
            x += advance;
        }
        return new ImageRenderedTextChunk.GPU(0, 0, width, size, igd);
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull char[] text, int index, int length, boolean textBlack) {
        int width = measureLine(text, index, length);
        IGrDriver igd = GaBIEn.makeOffscreenBuffer(width, charHeight);
        // :(
        IImage font = textBlack ? fontBlack : fontWhite;
        int x = 0;
        for (int p = 0; p < length; p++) {
            int cc = text[index + p];
            if (cc < 256) {
                igd.blitImage((cc % charsPerRow) * charWidth, (cc / charsPerRow) * charHeight, charWidth, charHeight, x, 0, font);
            } else {
                igd.blitImage(0, 0, charWidth, charHeight, x, 0, font);
            }
            x += advance;
        }
        return new ImageRenderedTextChunk.GPU(0, 0, width, size, igd);
    }
}
