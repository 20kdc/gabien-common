/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;

// This serves a dual purpose:
// 1. text is *always* the current text in the box.
//    It is only reverted a frame after deselection.
//    (This makes it useful for dialogue boxes where the selection behavior is non-obvious.)
// 2. onEdit is called when enter is pressed, and otherwise the text will revert.
//    (This makes it useful for property-editor interfaces which need that kind of confirmation.)
public class UITextBox extends UIElement {
    public int height;

    public UITextBox(int h) {
        height = h;
        setBounds(getRecommendedSize(height));
    }

    public static Rect getRecommendedSize(int height) {
        return UILabel.getRecommendedSize("the quick brown fox jumped over the lazy dog", height);
    }

    public String text = "";
    private String textLastSeen = "";
    private String textCStr = "";
    public Runnable onEdit = new Runnable() {

        @Override
        public void run() {
            // do nothing-this is one of those boxes that just sits there.
        }
    };

    private boolean tempDisableSelection = false;

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime,
                                boolean selected, IGrInDriver igd) {
        selected &= !tempDisableSelection;
        if (!textLastSeen.equals(text)) {
            textCStr = text;
            textLastSeen = text;
        } else if (!selected) {
            text = textCStr;
        }
        Rect bounds = getBounds();
        if (selected) {
            String ss = igd.maintain(ox, oy + (height / 2), bounds.width, text);
            text = ss;
            textLastSeen = ss;
            if (igd.isKeyJustPressed(IGrInDriver.VK_ENTER)) {
                textCStr = text;
                onEdit.run();
                igd.clearKeys();
                tempDisableSelection = true;
            }
        }
        UILabel.drawLabel(igd, bounds.width, ox, oy, text, selected ? 2 : 1, height);
    }

    @Override
    public void handleClick(int x, int y, int button) {
        tempDisableSelection = false;
    }
}
