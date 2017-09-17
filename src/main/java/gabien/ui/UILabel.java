/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.GaBIEn;
import gabien.IGrDriver;
import gabien.IGrInDriver;
import gabien.IImage;

public class UILabel extends UIPanel {
    public static String fontOverride;
    public static boolean fontOverrideUE8;

    public String Text = "No notice text.";
    public int textHeight;

    // Creates a label,with text,and sets the bounds accordingly.
    public UILabel(String text, int h) {
        textHeight = h;
        Text = text;
        setBounds(getRecommendedSize(text, textHeight));
    }

    public static Rect getRecommendedSize(String text, int height) {
        int margin = height / 8;
        if (margin == 0)
            margin = 1;
        // one of the margins is "removed" because actual font glyph size should be 7, 14, etc.
        return new Rect(0, 0, getTextLength(text, height) + margin, height + margin);
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean selected, IGrInDriver igd) {
        super.updateAndRender(ox, oy, DeltaTime, selected, igd);
        drawLabel(igd, elementBounds.width, ox, oy, Text, 0, textHeight);
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

    public static void drawString(IGrDriver igd, int xptr, int oy, String text, boolean bck, int height) {
        if (useSystemFont(text, height)) {
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
            font = GaBIEn.getImageCK(fontType, 0, 0, 0);
        } else {
            font = GaBIEn.getImage(fontType);
        }
        byte[] ascii = text.getBytes();

        for (int p = 0; p < ascii.length; p++) {
            UILabel.drawChar(igd, ascii[p], font, xptr, oy, hchSize, vchSize);
            xptr += hchSize + 1;
        }
    }

    private static void drawChar(IGrDriver igd, int cc, IImage font, int xptr, int yptr, int hchSize, int vchSize) {
        if (cc < 256) {
            igd.blitImage((cc & 0x0F) * hchSize, ((cc & 0xF0) >> 4) * vchSize, hchSize, vchSize, xptr, yptr, font);
        } else {
            igd.blitImage(0, 0, hchSize, vchSize, xptr, yptr, font);
        }
    }

    public static int getTextLength(String text, int height) {
        if (useSystemFont(text, height))
            return GaBIEn.measureText(height, text);
        if (height < 8) {
            // will use fonttiny in this case
            return text.length() * 4;
        }
        return text.length() * 8;
    }

    public static int drawLabel(IGrDriver igd, int wid, int ox, int oy, String string, int mode, int height) {
        // switch from bitmaps to something else
        Rect res = getRecommendedSize(string, height);
        if (mode == 0) {
            igd.clearRect(48, 48, 48, ox, oy, wid, res.height);
        } else if (mode == 1) {
            igd.clearRect(16, 16, 16, ox, oy, wid, res.height);
        } else if (mode == 2) {
            igd.clearRect(192, 192, 192, ox, oy, wid, res.height);
        }
        int margin = height / 8;
        if (margin == 0)
            margin = 1;
        igd.clearRect(32, 32, 32, ox + margin, oy + margin, wid - (margin * 2), res.height - (margin * 2));
        UILabel.drawString(igd, ox + margin, oy + margin, string, true, height);
        return wid;
    }
}
