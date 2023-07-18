/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

/**
 * Created on February 16th 2018 to be a superclass to Rect, to avoid breaking relativity.
 */
public class Size {
    public static final Size ZERO = new Size(0, 0);

    public final int width, height;
    public Size(int i1, int i2) {
        width = i1;
        height = i2;
    }

    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return width + "," + height;
    }

    public boolean sizeEquals(Size s) {
        return (s.width == width) && (s.height == height);
    }

    public Size sizeMax(Size b) {
        int w = b.width;
        if (width > b.width)
            w = width;
        int h = b.height;
        if (height > b.height)
            h = height;
        return new Size(w, h);
    }

    public int area() {
        return width * height;
    }
}
