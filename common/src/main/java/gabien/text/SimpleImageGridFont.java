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
    public RenderedTextChunk renderLine(@NonNull String text, boolean textBlack) {
        return renderLineWithOwnedCA(text.toCharArray(), textBlack);
    }

    @Override
    public RenderedTextChunk renderLine(@NonNull char[] text, int index, int length, boolean textBlack) {
        char[] tmp = new char[length];
        System.arraycopy(text, index, tmp, 0, length);
        return renderLineWithOwnedCA(tmp, textBlack);
    }

    private RenderedTextChunk renderLineWithOwnedCA(@NonNull char[] text, boolean textBlack) {
        // :(
        final IImage font = textBlack ? fontBlack : fontWhite;
        final int measureX = measureLine(text, 0, text.length);
        return new RenderedTextChunk(size) {
            @Override
            public void backgroundTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn, int r, int g, int b, int a) {
                int margin = 1;
                int margin2 = margin * 2;
                igd.clearRectAlpha(r, g, b, a, x + cursorXIn - margin, y + cursorYIn - margin, measureX + margin2, highestLineHeight + margin2);
            }
            @Override
            public int cursorX(int cursorXIn) {
                return cursorXIn + (text.length * advance);
            }
            @Override
            public int cursorY(int cursorYIn, int highestLineHeightIn) {
                return cursorYIn;
            }
            @Override
            public void renderTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn) {
                x += cursorXIn;
                y += cursorYIn;
                for (int p = 0; p < text.length; p++) {
                    int cc = text[p];
                    if (cc < 256) {
                        igd.blitImage((cc % charsPerRow) * charWidth, (cc / charsPerRow) * charHeight, charWidth, charHeight, x, y, font);
                    } else {
                        igd.blitImage(0, 0, charWidth, charHeight, x, y, font);
                    }
                    x += advance;
                }
            }
        };
    }
}
