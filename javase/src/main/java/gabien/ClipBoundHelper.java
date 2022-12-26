/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.awt.*;

/**
 * Used to plot relative coordinates
 * 1   2
 *
 * X6 9
 *  7 8
 * 4   3
 *
 * X: 5 and 10
 * Created on 01/06/17.
 */
public class ClipBoundHelper {
    int x = 0;
    int y = 0;
    Polygon p = new Polygon();

    public void point(int i, int i1) {
        x += i;
        y += i1;
        p.addPoint(x, y);
    }
}
