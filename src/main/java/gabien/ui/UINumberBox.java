/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.IGrInDriver;

/**
 *
 */
public class UINumberBox extends UIElement {
    public int textHeight;

    public UINumberBox(int h) {
        textHeight = h;
        setBounds(getRecommendedSize(textHeight));
    }

    public static Rect getRecommendedSize(int height) {
        return UILabel.getRecommendedSize("12344957", height);
    }

    // The caching exists so that edits have to be confirmed for onEdit usage.
    private int editingCNumber = 0;

    private int editingNLast = 0;
    public int number = 0;

    public boolean readOnly = false;
    public Runnable onEdit = new Runnable() {

        @Override
        public void run() {
            // do nothing-this is one of those boxes that just sits there.
        }
    };

    private boolean tempDisableSelection = false;

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean selected, IGrInDriver igd) {
        selected &= !tempDisableSelection;
        if (number != editingNLast) {
            editingCNumber = number;
            editingNLast = number;
        } else if (!selected) {
            number = editingCNumber;
        }
        if (selected && (!readOnly)) {
            int NNum = number;
            String ss = igd.maintain(ox, oy + (textHeight / 2), elementBounds.width, String.valueOf(NNum));
            try {
                NNum = Integer.parseInt(ss);
            } catch (Exception e) {
                NNum = 0;
            }
            if (igd.isKeyJustPressed(IGrInDriver.VK_ENTER)) {
                editingCNumber = number;
                onEdit.run();
                igd.clearKeys();
                tempDisableSelection = true;
            }
            number = NNum;
            editingNLast = number;
        }
        UILabel.drawLabel(igd, elementBounds.width, ox, oy, Integer.toString(number), selected, textHeight);
    }

    @Override
    public void handleClick(int x, int y, int button) {
        tempDisableSelection = false;
    }

}
