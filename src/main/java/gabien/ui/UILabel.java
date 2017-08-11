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
        drawLabel(igd, elementBounds.width, ox, oy, Text, false, textHeight);
    }

    private static boolean useSystemFont(String text, int height) {
        if (height != 16)
            if (height != 8)
                if (height != 6)
                    return true;
        int l = text.length();
        for (int p = 0; p < l; p++)
            if (text.charAt(p) >= 128)
                return true;
        return fontOverride != null;
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

    private static int drawLabelx1(IGrDriver igd, int wid, int ox, int oy, String string, boolean selected) {
        IImage i = GaBIEn.getImage("textButton.png");
        igd.blitImage(selected ? 3 : 0, 10, 1, 9, ox, oy, i);
        for (int pp = 1; pp < wid - 1; pp++)
            igd.blitImage(selected ? 4 : 1, 10, 1, 9, ox + pp, oy, i);
        igd.blitImage(selected ? 5 : 2, 10, 1, 9, ox + (wid - 1), oy, i);

        drawString(igd, ox + 1, oy + 1, string, true, 8);
        return wid;
    }

    private static int drawLabelx2(IGrDriver igd, int wid, int ox, int oy, String string, boolean selected) {
        IImage i = GaBIEn.getImage("textButton.png");
        int selectedOfs = selected ? 6 : 0;
        igd.blitImage(6 + selectedOfs, 20, 2, 18, ox, oy, i);
        for (int pp = 2; pp < wid - 1; pp += 2)
            igd.blitImage(8 + selectedOfs, 20, 2, 18, ox + pp, oy, i);
        igd.blitImage(10 + selectedOfs, 20, 2, 18, ox + (wid - 2), oy, i);

        drawString(igd, ox + 2, oy + 2, string, true, 16);
        return wid;
    }

    public static int drawLabel(IGrDriver igd, int wid, int ox, int oy, String string, boolean selected, int height) {
        if (height == 16)
            return drawLabelx2(igd, wid, ox, oy, string, selected);
        if (height == 8)
            return drawLabelx1(igd, wid, ox, oy, string, selected);
        // switch from bitmaps to something else
        Rect res = getRecommendedSize(string, height);
        if (selected) {
            igd.clearRect(255, 255, 255, ox, oy, wid, res.height);
        } else {
            igd.clearRect(64, 64, 64, ox, oy, wid, res.height);
        }
        int margin = height / 8;
        if (margin == 0)
            margin = 1;
        igd.clearRect(32, 32, 32, ox + margin, oy + margin, wid - (margin * 2), res.height - (margin * 2));
        UILabel.drawString(igd, ox + margin, oy + margin, string, true, height);
        return wid;
    }
}
