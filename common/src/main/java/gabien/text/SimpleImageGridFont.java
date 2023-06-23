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
public class SimpleImageGridFont implements IImmFixedSizeFont {
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
    public int measureLine(@NonNull String text, boolean withLastAdvance) {
        return measureLineCommon(advance, charWidth, text.length(), withLastAdvance);
    }

    @Override
    public int measureLine(@NonNull char[] text, int index, int length, boolean withLastAdvance) {
        return measureLineCommon(advance, charWidth, length, withLastAdvance);
    }

    public static int measureLineCommon(int advance, int charWidth, int length, boolean withLastAdvance) {
        if (withLastAdvance)
            return length * advance;
        if (length == 0)
            return 0;
        return ((length - 1) * advance) + charWidth;
    }

    @Override
    public void drawLine(@NonNull IGrDriver igd, int x, int y, @NonNull char[] text, int index, int length, boolean textBlack) {
        final IImage font = textBlack ? fontBlack : fontWhite;
        int lim = index + length;
        for (int p = index; p < lim; p++) {
            int cc = text[p];
            if (cc < 256) {
                igd.blitImage((cc % charsPerRow) * charWidth, (cc / charsPerRow) * charHeight, charWidth, charHeight, x, y, font);
            } else {
                igd.blitImage(0, 0, charWidth, charHeight, x, y, font);
            }
            x += advance;
        }
    }

    @Override
    public void drawLine(@NonNull IGrDriver igd, int x, int y, @NonNull String text, boolean textBlack) {
        final IImage font = textBlack ? fontBlack : fontWhite;
        int length = text.length();
        for (int p = 0; p < length; p++) {
            int cc = text.charAt(p);
            if (cc < 256) {
                igd.blitImage((cc % charsPerRow) * charWidth, (cc / charsPerRow) * charHeight, charWidth, charHeight, x, y, font);
            } else {
                igd.blitImage(0, 0, charWidth, charHeight, x, y, font);
            }
            x += advance;
        }
    }

    @Override
    public void drawBackground(IGrDriver igd, int x, int y, @NonNull char[] text, int index, int length, int r, int g, int b, int a) {
        drawBackgroundCommon(igd, x, y, length, r, g, b, a);
    }

    @Override
    public void drawBackground(IGrDriver igd, int x, int y, @NonNull String text, int r, int g, int b, int a) {
        drawBackgroundCommon(igd, x, y, text.length(), r, g, b, a);
    }

    private void drawBackgroundCommon(IGrDriver igd, int x, int y, int length, int r, int g, int b, int a) {
        ImageRenderedTextChunk.background(igd, x, y, measureLineCommon(advance, charWidth, length, false), charHeight, 1, r, g, b, a);
    }
}
