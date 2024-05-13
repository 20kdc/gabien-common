/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import java.util.LinkedList;

/**
 * Text tools, tools for text.
 * Importantly, these methods don't do the caching that FontManager does, for better or worse...
 * Created 7th June, 2023.
 */
public class TextTools {
    /**
     * Renders text to a chunk.
     */
    public static RenderedTextChunk renderString(String text, IFixedSizeFont font, boolean textBlack) {
        char[] textArray = text.toCharArray();
        int textStart = 0;
        int textPtr = 0;
        LinkedList<RenderedTextChunk> chunks = new LinkedList<>();
        while (true) {
            if (textPtr == textArray.length || textArray[textPtr] == '\n') {
                // draw segment (or final segment)
                int r = textBlack ? 0 : 255;
                chunks.add(font.renderLine(textArray, textStart, textPtr - textStart, r, r, r, 255));
                if (textPtr == textArray.length)
                    break;
                chunks.add(RenderedTextChunk.CRLF.INSTANCE);
                textStart = textPtr + 1;
            }
            textPtr++;
        }
        return new RenderedTextChunk.Compound(chunks.toArray(new RenderedTextChunk[0]));
    }

    public static String formatTextFor(String text, IFixedSizeFont font, int width) {
        // This is a bunch of worst-case scenarios that should be ignored anyway
        if (width <= 0)
            return "";
        // Actually do the thing
        String[] newlines = text.split("\n", -1);
        StringBuilder work = new StringBuilder();
        if (newlines.length == 1) {
            String firstLine = newlines[0];
            while (true) {
                String nextFirstLine = "";
                boolean testLen = font.measureLine(firstLine, false) > width;
                if (testLen) {
                    // Break down words...
                    int space;
                    while (((space = firstLine.lastIndexOf(' ')) > 0) && testLen) {
                        nextFirstLine = firstLine.substring(space) + nextFirstLine;
                        firstLine = firstLine.substring(0, space);
                        testLen = font.measureLine(firstLine, false) > width;
                    }
                    // And, if need be, letters.
                    while (testLen && (firstLine.length() > 1)) {
                        int split = firstLine.length() / 2;
                        nextFirstLine = firstLine.substring(split) + nextFirstLine;
                        firstLine = firstLine.substring(0, split);
                        testLen = font.measureLine(firstLine, false) > width;
                    }
                }

                work.append(firstLine);
                firstLine = nextFirstLine;
                if (firstLine.length() > 0) {
                    work.append("\n");
                } else {
                    break;
                }
            }
        } else {
            // This causes the caching to be applied per-line.
            for (int i = 0; i < newlines.length; i++) {
                if (i != 0)
                    work.append('\n');
                work.append(formatTextFor(newlines[i], font, width));
            }
        }
        return work.toString();
    }

    /**
     * Cached paragraph object that only re-renders when necessary.
     */
    public static class PlainCached {
        private IFixedSizeFont lastFont;
        private boolean lastBlackText;
        private String lastText;
        private boolean wasInitialized;
        public IFixedSizeFont font;
        public boolean blackText;
        public String text;
        private RenderedTextChunk lastChunk;

        /**
         * This constructor doesn't initialize the PlainCached, so you don't have to set properties immediately.
         */
        public PlainCached() {
        }

        public boolean shouldRender() {
            if (!wasInitialized)
                return true;
            if (lastFont != font)
                return true;
            if (lastBlackText != blackText)
                return true;
            if (!lastText.equals(text))
                return true;
            return false;
        }

        public final void update() {
            if (shouldRender())
                forceRender();
        }

        public void forceRender() {
            wasInitialized = true;
            lastFont = font;
            lastBlackText = blackText;
            lastText = text;
            lastChunk = renderString(lastText, font, lastBlackText);
        }

        public final RenderedTextChunk getChunk() {
            return lastChunk;
        }
    }
}
