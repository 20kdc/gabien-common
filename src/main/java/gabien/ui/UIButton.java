/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;


import gabien.GaBIEn;
import gabien.IGrDriver;
import gabien.IGrInDriver;
import gabien.IImage;

/**
 * This was actually a totally different class at one point.
 * Now it's a superclass of UITextButton.
 * Unknown creation date.
 */
public class UIButton extends UIElement {
    public Runnable onClick;
    public double pressedTime = 0;
    public boolean state = false;
    public boolean toggle = false;
    private Rect contentsBoundsST = new Rect(0, 0, 0, 0);
    private Rect contentsBoundsSF = new Rect(0, 0, 0, 0);

    @Override
    public void setBounds(Rect r) {
        super.setBounds(r);
        contentsBoundsST = getContentsRect(r.width, r.height, true);
        contentsBoundsSF = getContentsRect(r.width, r.height, false);
    }

    public Rect getContentsRect() {
        return state ? contentsBoundsST : contentsBoundsSF;
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean selected, IGrInDriver igd) {
        if (pressedTime > 0) {
            pressedTime -= DeltaTime;
            if (pressedTime <= 0)
                state = false;
        }
        Rect b = getBounds();
        drawButton(ox, oy, b.width, b.height, state, igd);
    }

    public static void drawButton(int ox, int oy, int w, int h, boolean state, IGrDriver igd) {
        if ((h != 9) && (h != 18)) {
            // no bitmaps here
            int margin = getPressOffset(h);
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
            drawButtonCore(c1 * 3, c2 * 3, c3, ox, oy + ooy, margin, w, h, igd, false);
            int m2 = 1 + (margin / 3);
            drawButtonCore(c1, c2, c3, ox + m2, oy + m2 + ooy, margin - m2, w - (m2 * 2), h - (m2 * 2), igd, true);
        } else {
            boolean x2 = h == 18;
            int po = state ? (x2 ? 6 : 3) : 0;
            IImage i = GaBIEn.getImageCKEx("textButton.png", false, true, 255, 0, 255);
            igd.blitImage((x2 ? 6 : 0) + po, 0, (x2 ? 2 : 1), (x2 ? 20 : 10), ox, oy, i);
            for (int pp = (x2 ? 2 : 1); pp < w - 1; pp += (x2 ? 2 : 1))
                igd.blitImage((x2 ? 8 : 1) + po, 0, (x2 ? 2 : 1), (x2 ? 20 : 10), ox + pp, oy, i);
            igd.blitImage((x2 ? 10 : 2) + po, 0, (x2 ? 2 : 1), (x2 ? 20 : 10), ox + (w - (x2 ? 2 : 1)), oy, i);
        }
    }

    protected static Rect getContentsRect(int width, int height, boolean state) {
        int margin, po;
        if ((height != 9) && (height != 18)) {
            margin = getPressOffset(height);
            po = margin;
        } else if (height == 18) {
            po = 2;
            margin = 2;
        } else {
            po = 1;
            margin = 1;
        }
        return new Rect(margin, margin + (state ? po : 0), width - (margin * 2), height - (margin * 2));
    }

    protected static int getPressOffset(int h) {
        return (h - (h / 5)) / 8;
    }

    private static void drawButtonCore(int c1, int c2, int c3, int ox, int oy, int margin, int width, int height, IGrDriver igd, boolean drawBack) {
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
