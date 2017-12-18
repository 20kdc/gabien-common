/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;

public class UITextButton extends UIButton {
    public String text = "";
    public final int textHeight;

    public UITextButton(int h, String tex, Runnable click) {
        onClick = click;
        text = tex;
        textHeight = h;
        setBounds(getRecommendedSize(text, h));
    }

    public static Rect getRecommendedSize(String text, int txh) {
        // See UILabel for the logic behind only adding margin once to the rectangle
        int margin = txh / 8;
        // Notably, there's an additional horizontal bit of margin for contrast against the (light) sides of the button.
        return new Rect(0, 0, UILabel.getTextLength(text, txh) + (margin * 2) + 2, txh + margin);
    }

    public UITextButton togglable() {
        toggle = true;
        return this;
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean selected, IGrInDriver igd) {
        super.updateAndRender(ox, oy, DeltaTime, selected, igd);
        Rect elementBounds = getBounds();
        int margin = getPressOffset(elementBounds.height);
        int m2 = 1 + (margin / 3);
        int po = 0;
        if (state)
            po = m2;
        // height could be elementBounds.height - m2, but some cases exist where buttons are resized
        UILabel.drawString(igd, ox + 2 + margin - m2, oy + 1 + (state ? (margin + po) : margin), text, true, textHeight);
    }
}
