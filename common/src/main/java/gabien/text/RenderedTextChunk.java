/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import gabien.render.IGrDriver;

/**
 * A ready-to-blit rendered text chunk.
 * Notably, each chunk is considered as updating a "cursor" structure, as follows:
 * int cursorX, cursorY;
 * These updates are atomic, so no field update function affects any other.
 * There is also a constant across the entire chunk tree, highestLineHeight.
 * This constant is replicated downwards to ensure that, say, blank lines work consistently.
 * Created 7th June, 2023.
 */
public abstract class RenderedTextChunk {
    /**
     * The highest line height contained within this chunk.
     * Don't use for calculations; Compound maximizes this and then the root will call with that.
     * Pass it on.
     */
    public final int highestLineHeight;

    public RenderedTextChunk(int hlh) {
        highestLineHeight = hlh;
    }

    /**
     * Advances the cursor X.
     */
    public abstract int cursorX(int cursorXIn);

    /**
     * Advances the cursor Y.
     */
    public abstract int cursorY(int cursorYIn, int highestLineHeightIn);

    /**
     * Blits the text to an output.
     */
    public abstract void renderTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn);

    /**
     * Draws a background under text.
     */
    public abstract void backgroundTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn, int r, int g, int b, int a);

    /**
     * renderTo, but with args set to reasonable values given this is the root chunk.
     */
    public final void renderRoot(IGrDriver igd, int x, int y) {
        renderTo(igd, x, y, 0, 0, highestLineHeight);
    }

    /**
     * Draws a background under text. This version is like renderRoot.
     */
    public void backgroundRoot(IGrDriver igd, int x, int y, int r, int g, int b, int a) {
        backgroundTo(igd, x, y, 0, 0, highestLineHeight, r, g, b, a);
    }

    /**
     * Carriage return, line feed.
     */
    public static class CRLF extends RenderedTextChunk {
        public static final CRLF INSTANCE = new CRLF();

        private CRLF() {
            super(0);
        }

        @Override
        public int cursorX(int cursorXIn) {
            return 0;
        }

        @Override
        public int cursorY(int cursorYIn, int lineHeight) {
            return cursorYIn + lineHeight;
        }

        @Override
        public void renderTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn) {
        }

        @Override
        public void backgroundTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn, int r, int g, int b, int a) {
        }
    }

    /**
     * Exists for a unified API. You shouldn't ever put a compound in another compound.
     */
    public static class Compound extends RenderedTextChunk {
        private final RenderedTextChunk[] components;

        private static int maxHLH(RenderedTextChunk[] c) {
            int hlh = 0;
            for (RenderedTextChunk rtc : c) {
                if (rtc.highestLineHeight > hlh)
                    hlh = rtc.highestLineHeight;
                return hlh;
            }
            return hlh;
        }

        public Compound(RenderedTextChunk... chunks) {
            super(maxHLH(chunks));
            components = chunks;
        }

        @Override
        public int cursorX(int cursorXIn) {
            for (RenderedTextChunk rtc : components)
                cursorXIn = rtc.cursorX(cursorXIn);
            return cursorXIn;
        }

        @Override
        public int cursorY(int cursorYIn, int highestLineHeightIn) {
            for (RenderedTextChunk rtc : components)
                cursorYIn = rtc.cursorY(cursorYIn, highestLineHeightIn);
            return cursorYIn;
        }

        @Override
        public void renderTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn) {
            for (RenderedTextChunk rtc : components) {
                rtc.renderTo(igd, x, y, cursorXIn, cursorYIn, highestLineHeightIn);
                cursorXIn = rtc.cursorX(cursorXIn);
                cursorYIn = rtc.cursorY(cursorYIn, highestLineHeightIn);
            }
        }

        @Override
        public void backgroundTo(IGrDriver igd, int x, int y, int cursorXIn, int cursorYIn, int highestLineHeightIn, int r, int g, int b, int a) {
            for (RenderedTextChunk rtc : components) {
                rtc.backgroundTo(igd, x, y, cursorXIn, cursorYIn, highestLineHeightIn, r, g, b, a);
                cursorXIn = rtc.cursorX(cursorXIn);
                cursorYIn = rtc.cursorY(cursorYIn, highestLineHeightIn);
            }
        }
    }
}
