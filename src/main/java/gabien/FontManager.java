/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.ui.Size;

/**
 * Just get this out of UILabel so I can continue doing meaningful stuff.
 * Created on 16th February 2018.
 */
public class FontManager {
    // Font override name.
    public static String fontOverride;
    public static boolean fontOverrideUE8;
    public static boolean fontsReady;

    private static IImage internalFont6, internalFont6B;
    private static IImage internalFont8, internalFont8B;
    private static IImage internalFont16, internalFont16B;

    private static IImage getInternalFontFor(int height, boolean textBlack) {
        if (height >= 16) {
            if (internalFont16 == null)
                internalFont16 = GaBIEn.getImageCKEx("font2x.png", false, true, 0, 0, 0);
            if (textBlack) {
                if (internalFont16B == null)
                    internalFont16B = processInvertText(internalFont16);
                return internalFont16B;
            }
            return internalFont16;
        } else if (height >= 8) {
            if (internalFont8 == null)
                internalFont8 = GaBIEn.getImageCKEx("font.png", false, true, 0, 0, 0);
            if (textBlack) {
                if (internalFont8B == null)
                    internalFont8B = processInvertText(internalFont8);
                return internalFont8B;
            }
            return internalFont8;
        } else {
            if (internalFont6 == null)
                internalFont6 = GaBIEn.getImageCKEx("fonttiny.png", false, true, 0, 0, 0);
            if (textBlack) {
                if (internalFont6B == null)
                    internalFont6B = processInvertText(internalFont6);
                return internalFont6B;
            }
            return internalFont6;
        }
    }

    private static IImage processInvertText(IImage internalFont16) {
        int[] px = internalFont16.getPixels();
        for (int i = 0; i < px.length; i++) {
            px[i] &= 0xFF000000;
        }
        return GaBIEn.createImage(px, internalFont16.getWidth(), internalFont16.getHeight());
    }

    public static void resetInternalFonts() {
        internalFont6 = null;
        internalFont8 = null;
        internalFont16 = null;
        internalFont6B = null;
        internalFont8B = null;
        internalFont16B = null;
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

    public static void drawString(IGrDriver igd, int xptr, int oy, String text, boolean noBackground, boolean textBlack, int height) {
        int cc = textBlack ? 255 : 0;
        if (!noBackground) {
            int lIdx;
            String workingText = text + '\n';
            int toy = oy;
            while ((lIdx = workingText.indexOf('\n')) != -1) {
                igd.clearRect(cc, cc, cc, xptr - 1, toy - 1, getLineLength(workingText, height) + 1, height + 1);
                workingText = workingText.substring(lIdx + 1);
                toy += height;
            }
        }
        if (useSystemFont(text, height)) {
            int lIdx;
            while ((lIdx = text.indexOf('\n')) != -1) {
                igd.drawText(xptr, oy, 255, 255, 255, height, text.substring(0, lIdx));
                text = text.substring(lIdx + 1);
                oy += height;
            }
            igd.drawText(xptr, oy, 255, 255, 255, height, text);
            return;
        }
        byte[] ascii = text.getBytes();
        IImage font = getInternalFontFor(height, textBlack);
        int hchSize = font.getWidth() / 16;
        int vchSize = font.getHeight() / 8;
        int oldXPtr = xptr;
        for (int p = 0; p < ascii.length; p++) {
            if (ascii[p] == 10) {
                xptr = oldXPtr;
                oy += height;
            } else {
                drawChar(igd, ascii[p], font, xptr, oy, hchSize, vchSize);
                xptr += hchSize + 1;
            }
        }
    }

    private static void drawChar(IGrDriver igd, int cc, IImage font, int xptr, int yptr, int hchSize, int vchSize) {
        if (cc < 256) {
            igd.blitImage((cc & 0x0F) * hchSize, ((cc & 0xF0) >> 4) * vchSize, hchSize, vchSize, xptr, yptr, font);
        } else {
            igd.blitImage(0, 0, hchSize, vchSize, xptr, yptr, font);
        }
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
        if (useSystemFont(text, height))
            return GaBIEn.measureText(height, text);
        if (height < 8) {
            // will use fonttiny in this case
            return text.length() * 4;
        }
        return text.length() * 8;
    }

    public static String formatTextFor(String text, int textHeight, int width) {
        // Best used for reading the IPCRESS file.
        StringBuilder work = new StringBuilder();
        while (text.length() > 0) {
            String firstLine = text;
            int firstLinePtrIdx = firstLine.indexOf('\n');
            if (firstLinePtrIdx != -1) {
                text = firstLine.substring(firstLinePtrIdx + 1);
                firstLine = firstLine.substring(0, firstLinePtrIdx);
            } else {
                text = "";
            }
            boolean testLen = getLineLength(firstLine, textHeight) > width;
            String spaceAppend = (text.length() > 0) ? "\n" : "";
            if (testLen) {
                int space = firstLine.lastIndexOf(' ');
                while ((space > 0) && testLen) {
                    text = " " + firstLine.substring(space + 1) + spaceAppend + text;
                    spaceAppend = "";
                    firstLine = firstLine.substring(0, space);
                    testLen = getLineLength(firstLine, textHeight) > width;
                    space = firstLine.lastIndexOf(' ');
                }
            }
            while (testLen && (firstLine.length() > 0)) {
                text = firstLine.substring(firstLine.length() - 1) + text;
                firstLine = firstLine.substring(0, firstLine.length() - 1);
                testLen = getLineLength(firstLine, textHeight) > width;
            }
            if (firstLine.length() == 0)
                return "";
            work.append(firstLine);
            if ((text.length() > 0) || (firstLinePtrIdx != -1))
                work.append('\n');
        }
        return work.toString();
    }
}
