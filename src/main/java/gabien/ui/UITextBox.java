/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
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
        if (selected) {
            String ss = igd.maintain(ox, oy + (height / 2), elementBounds.width, text);
            text = ss;
            textLastSeen = ss;
            if (igd.isKeyJustPressed(IGrInDriver.VK_ENTER)) {
                textCStr = text;
                onEdit.run();
                igd.clearKeys();
                tempDisableSelection = true;
            }
        }
        UILabel.drawLabel(igd, elementBounds.width, ox, oy, text, selected, height);
    }

    @Override
    public void handleClick(int x, int y, int button) {
        tempDisableSelection = false;
    }
}
