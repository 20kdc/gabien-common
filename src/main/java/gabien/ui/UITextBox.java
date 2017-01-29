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
    public boolean x2 = false;

    public UITextBox(boolean x2) {
        this.x2 = x2;
        setBounds(getRecommendedSize(x2));
    }

    public static Rect getRecommendedSize(boolean x2) {
        return new Rect(0, 0, 32, x2 ? 18 : 9);
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

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime,
            boolean selected, IGrInDriver igd) {
        if (!textLastSeen.equals(text)) {
            textCStr = text;
            textLastSeen = text;
        } else if (!selected) {
            text = textCStr;
        }
        if (selected) {
            String NT = text;
            String ss = igd.getTypeBuffer();
            igd.setTypeBuffer("");
            ss = ss.replace("\r", "");
            ss = ss.replace("\n", "");
            ss = ss.replace("\t", "    ");
            NT += ss;
            if (igd.isKeyJustPressed(IGrInDriver.VK_BACK_SPACE))
                if (NT.length() > 0)
                    NT = NT.substring(0, NT.length() - 1);
            if (igd.isKeyJustPressed(IGrInDriver.VK_ENTER)) {
                textCStr = text;
                onEdit.run();
            }
            text = NT;
            textLastSeen = text;
        }
        if (x2) {
            UILabel.drawLabelx2(igd, elementBounds.width, ox, oy,
                    text, selected);
        } else {
            UILabel.drawLabel(igd, elementBounds.width, ox, oy,
                    text, selected);
        }
    }

    @Override
    public void handleClick(int x, int y, int button) {
    }
}
