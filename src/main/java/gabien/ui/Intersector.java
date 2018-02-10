/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

/**
 * A GC-friendly utility class for intersecting rectangles.
 * NOTE: Not MT-friendly, so use ThreadLocal if you're particularly worried about MT.
 * Created on 10th February 2018.
 */
public class Intersector {
    public int x, y, width, height;

    public void set(Rect viewRct) {
        set(viewRct.x, viewRct.y, viewRct.width, viewRct.height);
    }

    public void set(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean intersect(Rect r) {
        return intersect(r.x, r.y, r.width, r.height);
    }

    public boolean intersect(int bx, int by, int bw, int bh) {
        if (intersects1i(bx, bw, x, width)) {
            int xStart = Math.max(bx, x);
            int xW = intersect1iWidth(bx, bw, x, width, xStart);
            x = xStart;
            width = xW;
            if (intersects1i(by, bh, y, height)) {
                int yStart = Math.max(by, y);
                int yH = intersect1iWidth(by, bh, y, height, yStart);
                y = yStart;
                height = yH;
                return true;
            } else {
                height = 0;
            }
        } else {
            width = 0;
            height = 0;
        }
        return false;
    }

    // If this is true, line intersection start is Math.max(A, B)
    //  and line intersection width is Math.min(A + AL, B + BL) - start
    public static boolean intersects1i(int A, int AL, int B, int BL) {
        if (AL <= 0)
            return false;
        if (BL <= 0)
            return false;
        if (A >= B)
            if (A < B + BL)
                return true;
        if (B >= A)
            if (B < A + AL)
                return true;
        return false;
    }

    public static int intersect1iWidth(int A, int AL, int B, int BL, int start) {
        return Math.min(A + AL, B + BL) - start;
    }
}
