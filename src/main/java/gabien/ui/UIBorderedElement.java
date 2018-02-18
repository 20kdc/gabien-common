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
    public static int borderTheme = 0;
    public static final int BORDER_THEMES = 4;
    private static IImage cachedTheme = null;
    private static int[] cachedThemeInts;

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
    public final void render(boolean selected, IPeripherals peripherals, IGrDriver igd) {
        Size s = getSize();
        boolean black = getBorderFlag(borderType, 5);
        if (getBorderFlag(borderType, 0)) {
            int[] localST = igd.getLocalST();
            int oldTY = localST[1];
            localST[1] += getBorderWidth();
            igd.updateST();
            drawBorder(igd, borderType, borderWidth, 0, 0, s.width, s.height);
            renderContents(selected, black, peripherals, igd);
            localST[1] = oldTY;
            igd.updateST();
        } else {
            drawBorder(igd, borderType, borderWidth, 0, 0, s.width, s.height);
            renderContents(selected, black, peripherals, igd);
        }
    }

    // This is the one you override.
    public abstract void renderContents(boolean selected, boolean drawBlack, IPeripherals peripherals, IGrDriver igd);

    public static boolean getBlackTextFlag(int i) {
        return getBorderFlag(i, 5);
    }

    // flag 0: use 'pressed' offset effect
    // flag 4: hi-res section is tiled, mid-res becomes 3-pixel border w/ added weirdness
    // flag 5: text, etc. should be black
    private static boolean getBorderFlag(int borderType, int flag) {
        if (cachedTheme == null)
            return false;
        int x = flag % 3;
        int y = flag / 3;
        int idx = (borderType * 12) + 3 + x + (cachedTheme.getWidth() * (3 + y + (borderTheme * 18)));
        if (idx < 0)
            return false;
        if (idx >= cachedThemeInts.length)
            return false;
        return cachedThemeInts[idx] == -1;
    }

    public static void drawBorder(IGrDriver igd, int borderType, int borderWidth, int x, int y, int w, int h) {
        if (cachedTheme == null) {
            cachedTheme = GaBIEn.getImageCKEx("themes.png", false, true, 255, 0, 255);
            cachedThemeInts = cachedTheme.getPixels();
        }
        int baseX;
        int baseY;
        int chunkSize, chunkSizeO;
        if (getBorderFlag(borderType, 4)) {
            // Bite the bullet - user *wants* tiling
            for (int i = 0; i < w; i += 12)
                for (int j = 0; j < h; j += 12)
                    igd.blitImage(borderType * 12, (borderTheme * 18) + 6, Math.min(12, w - i), Math.min(12, h - j), x + i, y + j, cachedTheme);
            // Entire highres border space is reserved for tiling pattern.
            // Try to make the most of lowres? :(
            borderWidth = Math.min(3, borderWidth);
            chunkSize = 3;
            chunkSizeO = 0;
            baseX = (borderType * 12) + 6;
            baseY = borderTheme * 18;
        } else {
            baseX = borderType * 12;
            baseY = borderTheme * 18;
            chunkSize = 1;
            if (borderWidth >= 2) {
                baseX += 6;
                chunkSize = 2;
            }
            if (borderWidth >= 4) {
                baseX -= 6;
                baseY += 6;
                chunkSize = 4;
            }

            chunkSizeO = chunkSize;
            igd.blitScaledImage(baseX + chunkSize, baseY + chunkSize, chunkSizeO, chunkSizeO, x + borderWidth, y + borderWidth, w - (borderWidth * 2), h - (borderWidth * 2), cachedTheme);
        }

        if (borderWidth <= 0)
            return;

        drawBorderCore(igd, baseX, baseY, chunkSize, chunkSizeO, borderWidth, x, y, w, h);
    }
    private static void drawBorderCore(IGrDriver igd, int x0, int y0, int chunkSizeLR, int chunkSizeM, int borderWidth, int x, int y, int w, int h) {
        IImage im = cachedTheme;

        int x1 = x0 + chunkSizeLR;
        int x2 = x1 + chunkSizeM;
        int y1 = y0 + chunkSizeLR;
        int y2 = y1 + chunkSizeM;

        // Positions calculated, now adjust for M=0
        if (chunkSizeM == 0) {
            x1--;
            y1--;
            chunkSizeM = 2;
        }

        // edges

        igd.blitScaledImage(x1, y0, chunkSizeM, chunkSizeLR, x + borderWidth, y, w - (borderWidth * 2), borderWidth, im);
        igd.blitScaledImage(x1, y2, chunkSizeM, chunkSizeLR, x + borderWidth, y + (h - borderWidth), w - (borderWidth * 2), borderWidth, im);

        igd.blitScaledImage(x0, y1, chunkSizeLR, chunkSizeM, x, y + borderWidth, borderWidth, h - (borderWidth * 2), im);
        igd.blitScaledImage(x2, y1, chunkSizeLR, chunkSizeM, x + (w - borderWidth), y + borderWidth, borderWidth, h - (borderWidth * 2), im);

        // corners

        igd.blitScaledImage(x0, y0, chunkSizeLR, chunkSizeLR, x, y, borderWidth, borderWidth, im);
        igd.blitScaledImage(x2, y0, chunkSizeLR, chunkSizeLR, x + (w - borderWidth), y, borderWidth, borderWidth, im);

        igd.blitScaledImage(x0, y2, chunkSizeLR, chunkSizeLR, x, y + (h - borderWidth), borderWidth, borderWidth, im);
        igd.blitScaledImage(x2, y2, chunkSizeLR, chunkSizeLR, x + (w - borderWidth), y + (h - borderWidth), borderWidth, borderWidth, im);
    }
}
