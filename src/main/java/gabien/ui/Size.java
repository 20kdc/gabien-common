/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

/**
 * Created on February 16th 2018 to be a superclass to Rect, to avoid breaking relativity.
 */
public class Size {
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

    @Override
    public boolean equals(Object o) {
        if (o.getClass() == Size.class) {
            Size s = (Size) o;
            return (s.width == width) && (s.height == height);
        }
        return false;
    }
}
