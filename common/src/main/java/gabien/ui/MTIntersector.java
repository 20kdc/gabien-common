/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

/**
 * A GC-friendly and MT-friendly utility singleton for intersecting rectangles.
 * NOTE - as a singleton, assume that when you call out, this gets altered.
 * Created on 10th February 2018.
 */
public class MTIntersector {
    public static MTIntersector singleton = new MTIntersector();
    private ThreadLocal<Intersector> intersect = new ThreadLocal<Intersector>();
    private MTIntersector() {

    }
    public Intersector get() {
        Intersector it = intersect.get();
        if (it == null) {
            it = new Intersector();
            intersect.set(it);
        }
        return it;
    }
}
