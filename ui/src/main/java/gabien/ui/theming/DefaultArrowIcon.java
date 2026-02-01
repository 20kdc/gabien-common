/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import gabien.render.IDrawable;
import gabien.render.IGrDriver;

/**
 * Just to make the scrollbars nicer.
 * Created 29th October, 2023.
 */
public class DefaultArrowIcon implements IDrawable {
    // size of the grid used for the points
    private final static int PSC = 8;
    private int[] points = {
            4, 2,
            6, 6,
            2, 6
    };
    private final float r, g, b, a;

    // default arrows for dark themes
    public static final DefaultArrowIcon DARK_U = new DefaultArrowIcon(0, 1, 1, 1, 1);
    public static final DefaultArrowIcon DARK_R = new DefaultArrowIcon(1, 1, 1, 1, 1);
    public static final DefaultArrowIcon DARK_D = new DefaultArrowIcon(2, 1, 1, 1, 1);
    public static final DefaultArrowIcon DARK_L = new DefaultArrowIcon(3, 1, 1, 1, 1);

    public DefaultArrowIcon(int rotation, float r, float g, float b, float a) {
        // rotate 90 degrees the specified number of times
        for (int i = 0; i < rotation; i++) {
            for (int j = 0; j < 6; j += 2) {
                int oX = points[j];
                int oY = points[j + 1];
                points[j] = PSC - oY;
                points[j + 1] = oX;
            }
        }
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public float getRegionWidth() {
        return 16;
    }

    @Override
    public float getRegionHeight() {
        return 16;
    }

    @Override
    public void drawTo(float x, float y, float w, float h, IGrDriver igd) {
        float aX = ((points[0] * w) / (float) PSC) + x;
        float aY = ((points[1] * h) / (float) PSC) + y;
        float bX = ((points[2] * w) / (float) PSC) + x;
        float bY = ((points[3] * h) / (float) PSC) + y;
        float cX = ((points[4] * w) / (float) PSC) + x;
        float cY = ((points[5] * h) / (float) PSC) + y;
        igd.drawXYSTRGBA(IGrDriver.BLEND_NORMAL, 0, null, aX, aY, 0, 0, r, g, b, a, bX, bY, 0, 0, r, g, b, a, cX, cY, 0, 0, r, g, b, a);
    }
}
