/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IPeripherals;

// This serves a dual purpose:
// 1. text is *always* the current text in the box.
//    It is only reverted a frame after deselection.
//    (This makes it useful for dialogue boxes where the selection behavior is non-obvious.)
// 2. onEdit is called when enter is pressed, and otherwise the text will revert.
//    (This makes it useful for property-editor interfaces which need that kind of confirmation.)
public class UITextBox extends UILabel {
    public UITextBox(String text, int h) {
        super(text, h);
        borderType = 3;
    }

    public static Size getRecommendedSize(int h) {
        return getRecommendedTextSize("Highly Responsive To Eggnog", h);
    }

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
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        selected &= !tempDisableSelection;
        if (!textLastSeen.equals(text)) {
            textCStr = text;
            textLastSeen = text;
        } else if (!selected) {
            text = textCStr;
        }
        Size bounds = getSize();
        if (selected) {
            String ss = peripherals.maintain(-getBorderWidth(), (bounds.height / 2) - getBorderWidth(), bounds.width, text, null);
            text = ss;
            textLastSeen = ss;
            if (peripherals.isEnterJustPressed()) {
                textCStr = text;
                onEdit.run();
                peripherals.clearKeys();
                tempDisableSelection = true;
            }
        }
        borderType = selected ? 4 : 3;
        super.updateContents(deltaTime, selected, peripherals);
    }

    @Override
    public void handlePointerBegin(IPointer ip) {
        tempDisableSelection = false;
    }
}
