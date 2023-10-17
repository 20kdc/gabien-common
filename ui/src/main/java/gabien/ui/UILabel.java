/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.GaBIEnUI;
import gabien.render.IGrDriver;
import gabien.text.TextTools;
import gabien.ui.theming.IBorder;
import gabien.ui.theming.Theme;
import gabien.wsi.IPeripherals;

/**
 * A label. Displays text.
 * Rewritten on February 16th, 2018 to be a UIBorderedElement and the base of UITextBox.
 */
public class UILabel extends UIBorderedElement {
    public String text;
    protected final Contents contents;
    public int alignX, alignY;

    // Creates a label, with text, and sets the bounds accordingly.
    public UILabel(String txt, int h) {
        this(txt, h, "");
    }

    public UILabel(String txt, int h, String spacer) {
        super(Theme.B_LABEL, getRecommendedBorderWidth(h));
        contents = new Contents(h, spacer);
        text = txt;

        // Using the sysThemeRoot here is cheating, but the alternative is summoning bugs that won't be found until too late.
        setForcedBounds(null, new Rect(getRecommendedTextSize(GaBIEnUI.sysThemeRoot.getTheme(), text, h)));
        runLayout();
        setForcedBounds(null, new Rect(getWantedSize()));
    }

    public UILabel centred() {
        alignX = 1;
        alignY = 1;
        return this;
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        runLayout();
    }

    @Override
    public void onThemeChanged() {
        super.onThemeChanged();
        // important because the label render can change based on theme now
        runLayout();
    }

    // This just gets spammed every frame, in order to update text at every possible time.
    // It's not perfect, but contents.update checks enough so everything's :ok_hand:
    @Override
    public void runLayout() {
        super.runLayout();
        Size p = contents.update(getTheme(), getSize(), getBorderWidth(), text);
        if (p != null)
            setWantedSize(p);
    }

    @Override
    public void renderContents(boolean textBlack, IGrDriver igd) {
        contents.render(textBlack, 0, 0, igd, alignX, alignY);
    }

    /**
     * This class, created just before midnight (heading into 17th February 2018),
     *  is for common stuff between - midnight - UILabel and UITextButton.
     */
    public static class Contents {
        private String lastText = "", textFormatted = "";
        private Size lastSize = new Size(0, 0);
        private Size lastActSize = new Size(0, 0);
        private Size lastSpacerSize = null;
        private FontManager lastFM = null;
        private int lastBw = 1;
        private TextTools.PlainCached paragraph = new TextTools.PlainCached();

        public final int textHeight;
        public final String spacerText;

        public Contents(int th) {
            this(th, "");
        }

        public Contents(int th, String st) {
            textHeight = th;
            spacerText = st;
        }

        public Size update(Theme theme, Size sz, int bw, String text) {
            // run formatting...
            Size sz2 = null;
            FontManager fm = Theme.FM_GLOBAL.get(theme);
            boolean overrideChanged = lastFM != fm;
            if ((lastSpacerSize == null) || (!lastText.equals(text)) || (lastBw != bw) || (!lastSize.sizeEquals(sz)) || overrideChanged) {
                lastText = text;
                lastSize = sz;
                lastBw = bw;
                lastFM = fm;
                textFormatted = fm.formatTextFor(text, textHeight, sz.width - (bw * 2));
                // You may be wondering why this is set up the way it is.
                // The answer is simply that B's height is what we need to be given the width,
                //  and A is what we want to be, width and height alike.
                Size a = getRecommendedTextSize(theme, text, textHeight, bw);
                Size b = lastActSize = getRecommendedTextSize(theme, textFormatted, textHeight, bw);
                lastSpacerSize = getRecommendedTextSize(theme, spacerText, textHeight, bw);
                sz2 = new Size(a.width, b.height);
                sz2 = sz2.sizeMax(lastSpacerSize);
                paragraph.font = fm.getFontForText(textFormatted, textHeight);
                paragraph.text = textFormatted;
            }
            return sz2;
        }

        public void render(boolean blackText, int x, int y, IGrDriver igd, boolean centre) {
            render(blackText, x, y, igd, centre ? 1 : 0, centre ? 1 : 0);
        }

        public void render(boolean blackText, int x, int y, IGrDriver igd, int alignX, int alignY) {
            x += ((lastSize.width - lastActSize.width) * alignX) / 2;
            y += ((lastSize.height - lastActSize.height) * alignY) / 2;
            x += lastBw;
            y += lastBw;
            paragraph.blackText = blackText;
            paragraph.update();
            paragraph.getChunk().renderRoot(igd, x, y);
        }
    }

    // Sort of a "lite" UILabel.
    public static class StatusLine extends LAFChain {
        private Contents statusLine;
        // used to prevent allocating Size objects
        private Size lastSize = new Size(0, 0);
        private int height = 0;
        public void draw(String text, int textHeight, IGrDriver igd, int x, int y, int w) {
            // Status line stuff
            if (statusLine == null) {
                statusLine = new UILabel.Contents(textHeight);
            } else  if (statusLine.textHeight != textHeight) {
                statusLine = new UILabel.Contents(textHeight);
            }
            int bw = UIBorderedElement.getRecommendedBorderWidth(textHeight);
            pokeLastSize(w, height);
            Size sz = statusLine.update(getTheme(), lastSize, bw, text);
            if (sz != null) {
                height = sz.height;
                pokeLastSize(w, height);
            }
            Theme theme = getTheme();
            UIBorderedElement.drawBorder(theme, igd, Theme.B_LABEL, bw, x, y, w, height);
            boolean statusLineBT = UIBorderedElement.getBlackTextFlag(theme, Theme.B_LABEL);
            statusLine.render(statusLineBT, x, y, igd, false);
        }

        private void pokeLastSize(int w, int h) {
            if ((lastSize.width != w) || (lastSize.height != h))
                lastSize = new Size(w, h);
        }
    }

    public static int drawLabel(Theme theme, IGrDriver igd, int wid, int ox, int oy, String string, Theme.Attr<IBorder> mode, int height, TextTools.PlainCached cache) {
        return drawLabel(theme, igd, wid, ox, oy, string, mode, height, cache, true, true);
    }

    // NOTE: Assumes the label is already formatted accordingly.
    // If not, expect it to go off the right of the screen if need be.
    // If you want multiline support, use a Contents instance.
    public static int drawLabel(Theme theme, IGrDriver igd, int wid, int ox, int oy, String string, Theme.Attr<IBorder> mode, int height, TextTools.PlainCached cache, boolean enBack, boolean enFore) {
        int h = UIBorderedElement.getRecommendedBorderWidth(height);
        int h2 = height + (h * 2) - (height / 8);
        if (enBack)
            UIBorderedElement.drawBorder(theme, igd, mode, h, ox, oy, wid, h2);
        if (enFore) {
            FontManager fm = Theme.FM_GLOBAL.get(theme);
            cache.font = fm.getFontForText(string, height);
            cache.blackText = UIBorderedElement.getBlackTextFlag(theme, mode);
            cache.text = string;
            cache.update();
            cache.getChunk().renderRoot(igd, ox + h, oy + h);
        }
        return wid;
    }
}
