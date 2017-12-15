/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
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
        int boundsWidth = getBounds().width;
        if (selected && (!readOnly)) {
            String ss = igd.maintain(ox, oy + (textHeight / 2), boundsWidth, String.valueOf(number));
            int lastMinusIdx = ss.lastIndexOf("-");
            boolean doInvertLater = false;
            if (lastMinusIdx > 0) {
                String pre = ss.substring(0, lastMinusIdx);
                String post = ss.substring(lastMinusIdx + 1);
                ss = pre + post;
                doInvertLater = true;
            }
            int newNum = 0;
            try {
                newNum = Integer.parseInt(ss);
                if (doInvertLater)
                    newNum = -newNum;
            } catch (Exception e) {
            }
            if (igd.isKeyJustPressed(IGrInDriver.VK_ENTER)) {
                editingCNumber = number;
                onEdit.run();
                igd.clearKeys();
                tempDisableSelection = true;
            }
            number = newNum;
            editingNLast = number;
        }
        UILabel.drawLabel(igd, boundsWidth, ox, oy, Integer.toString(number), selected ? 2 : 1, textHeight);
    }

    @Override
    public void handleClick(int x, int y, int button) {
        tempDisableSelection = false;
    }

}
