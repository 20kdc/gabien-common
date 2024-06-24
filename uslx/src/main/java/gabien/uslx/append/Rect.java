/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.append;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A basic rectangle class to make life easier.
 * Creation date unknown.
 */
public final class Rect extends Size {
    public static final Rect ZERO = new Rect(0, 0, 0, 0);

    public final int x, y, right, bottom;

    public Rect(int x, int y, int w, int h) {
        super(w, h);
        this.x = x;
        this.y = y;
        this.right = x + w;
        this.bottom = y + h;
    }

    public Rect(@NonNull Size size) {
        this(0, 0, size.width, size.height);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + super.toString();
    }

    public boolean rectEquals(@NonNull Rect o) {
        return sizeEquals(o) && (o.x == x) && (o.y == y);
    }

    public boolean contains(int x, int y) {
        return (x >= this.x) && (y >= this.y) && (x < this.right) && (y < this.bottom);
    }

    public boolean intersects(@NonNull Rect rect) {
        return (rect.x < right) && (rect.right > x) && (rect.y < bottom) && (rect.bottom > y);
    }

    public @Nullable Rect getIntersection(@NonNull Rect rect) {
        if (RectIntersector.intersects1i(rect.x, rect.width, x, width)) {
            int xStart = Math.max(rect.x, x);
            int xW = RectIntersector.intersect1iWidth(rect.x, rect.width, x, width, xStart);
            if (RectIntersector.intersects1i(rect.y, rect.height, y, height)) {
                int yStart = Math.max(rect.y, y);
                int yH = RectIntersector.intersect1iWidth(rect.y, rect.height, y, height, yStart);
                return new Rect(xStart, yStart, xW, yH);
            }
        }
        return null;
    }

    /**
     * Shrinks the rectangle by the given margin.
     */
    public @NonNull Rect margin(int l, int u, int r, int d) {
        return new Rect(x + l, y + u, width - (l + r), height - (u + d));
    }

    /**
     * Applies the same amount of X/Y position change as occurred between from and to.
     * Also considers the amount of height-cropping involved -
     * from's size is compared to this rectangle's size, and the width/height changes are scaled accordingly.
     * Useful for cropping, as you can crop in screen-space then apply the transform in sprite-space.
     */
    public @NonNull Rect transformed(Rect from, Rect to) {
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

    /**
     * Multiplies this rect, as if applied to an image undergoing nearest-neighbour magnification.
     */
    public Rect multiplied(int tileSize) {
        return new Rect(x * tileSize, y * tileSize, width * tileSize, height * tileSize);
    }
}
