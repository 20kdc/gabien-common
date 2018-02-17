/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.*;

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
        Size sz = getRecommendedTextSize(text, h);
        setWantedSize(sz);
        setForcedBounds(null, new Rect(0, 0, sz.width, sz.height));
    }

    @Override
    public void update(double deltaTime) {

    }

    @Override
    public void render(boolean selected, IPointer mouse, IGrInDriver igd) {
        super.render(selected, mouse, igd);
        contents.render(getSize(), getBorderWidth(), text, igd);
    }

    /**
     * This class, created just before midnight (heading into 17th February 2018),
     *  is for common stuff between - midnight - UILabel and UITextButton.
     */
    public static class Contents {
        private String lastText = "", textFormatted = "";
        private Size lastSize = new Size(0, 0);
        public final int textHeight;
        public Contents(int th) {
            textHeight = th;
        }
        public Size render(Size sz, int bw, String text, IGrDriver igd) {
            // run formatting...
            Size sz2 = null;
            if ((!lastText.equals(text)) || (!lastSize.equals(sz))) {
                lastText = text;
                lastSize = sz;
                textFormatted = FontManager.formatTextFor(text, textHeight, sz.width - (bw * 2));
                // You may be wondering why this is set up the way it is.
                // The answer is simply that B's height is what we need to be given the width,
                //  and A is what we want to be, width and height alike.
                Size a = getRecommendedTextSize(text, textHeight);
                Size b = getRecommendedTextSize(textFormatted, textHeight);
                sz2 = new Size(a.width, b.height);
            }
            FontManager.drawString(igd, bw, bw, textFormatted, true, textHeight);
            return sz2;
        }
    }

    // NOTE: Assumes the label is already formatted accordingly.
    // If not, expect it to go off the right of the screen if need be.
    // Also note that this basically only exists for compatibility,
    //  and thus doesn't actually get used in UILabel itself.
    public static int drawLabel(IGrDriver igd, int wid, int ox, int oy, String string, int mode, int height) {
        int h = UIBorderedElement.getRecommendedBorderWidth(height);
        int h2 = height + (h * 2) - (height / 8);
        UIBorderedElement.drawBorder(igd, mode + 2, h, wid, h2);
        FontManager.drawString(igd, ox + h, oy + h, string, true, height);
        return wid;
    }
}
