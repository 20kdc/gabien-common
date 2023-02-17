/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.*;
import gabien.ui.theming.EightPatch;

/**
 * Responsible for borders, and the drawing thereof.
 * Created on February 16th, 2018.
 */
public abstract class UIBorderedElement extends UIElement {
    public static int borderTheme = 0;
    public static final int BORDER_THEMES = 4;
    public static final int BORDER_TYPES = 14;

    private static IImage cachedTheme = null;
    private static int[] cachedThemeInts;

    private static int lastCachedThemeTiles = -1;
    private static IImage[] cachedThemeTiles;

    private static EightPatch[] borderW1 = new EightPatch[BORDER_TYPES * BORDER_THEMES];
    private static EightPatch[] borderW2 = new EightPatch[BORDER_TYPES * BORDER_THEMES];
    private static EightPatch[] borderW3 = new EightPatch[BORDER_TYPES * BORDER_THEMES];
    private static EightPatch[] borderW4 = new EightPatch[BORDER_TYPES * BORDER_THEMES];

    public int borderType;
    private int borderWidth;

    private Rect contentsRelativeInputBounds;

    public UIBorderedElement(int bt, int bw) {
        borderType = bt;
        borderWidth = bw;
        calcContentsRelativeInputBounds();
    }

    public UIBorderedElement(int bt, int bw, int w, int h) {
        super(w + (bw * 2), h + (bw * 2));
        borderType = bt;
        borderWidth = bw;
        calcContentsRelativeInputBounds();
    }

    /**
     * Internal use only please
     */
    public static void setupAssets() {
        cachedTheme = GaBIEn.getImageEx("themes.png", false, true);
        cachedThemeInts = cachedTheme.getPixels();
        lastCachedThemeTiles = -1;
        int index = 0;
        for (int i = 0; i < BORDER_TYPES; i++) {
            for (int j = 0; j < BORDER_THEMES; j++) {
                int baseX = i * 12;
                int baseY = j * 18;
                Rect outerRegion, innerRegion;

                outerRegion = new Rect(baseX, baseY, 3, 3);
                innerRegion = new Rect(1, 1, 1, 1);
                borderW1[index] = new EightPatch(cachedTheme, outerRegion, innerRegion);

                outerRegion = new Rect(baseX + 6, baseY, 6, 6);
                innerRegion = new Rect(2, 2, 2, 2);
                borderW2[index] = new EightPatch(cachedTheme, outerRegion, innerRegion);

                outerRegion = new Rect(baseX + 6, baseY, 6, 6);
                innerRegion = new Rect(3, 3, 0, 0);
                borderW3[index] = new EightPatch(cachedTheme, outerRegion, innerRegion);

                outerRegion = new Rect(baseX, baseY + 6, 12, 12);
                innerRegion = new Rect(4, 4, 4, 4);
                borderW4[index] = new EightPatch(cachedTheme, outerRegion, innerRegion);
                index++;
            }
        }
    }

    public static int getRecommendedBorderWidth(int textHeight) {
        return Math.max(1, textHeight / 8);
    }
    // Used for various texty things.
    public static Size getRecommendedTextSize(String text, int textHeight) {
        return getRecommendedTextSize(text, textHeight, getRecommendedBorderWidth(textHeight));
    }

    public static Size getRecommendedTextSize(String text, int textHeight, int bs) {
        Size s = FontManager.getTextSize(text, textHeight);
        return new Size(s.width + (bs * 2), s.height + (bs * 2));
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    private void calcContentsRelativeInputBounds() {
        int bw = borderWidth;
        int bwy = bw;
        if (getBorderFlag(borderType, 0))
            bwy *= 2;
        Size sz = getSize();
        contentsRelativeInputBounds = new Rect(-bw, -bwy, sz.width, sz.height);
    }

    protected Rect getContentsRelativeInputBounds() {
        return contentsRelativeInputBounds;
    }

    @Override
    public void runLayout() {
        super.runLayout();
        calcContentsRelativeInputBounds();
    }

    @Override
    public final void render(IGrDriver igd) {
        Size s = getSize();
        boolean black = getBorderFlag(borderType, 5);
        if (getBorderFlag(borderType, 0)) {
            int[] localST = igd.getLocalST();
            int oldTY = localST[1];
            localST[1] += getBorderWidth();
            igd.updateST();
            drawBorder(igd, borderType, borderWidth, 0, 0, s.width, s.height);
            renderContents(black, igd);
            localST[1] = oldTY;
            igd.updateST();
        } else {
            drawBorder(igd, borderType, borderWidth, 0, 0, s.width, s.height);
            renderContents(black, igd);
        }
    }

    @Override
    public final void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        peripherals.performOffset(contentsRelativeInputBounds.x, contentsRelativeInputBounds.y);
        updateContents(deltaTime, selected, peripherals);
        peripherals.performOffset(-contentsRelativeInputBounds.x, -contentsRelativeInputBounds.y);
    }

    // This is the one you override.
    public abstract void renderContents(boolean drawBlack, IGrDriver igd);
    public abstract void updateContents(double deltaTime, boolean selected, IPeripherals peripherals);

    public static boolean getMoveDownFlag(int base) {
        return getBorderFlag(base, 0);
    }

    public static boolean getClearFlag(int base) {
        return getBorderFlag(base, 1);
    }

    public static boolean getTiledFlag(int base) {
        return getBorderFlag(base, 4);
    }

    public static boolean getBlackTextFlagWindowRoot() {
        return getBorderFlag(5, 5);
    }

    public static boolean getBlackTextFlag(int i) {
        return getBorderFlag(i, 5);
    }

    // flag 0: use 'pressed' offset effect (WHERE SUPPORTED)
    // flag 1: Contents are black, use a clear for speed. (Ignored if tiling!)
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

    public static void drawBorder(IGrDriver igd, int borderType, int borderWidth, Rect where) {
        drawBorder(igd, borderType, borderWidth, where.x, where.y, where.width, where.height);
    }
    public static void drawBorder(IGrDriver igd, int borderType, int borderWidth, int x, int y, int w, int h) {
        // This variable dates back to when this class did its own rendering.
        // This has been preserved.
        int chunkSize;
        if (getTiledFlag(borderType)) {
            // Bite the bullet - user *wants* tiling
            if (lastCachedThemeTiles != borderTheme)
                cachedThemeTiles = null;
            if (cachedThemeTiles == null)
                cachedThemeTiles = new IImage[BORDER_TYPES];
            if (cachedThemeTiles[borderType] == null) {
                // Extract tile
                IGrDriver osb = GaBIEn.makeOffscreenBuffer(12, 12, true);
                osb.blitImage(borderType * 12, (borderTheme * 18) + 6, 12, 12, 0, 0, cachedTheme);
                cachedThemeTiles[borderType] = GaBIEn.createImage(osb.getPixels(), 12, 12);
                osb.shutdown();
            }
            igd.blitTiledImage(x, y, w, h, cachedThemeTiles[borderType]);
            // Entire highres border space is reserved for tiling pattern.
            // Try to make the most of lowres? :(
            chunkSize = 3;
            if (borderWidth != 0)
                borderWidth = ensureBWV(Math.max(borderWidth, 3), chunkSize);
        } else {
            int eBorderWidth = borderWidth;
            if (borderWidth == 0)
                eBorderWidth = Math.min(w, h);
            int baseX = borderType * 12;
            int baseY = borderTheme * 18;
            chunkSize = 1;
            if (eBorderWidth >= 2) {
                baseX += 6;
                chunkSize = 2;
            }
            if (eBorderWidth >= 4) {
                baseX -= 6;
                baseY += 6;
                chunkSize = 4;
            }

            borderWidth = ensureBWV(borderWidth, chunkSize);

            if (getClearFlag(borderType)) {
                igd.clearRect(0, 0, 0, x + borderWidth, y + borderWidth, w - (borderWidth * 2), h - (borderWidth * 2));
            } else {
                igd.blitScaledImage(baseX + chunkSize, baseY + chunkSize, chunkSize, chunkSize, x + borderWidth, y + borderWidth, w - (borderWidth * 2), h - (borderWidth * 2), cachedTheme);
            }
        }

        if (borderWidth <= 0)
            return;
        EightPatch borderAsset;
        if (chunkSize == 1) {
            borderAsset = borderW1[(borderType * BORDER_THEMES) + borderTheme];
        } else if (chunkSize == 2) {
            borderAsset = borderW2[(borderType * BORDER_THEMES) + borderTheme];
        } else if (chunkSize == 3) {
            borderAsset = borderW3[(borderType * BORDER_THEMES) + borderTheme];
        } else {
            borderAsset = borderW4[(borderType * BORDER_THEMES) + borderTheme];
        }
        borderAsset.draw(igd, borderWidth, borderWidth, borderWidth, borderWidth, x, y, w, h);
    }

    private static int ensureBWV(int borderWidth, int chunk) {
        if (borderWidth > chunk)
            return ((borderWidth + 2) / chunk) * chunk;
        return borderWidth;
    }
}
