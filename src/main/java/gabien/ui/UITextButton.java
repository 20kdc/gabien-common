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
    public boolean x2 = false;

    public UITextButton() {

    }

    // makes array initializers easier
    public UITextButton(boolean x2, String text, Runnable onClick) {
        OnClick = onClick;
        this.x2 = x2;
        Text = text;
        setRecommendedSize();
    }

    public void setRecommendedSize() {
        setBounds(new Rect(0, 0, ((x2 ? 16 : 8) * Text.length()) + (x2 ? 4 : 2), x2 ? 20 : 10));
    }

    public UITextButton togglable() {
        toggle = true;
        return this;
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime,
            boolean selected, IGrInDriver igd) {
        int po = state ? (x2?6:3) : 0;
        if (PressedTime > 0) {
            PressedTime -= DeltaTime;
            if (PressedTime <= 0)
                state = false;
        }
        IImage i = GaBIEn.getImage("textButton.png", 255, 0, 255);
        igd.blitBCKImage((x2?6:0)+po, 0, (x2?2:1), (x2?20:10), ox, oy, i);
        for (int pp = (x2?2:1); pp < elementBounds.width - 1; pp+=(x2?2:1))
            igd.blitBCKImage((x2?8:1) + po, 0, (x2?2:1), (x2?20:10), ox + pp, oy, i);
        igd.blitBCKImage((x2?10:2) + po, 0, (x2?2:1), (x2?20:10), ox + (elementBounds.width - (x2?2:1)), oy, i);
        UILabel.drawString(igd, ox + (x2 ? 2 : 1), oy + (state ? (x2 ? 4 : 2) : (x2 ? 2 : 1)), Text, true, x2);
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
