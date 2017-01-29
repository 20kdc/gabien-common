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
    public boolean x2 = false;

    public UINumberBox(boolean x2) {
        this.x2 = x2;
        setBounds(getRecommendedSize(x2));
    }

    public static Rect getRecommendedSize(boolean x2) {
        return new Rect(0, 0, 32, x2 ? 18 : 9);
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

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean selected, IGrInDriver igd) {
        if (number != editingNLast) {
            editingCNumber = number;
            editingNLast = number;
        } else if (!selected) {
            number = editingCNumber;
        }
        if (selected && (!readOnly)) {
            int NNum = number;
            String ss = igd.getTypeBuffer();
            if (ss.length() > 0) {
                char c = ss.charAt(0);
                if ((c >= '0') && (c <= '9')) {
                    NNum *= 10;
                    if (NNum < 0) {
                        NNum -= c - '0';
                    } else {
                        NNum += c - '0';
                    }
                }
                if (c == '-')
                    NNum = -NNum;
            }
            igd.setTypeBuffer("");
            if (igd.isKeyJustPressed(IGrInDriver.VK_BACK_SPACE))
                NNum = 0;
            if (igd.isKeyJustPressed(IGrInDriver.VK_ENTER)) {
                editingCNumber = number;
                onEdit.run();
            }
            number = NNum;
            editingNLast = number;
        }
        if (x2) {
            UILabel.drawLabelx2(igd, elementBounds.width, ox, oy, Integer.toString(number), selected);
        } else {
            UILabel.drawLabel(igd, elementBounds.width, ox, oy, Integer.toString(number), selected);
        }
    }

    @Override
    public void handleClick(int x, int y, int button) {
    }

}
