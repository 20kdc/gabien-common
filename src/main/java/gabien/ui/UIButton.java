/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;


import gabien.GaBIEn;
import gabien.IGrInDriver;
import gabien.IImage;

public class UIButton extends UIElement {
    public Runnable onClick;
    public double pressedTime = 0;
    public boolean state = false;
    public boolean toggle = false;

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean selected, IGrInDriver igd) {
        if (pressedTime > 0) {
            pressedTime -= DeltaTime;
            if (pressedTime <= 0)
                state = false;
        }
        Rect elementBounds = getBounds();
        boolean x2 = elementBounds.height == 18;
        if ((elementBounds.height != 10) && (elementBounds.height != 18)) {
            // no bitmaps here
            int margin = getPressOffset(elementBounds.height);
            Rect bounds = getBounds();
            int c1 = 32; // shadow
            int c2 = 64; // lit
            int c3 = 48; // middle
            int ooy = 0;
            if (state) {
                c2 = 32;
                c1 = 64;
                c3 = 16;
                ooy = margin;
            }
            drawButton(c1 * 3, c2 * 3, c3, ox, oy + ooy, margin, bounds.width, bounds.height, igd, false);
            int m2 = 1 + (margin / 3);
            drawButton(c1, c2, c3, ox + m2, oy + m2 + ooy, margin - m2, bounds.width - (m2 * 2), bounds.height - (m2 * 2), igd, true);
        } else {
            int po = state ? (x2 ? 6 : 3) : 0;
            IImage i = GaBIEn.getImageCK("textButton.png", 255, 0, 255);
            igd.blitImage((x2 ? 6 : 0) + po, 0, (x2 ? 2 : 1), (x2 ? 20 : 10), ox, oy, i);
            for (int pp = (x2 ? 2 : 1); pp < elementBounds.width - 1; pp += (x2 ? 2 : 1))
                igd.blitImage((x2 ? 8 : 1) + po, 0, (x2 ? 2 : 1), (x2 ? 20 : 10), ox + pp, oy, i);
            igd.blitImage((x2 ? 10 : 2) + po, 0, (x2 ? 2 : 1), (x2 ? 20 : 10), ox + (elementBounds.width - (x2 ? 2 : 1)), oy, i);
        }
    }

    protected int getPressOffset(int h) {
        return (h - (h / 5)) / 8;
    }

    private void drawButton(int c1, int c2, int c3, int ox, int oy, int margin, int width, int height, IGrInDriver igd, boolean drawBack) {
        igd.clearRect(c1, c1, c1, ox, oy, width, height);
        igd.clearRect(c2, c2, c2, ox, oy, width - margin, height - margin);
        if (drawBack)
            igd.clearRect(c3, c3, c3, ox + margin, oy + margin, width - (margin * 2), height - (margin * 2));
    }

    @Override
    public void handleClick(int x, int y, int button) {
        if (button == 1) {
            if (toggle) {
                state = !state;
            } else {
                state = true;
                pressedTime = 0.5;
            }
            if (onClick != null)
                onClick.run();
        }
    }
}
