/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import org.eclipse.jdt.annotation.NonNull;

import gabien.render.IGrDriver;

/**
 * Immediate-drawable font interface.
 * Created 23rd June, 2023.
 */
public interface IImmFixedSizeFont extends IFixedSizeFont {
    /**
     * Immediately render the line to a surface.
     */
    void drawLine(@NonNull IGrDriver igd, int x, int y, @NonNull String text, int r, int g, int b, int a);

    /**
     * Immediately render the line to a surface.
     */
    void drawLine(@NonNull IGrDriver igd, int x, int y, @NonNull char[] text, int index, int length, int r, int g, int b, int a);

    /**
     * Immediately render the background to a surface.
     */
    void drawBackground(IGrDriver igd, int x, int y, @NonNull String text, int r, int g, int b, int a);

    /**
     * Immediately render the background to a surface.
     */
    void drawBackground(IGrDriver igd, int x, int y, @NonNull char[] text, int index, int length, int r, int g, int b, int a);

    /**
     * Immediately render the line and background to a surface.
     */
    default void drawLAB(IGrDriver igd, int x, int y, @NonNull String text, boolean textBlack) {
        int l = textBlack ? 255 : 0;
        int c = textBlack ? 0 : 255;
        drawBackground(igd, x, y, text, l, l, l, 255);
        drawLine(igd, x, y, text, c, c, c, 255);
    }

    /**
     * Immediately render the line and background to a surface.
     */
    default void drawLAB(IGrDriver igd, int x, int y, @NonNull char[] text, int index, int length, boolean textBlack) {
        int l = textBlack ? 255 : 0;
        int c = textBlack ? 0 : 255;
        drawBackground(igd, x, y, text, index, length, l, l, l, 255);
        drawLine(igd, x, y, text, index, length, c, c, c, 255);
    }

    /**
     * Fallback for compat. with non-immediate fonts
     */
    @Override
    default RenderedTextChunk renderLine(@NonNull String text, int r, int g, int b, int a) {
        final int wla = measureLine(text, true);
        return new RenderedTextChunk(getLineHeight(), getContentHeight(), 0) {

            @Override
            public void renderTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn) {
                drawLine(igd, x + cursorXIn, y + cursorYIn, text, r, g, b, a);
            }

            @Override
            public int cursorX(int cursorXIn) {
                return cursorXIn + wla;
            }

            @Override
            public int cursorY(int cursorYIn, int highestLineHeightIn) {
                return cursorYIn;
            }

            @Override
            public void backgroundTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn, int r, int g, int b, int a) {
                drawBackground(igd, x + cursorXIn, y + cursorYIn, text, r, g, b, a);
            }
        };
    }

    /**
     * Fallback for compat. with non-immediate fonts
     */
    default RenderedTextChunk renderLine(@NonNull char[] text, int index, int length, int r, int g, int b, int a) {
        final int wla = measureLine(text, index, length, true);
        return new RenderedTextChunk(getLineHeight(), getContentHeight(), 0) {

            @Override
            public void renderTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn) {
                drawLine(igd, x + cursorXIn, y + cursorYIn, text, index, length, r, g, b, a);
            }

            @Override
            public int cursorX(int cursorXIn) {
                return cursorXIn + wla;
            }

            @Override
            public int cursorY(int cursorYIn, int highestLineHeightIn) {
                return cursorYIn;
            }

            @Override
            public void backgroundTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn, int r, int g, int b, int a) {
                drawBackground(igd, x + cursorXIn, y + cursorYIn, text, index, length, r, g, b, a);
            }
        };
    }
}
