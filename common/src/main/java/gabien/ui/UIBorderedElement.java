/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.*;
import gabien.ui.theming.Theme;
import gabien.ui.theming.ThemingCentral;

/**
 * Responsible for borders, and the drawing thereof.
 * Created on February 16th, 2018.
 */
public abstract class UIBorderedElement extends UIElement {
    public static final int BORDER_TYPES = 14;

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
        Theme theme = getTheme();
        if (getBorderFlag2(theme, borderType, ThemingCentral.BF_MOVEDOWN))
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
    public final void renderLayer(IGrDriver igd, UILayer layer) {
        if (layer != UILayer.Base && layer != UILayer.Content)
            return;
        Size s = getSize();
        Theme theme = getTheme();
        boolean black = getBorderFlag2(theme, borderType, ThemingCentral.BF_LIGHTBKG);
        if (getBorderFlag2(theme, borderType, ThemingCentral.BF_MOVEDOWN)) {
            float oty = igd.trsTYS(getBorderWidth());
            if (layer == UILayer.Base)
                drawBorder(theme, igd, borderType, borderWidth, 0, 0, s.width, s.height);
            else if (layer == UILayer.Content)
                renderContents(black, igd);
            igd.trsTYE(oty);
        } else {
            if (layer == UILayer.Base)
                drawBorder(theme, igd, borderType, borderWidth, 0, 0, s.width, s.height);
            else if (layer == UILayer.Content)
                renderContents(black, igd);
        }
    }

    @Override
    protected final void render(IGrDriver igd) {
        // Disabled to stop shenanigans
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

    public static boolean getBlackTextFlagWindowRoot(Theme theme) {
        return getBlackTextFlag(theme, 5);
    }

    public static boolean getMoveDownFlag(Theme theme, int base) {
        return getBorderFlag2(theme, base, ThemingCentral.BF_MOVEDOWN);
    }

    public static boolean getBlackTextFlag(Theme theme, int i) {
        return getBorderFlag2(theme, i, ThemingCentral.BF_LIGHTBKG);
    }

    private static boolean getBorderFlag2(Theme theme, int borderType, int flag) {
        return theme.getBorder(borderType).getFlag(flag);
    }

    public static void drawBorder(Theme theme, IGrDriver igd, int borderType, int borderWidth, Rect where) {
        drawBorder(theme, igd, borderType, borderWidth, where.x, where.y, where.width, where.height);
    }
    public static void drawBorder(Theme theme, IGrDriver igd, int borderType, int borderWidth, int x, int y, int w, int h) {
        theme.getBorder(borderType).draw(igd, borderWidth, x, y, w, h);
    }
}
