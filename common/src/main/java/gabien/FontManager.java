/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.text.IFixedSizeFont;
import gabien.text.RenderedTextChunk;
import gabien.text.SimpleImageGridFont;
import gabien.ui.Size;

import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Just get this out of UILabel so I can continue doing meaningful stuff.
 * Created on 16th February 2018.
 */
public class FontManager {
    // Font override name.
    public static String fontOverride;
    public static boolean fontOverrideUE8;
    public static boolean fontsReady;

    private static SimpleImageGridFont internalFont6;
    private static SimpleImageGridFont internalFont8;
    private static SimpleImageGridFont internalFont16;

    private static ReentrantLock formatLock = new ReentrantLock();
    // Key format is a weird mess, check the relevant function
    private static WeakHashMap<String, String> formatData = new WeakHashMap<String, String>();

    static void setupFonts() {
        IImage f16 = GaBIEn.getImageCKEx("font2x.png", false, true, 0, 0, 0);
        IImage f8 = GaBIEn.getImageCKEx("font.png", false, true, 0, 0, 0);
        IImage f6 = GaBIEn.getImageCKEx("fonttiny.png", false, true, 0, 0, 0);
        //                                            W  H   C   A  S
        internalFont16 = new SimpleImageGridFont(f16, 7, 14, 16, 8, 16);
        internalFont8 =  new SimpleImageGridFont(f8,  7,  7, 16, 8,  8);
        internalFont6 =  new SimpleImageGridFont(f6,  3,  5, 16, 4,  6);
    }

    /**
     * Returns an internal font.
     * @param height The target font height (pixels per line).
     * @return An internal font with a 128-character image covering ASCII (with some codepage 437)
     */
    public static IFixedSizeFont getInternalFontFor(int height) {
        if (height >= 16) {
            return internalFont16;
        } else if (height >= 8) {
            return internalFont8;
        } else {
            return internalFont6;
        }
    }

    private static boolean useSystemFont(String text, int height) {
        if (fontOverride != null) {
            if (height > 8)
                return true;
            // <= 8 - still use system?
            if (fontOverrideUE8)
                return true;
        }
        // Does the font exist?
        if (height != 16)
            if (height != 8)
                if (height != 6)
                    return true;
        // Finally resolved to use internal if possible - is this allowed?
        int l = text.length();
        for (int p = 0; p < l; p++)
            if (text.charAt(p) >= 128)
                return true;
        return false;
    }

    public static IFixedSizeFont getFontForText(String text, int height) {
        if (useSystemFont(text, height))
            return GaBIEn.getNativeFont(height, fontOverride, true);
        return getInternalFontFor(height);
    }

    /**
     * Please don't use this, it bleeds performance.
     */
    public static void drawString(IGrDriver igd, int xptr, int oy, String text, boolean noBackground, boolean textBlack, int height) {
        RenderedTextChunk rtc = renderString(text, getFontForText(text, height), textBlack);
        int cc = textBlack ? 255 : 0;
        if (!noBackground)
            rtc.backgroundRoot(igd, xptr, oy, cc, cc, cc, 255);
        rtc.renderRoot(igd, xptr, oy);
    }

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
                chunks.add(font.renderLine(textArray, textStart, textPtr - textStart, textBlack));
                if (textPtr == textArray.length)
                    break;
                chunks.add(RenderedTextChunk.CRLF.INSTANCE);
                textStart = textPtr + 1;
            }
            textPtr++;
        }
        return new RenderedTextChunk.Compound(chunks.toArray(new RenderedTextChunk[0]));
    }

    // NOTE: This assumes the results are for the final content block.
    //       So it doesn't include the padding at the bottom.
    public static Size getTextSize(String text, int textHeight) {
        int w = 0;
        int h = textHeight;
        while (text.length() > 0) {
            int nlI = text.indexOf('\n');
            String tLine = text;
            if (nlI != -1) {
                tLine = tLine.substring(0, nlI);
                text = text.substring(nlI + 1);
                // Another line incoming, add pre-emptively.
                h += textHeight;
            } else {
                text = "";
            }
            w = Math.max(w, getLineLength(tLine, textHeight));
        }
        return new Size(w, h - (textHeight / 8));
    }

    public static int getLineLength(String text, int height) {
        return getFontForText(text, height).measureLine(text);
    }

    public static String formatTextFor(String text, int textHeight, int width) {
        // This is a bunch of worst-case scenarios that should be ignored anyway
        if (width <= 0)
            return "";
        String fo = fontOverride;
        String key = (fo == null ? "<NULL, NOBODY WOULD NAME A FONT THIS, IF YOU DO, PLEASE DON'T>" : fo) + ";`bird`;" + text + ";`bird`;" + width + ";" + textHeight;
        String res;
        // This takes a while, and is a critical path, particularly on Android.
        // So *cache it*.
        formatLock.lock();
        res = formatData.get(key);
        formatLock.unlock();
        if (res != null)
            return res;
        // Actually do the thing
        IFixedSizeFont font = getFontForText(text, textHeight);
        String[] newlines = text.split("\n", -1);
        StringBuilder work = new StringBuilder();
        if (newlines.length == 1) {
            String firstLine = newlines[0];
            while (true) {
                String nextFirstLine = "";
                boolean testLen = font.measureLine(firstLine) > width;
                if (testLen) {
                    // Break down words...
                    int space;
                    while (((space = firstLine.lastIndexOf(' ')) > 0) && testLen) {
                        nextFirstLine = firstLine.substring(space) + nextFirstLine;
                        firstLine = firstLine.substring(0, space);
                        testLen = font.measureLine(firstLine) > width;
                    }
                    // And, if need be, letters.
                    while (testLen && (firstLine.length() > 1)) {
                        int split = firstLine.length() / 2;
                        nextFirstLine = firstLine.substring(split) + nextFirstLine;
                        firstLine = firstLine.substring(0, split);
                        testLen = font.measureLine(firstLine) > width;
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
                work.append(formatTextFor(newlines[i], textHeight, width));
            }
        }
        formatLock.lock();
        formatData.put(key, res = work.toString());
        formatLock.unlock();
        return res;
    }
}
