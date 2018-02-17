/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

/**
 * Just get this out of UILabel so I can continue doing meaningful stuff.
 * Created on 16th February 2018.
 */
public class FontManager {
    // Font override name.
    public static String fontOverride;
    public static boolean fontOverrideUE8;

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

    public static void drawString(IGrDriver igd, int xptr, int oy, String text, boolean bck, int height) {
        if (!bck)
            igd.clearRect(0, 0, 0, xptr - 1, oy - 1, getLineLength(text, height) + 1, height + 1);
        if (useSystemFont(text, height)) {
            int lIdx;
            while ((lIdx = text.indexOf('\n')) != -1) {
                igd.drawText(xptr, oy, 255, 255, 255, height, text.substring(0, lIdx));
                text = text.substring(lIdx + 1);
            }
            igd.drawText(xptr, oy, 255, 255, 255, height, text);
            return;
        }
        String fontType = "fonttiny.png";
        int hchSize = 3;
        int vchSize = 5;
        if (height >= 16) {
            fontType = "font2x.png";
            hchSize = 7;
            vchSize = 14;
        } else if (height >= 8) {
            fontType = "font.png";
            hchSize = 7;
            vchSize = 7;
        }
        IImage font;
        if (bck) {
            font = GaBIEn.getImageCKEx(fontType, false, true, 0, 0, 0);
        } else {
            font = GaBIEn.getImageEx(fontType, false, true);
        }
        byte[] ascii = text.getBytes();

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
