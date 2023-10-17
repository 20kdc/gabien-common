/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.render.IGrDriver;
import gabien.ui.theming.*;
import gabien.wsi.IPeripherals;

/**
 * Responsible for borders, and the drawing thereof.
 * Created on February 16th, 2018.
 */
public abstract class UIBorderedElement extends UIElement {
    public static final int BORDER_TYPES = 14;

    private Theme.Attr<IBorder> borderType;
    private int borderWidth;

    private Rect contentsRelativeInputBounds;
    private IBorder border;

    public UIBorderedElement(Theme.Attr<IBorder> bt, int bw) {
        borderType = bt;
        borderWidth = bw;
        updateBorder();
    }

    public UIBorderedElement(Theme.Attr<IBorder> bt, int bw, int w, int h) {
        super(w + (bw * 2), h + (bw * 2));
        borderType = bt;
        borderWidth = bw;
        updateBorder();
    }

    public static int getRecommendedBorderWidth(int textHeight) {
        return Math.max(1, textHeight / 8);
    }
    // Used for various texty things.
    public static Size getRecommendedTextSize(Theme theme, String text, int textHeight) {
        return getRecommendedTextSize(theme, text, textHeight, getRecommendedBorderWidth(textHeight));
    }

    public static Size getRecommendedTextSize(Theme theme, String text, int textHeight, int bs) {
        FontManager fm = Theme.FM_GLOBAL.get(theme);
        Size s = fm.getTextSize(text, textHeight);
        return new Size(s.width + (bs * 2), s.height + (bs * 2));
    }

    public static int getBorderedTextHeight(Theme theme, int textHeight) {
        return getBorderedTextHeight(theme, textHeight, getRecommendedBorderWidth(textHeight));
    }

    public static int getBorderedTextHeight(Theme theme, int textHeight, int bs) {
        FontManager fm = Theme.FM_GLOBAL.get(theme);
        return fm.getFontSizeGeneralContentHeight(textHeight) + (bs * 2);
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderType(Theme.Attr<IBorder> bt) {
        borderType = bt;
        updateBorder();
    }

    @Override
    public void onThemeChanged() {
        // System.out.println("theme updated @ " + this + " -> " + this.getTheme());
        updateBorder();
    }

    private void updateBorder() {
        border = borderType.get(this);
        updateContentsRelativeInputBounds();
    }
    private void updateContentsRelativeInputBounds() {
        int bw = borderWidth;
        int bwy = bw;
        if (border.getFlag(ThemingCentral.BF_MOVEDOWN))
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
        updateContentsRelativeInputBounds();
    }

    @Override
    public final void renderLayer(IGrDriver igd, UILayer layer) {
        if (layer != UILayer.Base && layer != UILayer.Content)
            return;
        Size s = getSize();
        boolean black = border.getFlag(ThemingCentral.BF_LIGHTBKG);
        if (border.getFlag(ThemingCentral.BF_MOVEDOWN)) {
            float oty = igd.trsTYS(getBorderWidth());
            if (layer == UILayer.Base)
                border.draw(igd, borderWidth, 0, 0, s.width, s.height);
            else if (layer == UILayer.Content)
                renderContents(black, igd);
            igd.trsTYE(oty);
        } else {
            if (layer == UILayer.Base)
                border.draw(igd, borderWidth, 0, 0, s.width, s.height);
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
        return getBlackTextFlag(theme, Theme.B_WINDOW);
    }

    public static boolean getMoveDownFlag(Theme theme, Theme.Attr<IBorder> borderType) {
        return getBorderFlag2(theme, borderType, ThemingCentral.BF_MOVEDOWN);
    }

    public static boolean getBlackTextFlag(Theme theme, Theme.Attr<IBorder> borderType) {
        return getBorderFlag2(theme, borderType, ThemingCentral.BF_LIGHTBKG);
    }

    private static boolean getBorderFlag2(Theme theme, Theme.Attr<IBorder> borderType, int flag) {
        return borderType.get(theme).getFlag(flag);
    }

    public static void drawBorder(Theme theme, IGrDriver igd, Theme.Attr<IBorder> borderType, int borderWidth, Rect where) {
        drawBorder(theme, igd, borderType, borderWidth, where.x, where.y, where.width, where.height);
    }
    public static void drawBorder(Theme theme, IGrDriver igd, Theme.Attr<IBorder> borderType, int borderWidth, int x, int y, int w, int h) {
        borderType.get(theme).draw(igd, borderWidth, x, y, w, h);
    }
}
