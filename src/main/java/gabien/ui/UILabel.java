/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.GaBIEn;
import gabien.IGrInDriver;
import gabien.IGrInDriver.IImage;

public class UILabel extends UIPanel {
    public static boolean iDislikeTheFont = false;
    public static boolean iAmAbsolutelySureIHateTheFont = false;

    public String Text = "No notice text.";
    public boolean x2;

    // Creates a label,with text,and sets the bounds accordingly.
    public UILabel(String text, boolean x2) {
        this.x2 = x2;
        Text = text;
        setRecommendedSize();
    }

    public void setRecommendedSize() {
        setBounds(new Rect(0, 0, ((x2 ? 16 : 8) * Text.length()) + (x2 ? 4 : 2), x2 ? 18 : 9));
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean selected, IGrInDriver igd) {
        super.updateAndRender(ox, oy, DeltaTime, selected, igd);
        if (x2) {
            drawLabelx2(igd, elementBounds.width, ox, oy, Text, false);
        } else {
            drawLabel(igd, elementBounds.width, ox, oy, Text, false);
        }
    }

    public static void drawString(IGrInDriver igd, int xptr, int oy, String text, boolean bck, boolean x2) {
        IImage font = GaBIEn.getImage(x2 ? "font2x.png" : "font.png", 0, 0, 0);
        int l = text.length();
        if (iDislikeTheFont) {
            if (x2) {
                igd.drawText(xptr, oy, 255, 255, 255, 16, text);
                return;
            } else if (iAmAbsolutelySureIHateTheFont) {
                igd.drawText(xptr, oy, 255, 255, 255, 8, text);
                return;
            }
        }
        for (int p = 0; p < l; p++) {
            if (text.charAt(p) >= 128) {
                igd.drawText(xptr, oy, 255, 255, 255, x2?16:8, text);
                return;
            }
        }
        byte[] ascii = text.getBytes();
        for (int p = 0; p < ascii.length; p++) {
            UILabel.drawChar(igd, ascii[p], font, xptr, oy, bck, x2);
            xptr += x2 ? 16 : 8;
        }
    }

    public static void drawChar(IGrInDriver igd, int cc, IImage font, int xptr, int yptr, boolean bck, boolean x2) {
        int chSize = x2 ? 14 : 7;
        if (cc < 256) {
            if (bck) {
                igd.blitBCKImage((cc & 0x0F) * chSize, ((cc & 0xF0) >> 4) * chSize, chSize, chSize, xptr, yptr, font);
            } else {
                igd.blitImage((cc & 0x0F) * chSize, ((cc & 0xF0) >> 4) * chSize, chSize, chSize, xptr, yptr, font);
            }
        } else {
            if (bck) {
                igd.blitBCKImage(0, 0, chSize, chSize, xptr, yptr, font);
            } else {
                igd.blitImage(0, 0, chSize, chSize, xptr, yptr, font);
            }
        }
    }

    public static int drawLabel(IGrInDriver igd, int wid, int ox, int oy, String string, boolean selected) {
        IImage i = GaBIEn.getImage("textButton.png", 255, 0, 255);
        if (wid == 0)
            wid = (string.length() * 8) + 2;
        igd.blitImage(selected ? 3 : 0, 10, 1, 9, ox, oy, i);
        for (int pp = 1; pp < wid - 1; pp++)
            igd.blitImage(selected ? 4 : 1, 10, 1, 9, ox + pp, oy, i);
        igd.blitImage(selected ? 5 : 2, 10, 1, 9, ox + (wid - 1), oy, i);

        drawString(igd, ox + 1, oy + 1, string, true, false);
        return wid;
    }

    public static void drawLabelx2(IGrInDriver igd, int wid, int ox, int oy, String string, boolean selected) {
        IImage i = GaBIEn.getImage("textButton.png", 255, 0, 255);
        if (wid == 0)
            wid = (string.length() * 16) + 4;
        int selectedOfs = selected ? 6 : 0;
        igd.blitImage(6 + selectedOfs, 20, 2, 18, ox, oy, i);
        for (int pp = 2; pp < wid - 1; pp += 2)
            igd.blitImage(8 + selectedOfs, 20, 2, 18, ox + pp, oy, i);
        igd.blitImage(10 + selectedOfs, 20, 2, 18, ox + (wid - 2), oy, i);

        drawString(igd, ox + 2, oy + 2, string, true, true);
    }
}
