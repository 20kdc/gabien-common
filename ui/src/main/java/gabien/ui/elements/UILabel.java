/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.elements;

import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEnUI;
import gabien.render.IGrDriver;
import gabien.text.TextTools;
import gabien.ui.FontManager;
import gabien.ui.LAFChain;
import gabien.ui.theming.IBorder;
import gabien.ui.theming.Theme;
import gabien.uslx.append.Size;
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

        labelDoUpdate();
        forceToRecommended();
    }

    public UILabel centred() {
        alignX = 1;
        alignY = 1;
        return this;
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        // Spamming this is fine because if nothing actually changes it becomes a no-op
        labelDoUpdate();
    }

    @Override
    public void onThemeChanged() {
        super.onThemeChanged();
        layoutRecalculateMetrics();
    }

    @Override
    public int layoutGetHForW(int width) {
        return contents.getHForW(getTheme(), getBorderWidth(), text, width);
    }

    @Override
    protected @Nullable Size layoutRecalculateMetricsImpl() {
        return null;
    }

    @Override
    public void renderContents(boolean textBlack, IGrDriver igd) {
        contents.render(textBlack, 0, 0, igd, alignX, alignY);
    }

    public void setText(String didThing) {
        text = didThing;
        labelDoUpdate();
    }

    // Allows for overrides and such.
    public void labelDoUpdate() {
        Size sz = contents.update(getTheme(), getSize(), getBorderWidth(), text);
        if (sz != null)
            setWantedSize(sz);
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

        public int getHForW(Theme theme, int bw, String text, int width) {
            FontManager fm = Theme.FM_GLOBAL.get(theme);
            String formattedText = fm.formatTextFor(text, textHeight, width - (bw * 2));
            return getRecommendedTextSize(theme, formattedText, textHeight, bw).height;
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
                lastActSize = getRecommendedTextSize(theme, textFormatted, textHeight, bw);
                lastSpacerSize = getRecommendedTextSize(theme, spacerText, textHeight, bw);
                sz2 = a;
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
    public static class StatusLine {
        public LAFChain themeSource = GaBIEnUI.sysThemeRoot;

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
            Theme theme = themeSource.getTheme();
            Size sz = statusLine.update(theme, lastSize, bw, text);
            if (sz != null) {
                height = sz.height;
                pokeLastSize(w, height);
            }
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
