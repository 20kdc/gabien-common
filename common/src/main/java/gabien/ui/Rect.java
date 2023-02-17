/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

/**
 * A basic rectangle class to make life easier.
 * Creation date unknown.
 */
public final class Rect extends Size {
    public static final Rect ZERO = new Rect(0, 0, 0, 0);

    public final int x, y;

    public Rect(int i, int i0, int i1, int i2) {
        super(i1, i2);
        x = i;
        y = i0;
    }

    public Rect(Size size) {
        this(0, 0, size.width, size.height);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + super.toString();
    }

    public boolean rectEquals(Rect o) {
        return sizeEquals(o) && (o.x == x) && (o.y == y);
    }

    public boolean contains(int x, int y) {
        if (x >= this.x)
            if (y >= this.y)
                if (x < this.x + width)
                    if (y < this.y + height)
                        return true;
        return false;
    }

    public boolean intersects(Rect rect) {
        if (Intersector.intersects1i(rect.x, rect.width, x, width))
            if (Intersector.intersects1i(rect.y, rect.height, y, height))
                return true;
        return false;
    }

    public Rect getIntersection(Rect rect) {
        if (Intersector.intersects1i(rect.x, rect.width, x, width)) {
            int xStart = Math.max(rect.x, x);
            int xW = Intersector.intersect1iWidth(rect.x, rect.width, x, width, xStart);
            if (Intersector.intersects1i(rect.y, rect.height, y, height)) {
                int yStart = Math.max(rect.y, y);
                int yH = Intersector.intersect1iWidth(rect.y, rect.height, y, height, yStart);
                return new Rect(xStart, yStart, xW, yH);
            }
        }
        return null;
    }

    /**
     * Shrinks the rectangle by the given margin.
     */
    public Rect margin(int l, int u, int r, int d) {
        return new Rect(x + l, y + u, width - (l + r), height - (u + d));
    }

    /**
     * Applies the same amount of X/Y position change as occurred between from and to.
     * Also considers the amount of height-cropping involved -
     * from's size is compared to this rectangle's size, and the width/height changes are scaled accordingly.
     * Useful for cropping, as you can crop in screen-space then apply the transform in sprite-space.
     */
    public Rect transformed(Rect from, Rect to) {
        // If width or height is zero, then it would divide by zero - and if that didn't lead to an exception (IIRC?)
        //  one or both of the parameters would be 0 (0 * infinity == 0),
        //  so just skip that and do this:
        if ((from.width == 0) || (from.height == 0))
            return new Rect(x + (to.x - from.x), y + (to.y - from.y), 0, 0);
        if ((width == from.width) && (height == from.height))
            return new Rect(x + (to.x - from.x), y + (to.y - from.y), to.width, to.height);
        // Theoretical speedup but also worsens debugging.
        if ((to.width == from.width) && (to.height == from.height))
            return new Rect(x + (to.x - from.x), y + (to.y - from.y), width, height);

        // Notably this is set up so that the final output is the result of a multiplication,
        //  so the logic above makes sense.
        // Anyway. The idea is that for a given value in FromTo units, this ratio has to convert it to our "native units".
        // So if we use 2.5, and it uses 5, then the value is 0.5.
        double selfFromRatioW = width / (double) from.width;
        double selfFromRatioH = height / (double) from.height;
        return new Rect(x + ((int) ((to.x - from.x) * selfFromRatioW)), y + ((int) ((to.y - from.y) * selfFromRatioH)), (int) (to.width * selfFromRatioW), (int) (to.height * selfFromRatioH));
    }
}
