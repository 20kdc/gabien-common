/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.FontManager;
import gabien.IGrDriver;
import gabien.IPeripherals;

/**
 * A label. Displays text.
 * Rewritten on February 16th, 2018 to be a UIBorderedElement and the base of UITextBox.
 */
public class UILabel extends UIBorderedElement {
    public String text;
    private final Contents contents;

    // Creates a label,with text,and sets the bounds accordingly.
    public UILabel(String txt, int h) {
        super(2, getRecommendedBorderWidth(h));
        contents = new Contents(h);
        text = txt;

        setWantedSize(getRecommendedTextSize("", h));
        runLayout();
        setForcedBounds(null, new Rect(getWantedSize()));
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        runLayout();
    }

    // This just gets spammed every frame, in order to update text at every possible time.
    // It's not perfect, but contents.update checks enough so everything's :ok_hand:
    @Override
    public void runLayout() {
        super.runLayout();
        Size p = contents.update(getSize(), getBorderWidth(), text);
        if (p != null)
            setWantedSize(p);
    }

    @Override
    public void renderContents(boolean textBlack, IGrDriver igd) {
        contents.render(textBlack, 0, 0, igd);
    }

    /**
     * This class, created just before midnight (heading into 17th February 2018),
     *  is for common stuff between - midnight - UILabel and UITextButton.
     */
    public static class Contents {
        private String lastText = "", textFormatted = "";
        private Size lastSize = new Size(0, 0);
        private String lastOverride = null;
        private boolean lastOverrideUE8 = false;
        private int lastBw = 1;
        public final int textHeight;
        public Contents(int th) {
            textHeight = th;
        }
        public Size update(Size sz, int bw, String text) {
            // run formatting...
            Size sz2 = null;
            boolean overrideChanged = false;
            if (lastOverride != null) {
                if (FontManager.fontOverride == null) {
                    overrideChanged = true;
                } else if (!FontManager.fontOverride.equals(lastOverride)) {
                    overrideChanged = true;
                }
            } else if (FontManager.fontOverride != null) {
                overrideChanged = true;
            } else if (lastOverrideUE8 != FontManager.fontOverrideUE8) {
                overrideChanged = true;
            }
            if ((!lastText.equals(text)) || (lastBw != bw) || (!lastSize.sizeEquals(sz)) || overrideChanged) {
                lastText = text;
                lastSize = sz;
                lastBw = bw;
                lastOverride = FontManager.fontOverride;
                lastOverrideUE8 = FontManager.fontOverrideUE8;
                textFormatted = FontManager.formatTextFor(text, textHeight, sz.width - (bw * 2));
                // You may be wondering why this is set up the way it is.
                // The answer is simply that B's height is what we need to be given the width,
                //  and A is what we want to be, width and height alike.
                Size a = getRecommendedTextSize(text, textHeight, bw);
                Size b = getRecommendedTextSize(textFormatted, textHeight, bw);
                sz2 = new Size(a.width, b.height);
            }
            return sz2;
        }

        public void render(boolean blackText, int x, int y, IGrDriver igd) {
            FontManager.drawString(igd, x + lastBw, y + lastBw, textFormatted, true, blackText, textHeight);
        }
    }

    // Sort of a "lite" UILabel.
    public static class StatusLine {
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
            Size sz = statusLine.update(lastSize, bw, text);
            if (sz != null) {
                height = sz.height;
                pokeLastSize(w, height);
            }
            UIBorderedElement.drawBorder(igd, 2, bw, x, y, w, height);
            boolean statusLineBT = UIBorderedElement.getBlackTextFlag(2);
            statusLine.render(statusLineBT, x, y, igd);
        }

        private void pokeLastSize(int w, int h) {
            if ((lastSize.width != w) || (lastSize.height != h))
                lastSize = new Size(w, h);
        }
    }

    // NOTE: Assumes the label is already formatted accordingly.
    // If not, expect it to go off the right of the screen if need be.
    // If you want multiline support, use a Contents instance.
    public static int drawLabel(IGrDriver igd, int wid, int ox, int oy, String string, int mode, int height) {
        int h = UIBorderedElement.getRecommendedBorderWidth(height);
        int h2 = height + (h * 2) - (height / 8);
        UIBorderedElement.drawBorder(igd, mode + 2, h, ox, oy, wid, h2);
        FontManager.drawString(igd, ox + h, oy + h, string, true, UIBorderedElement.getBlackTextFlag(mode + 2), height);
        return wid;
    }
}
