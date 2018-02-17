/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.*;

/**
 * Responsible for borders, and the drawing thereof.
 * Created on February 16th, 2018.
 */
public abstract class UIBorderedElement extends UIElement {

    protected int borderType;
    private int borderWidth;

    public UIBorderedElement(int bt, int bw) {
        borderType = bt;
        borderWidth = bw;
    }

    public UIBorderedElement(int bt, int bw, int w, int h) {
        super(w + (bw * 2), h + (bw * 2));
        borderType = bt;
        borderWidth = bw;
    }

    public static int getRecommendedBorderWidth(int textHeight) {
        return Math.max(1, textHeight / 8);
    }
    // Used for various texty things.
    public static Size getRecommendedTextSize(String text, int textHeight) {
        int bs = textHeight / 8;
        Size s = FontManager.getTextSize(text, textHeight);
        return new Size(s.width + (bs * 2), s.height + (bs * 2));
    }


    // NOTE: Obviously, this may require you change your layout.
    //       ... that's *your* problem.
    protected void setBorderWidth(int w) {
        borderWidth = w;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    @Override
    public void render(boolean selected, IPointer mouse, IGrInDriver igd) {
        Size s = getSize();
        drawBorder(igd, borderType, borderWidth, s.width, s.height);
    }

    public static void drawBorder(IGrDriver igd, int borderType, int borderWidth, int w, int h) {
        IImage im = GaBIEn.getImageCKEx("theme.png", false, true, 255, 0, 255);
        int baseX = borderType * 12;
        int baseY = 0;
        int chunkSize = 1;
        if (borderWidth >= 2) {
            baseX += 6;
            chunkSize = 2;
        }
        if (borderWidth >= 4) {
            baseX -= 6;
            baseY += 6;
            chunkSize = 4;
        }

        igd.blitScaledImage(baseX + chunkSize, baseY + chunkSize, chunkSize, chunkSize, borderWidth, borderWidth, w - (borderWidth * 2), h - (borderWidth * 2), im);

        // edges

        igd.blitScaledImage(baseX + chunkSize, baseY, chunkSize, chunkSize, borderWidth, 0, w - (borderWidth * 2), borderWidth, im);
        igd.blitScaledImage(baseX + chunkSize, baseY + (chunkSize * 2), chunkSize, chunkSize, borderWidth, h - borderWidth, w - (borderWidth * 2), borderWidth, im);

        igd.blitScaledImage(baseX, baseY + chunkSize, chunkSize, chunkSize, 0, borderWidth, borderWidth, h - (borderWidth * 2), im);
        igd.blitScaledImage(baseX + (chunkSize * 2), baseY + chunkSize, chunkSize, chunkSize, w - borderWidth, borderWidth, borderWidth, h - (borderWidth * 2), im);

        // corners

        igd.blitScaledImage(baseX, baseY, chunkSize, chunkSize, 0, 0, borderWidth, borderWidth, im);
        igd.blitScaledImage(baseX + (chunkSize * 2), baseY, chunkSize, chunkSize, w - borderWidth, 0, borderWidth, borderWidth, im);

        igd.blitScaledImage(baseX, baseY + (chunkSize * 2), chunkSize, chunkSize, 0, h - borderWidth, borderWidth, borderWidth, im);
        igd.blitScaledImage(baseX + (chunkSize * 2), baseY + (chunkSize * 2), chunkSize, chunkSize, w - borderWidth, h - borderWidth, borderWidth, borderWidth, im);
    }
}
