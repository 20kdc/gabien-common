/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

/**
 * A basic rectangle class to make life easier.
 */
public class Rect {
    public int x, y, width, height;
    public Rect(int i, int i0, int i1, int i2) {
        x = i;
        y = i0;
        width = i1;
        height = i2;
    }

    public boolean contains(int x, int y) {
        if (x >= this.x)
            if (y >= this.y)
                if (x < this.x + width)
                    if (y < this.y + height)
                        return true;
        return false;
    }

    // If this is true, line intersection start is Math.max(A, B)
    //  and line intersection width is Math.min(A + AL, B + BL) - start
    private boolean lineIntersects(int A, int AL, int B, int BL) {
        if (A >= B)
            if (A < B + BL)
                return true;
        if (B >= A)
            if (B < A + AL)
                return true;
        return false;
    }

    private int lineintersectWidth(int A, int AL, int B, int BL, int start) {
        return Math.min(A + AL, B + BL) - start;
    }

    public boolean intersects(Rect rect) {
        if (lineIntersects(rect.x, rect.width, x, width))
            if (lineIntersects(rect.y, rect.height, y, height))
                return true;
        return false;
    }

    public Rect getIntersection(Rect rect) {
        if (lineIntersects(rect.x, rect.width, x, width)) {
            int xStart = Math.max(rect.x, x);
            int xW = lineintersectWidth(rect.x, rect.width, x, width, xStart);
            if (lineIntersects(rect.y, rect.height, y, height)) {
                int yStart = Math.max(rect.y, y);
                int yH = lineintersectWidth(rect.y, rect.height, y, height, yStart);
                return new Rect(xStart, yStart, xW, yH);
            }
        }
        return null;
    }


    // Applies the same amount of X/Y position change as occurred between from and to.
    // Uses to's width/height.
    // Useful for cropping, as you can crop in screen-space then apply the transform in sprite-space.
    public Rect transformed(Rect from, Rect to) {
        return new Rect(x + (to.x - from.x), y + (to.y - from.y), to.width, to.height);
    }
}
