/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.GaBIEn;
import gabien.IGrInDriver;
import gabien.IGrInDriver.IImage;

public class UITextButton extends UIElement {
    public Runnable OnClick;
    public String Text = "";
    public double PressedTime = 0;
    public boolean state = false;
    public boolean toggle = false;
    public int textHeight;

    public UITextButton() {

    }

    // makes array initializers easier
    public UITextButton(int h, String text, Runnable onClick) {
        OnClick = onClick;
        textHeight = h;
        Text = text;
        setBounds(getRecommendedSize(text, h));
    }

    public static Rect getRecommendedSize(String text, int txh) {
        // See UILabel for the logic behind only adding margin once to the rectangle
        int margin = txh / 8;
        return new Rect(0, 0, UILabel.getTextLength(text, txh) + (margin * 2), txh + margin);
    }

    public UITextButton togglable() {
        toggle = true;
        return this;
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime,
            boolean selected, IGrInDriver igd) {
        if (PressedTime > 0) {
            PressedTime -= DeltaTime;
            if (PressedTime <= 0)
                state = false;
        }
        boolean x2 = textHeight == 16;
        if (!x2)
            if (textHeight != 8) {
                // no bitmaps here
                int margin = textHeight / 8;
                Rect bounds = getBounds();
                int c1 = 32;
                int c2 = 64;
                int c3 = 48;
                int ooy = 0;
                if (state) {
                    c2 = 32;
                    c1 = 64;
                    c3 = 16;
                    ooy = margin;
                }
                igd.clearRect(c1, c1, c1, ox, oy + ooy, bounds.width, bounds.height);
                igd.clearRect(c2, c2, c2, ox, oy + ooy, bounds.width - margin, bounds.height - margin);
                igd.clearRect(c3, c3, c3, ox + margin, oy + ooy + margin, bounds.width - (margin * 2), bounds.height - (margin * 2));
                UILabel.drawString(igd, ox + margin, oy + margin + ooy, Text, true, textHeight);
                return;
            }
        int po = state ? (x2?6:3) : 0;
        IImage i = GaBIEn.getImage("textButton.png", 255, 0, 255);
        igd.blitBCKImage((x2?6:0)+po, 0, (x2?2:1), (x2?20:10), ox, oy, i);
        for (int pp = (x2?2:1); pp < elementBounds.width - 1; pp+=(x2?2:1))
            igd.blitBCKImage((x2?8:1) + po, 0, (x2?2:1), (x2?20:10), ox + pp, oy, i);
        igd.blitBCKImage((x2?10:2) + po, 0, (x2?2:1), (x2?20:10), ox + (elementBounds.width - (x2?2:1)), oy, i);
        UILabel.drawString(igd, ox + (x2 ? 2 : 1), oy + (state ? (x2 ? 4 : 2) : (x2 ? 2 : 1)), Text, true, textHeight);
    }

    @Override
    public void handleClick(int x, int y, int button) {
        if (button == 1) {
            if (toggle) {
                state = !state;
            } else {
                state = true;
                PressedTime = 0.5;
            }
            if (OnClick != null)
                OnClick.run();
        }
    }
}
