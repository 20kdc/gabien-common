/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

/**
 * Covers simple cases where you want to split something into two.
 * If weight is used, the split is between A laid out by initial value and B laid out by initial value (as would make sense for a weight).
 * If dividend/divisor is used, the initial values are zeroed, thus giving an exact fractional split.
 * Created on 6/17/17.
 */
public class UISplitterLayout extends UIPanel {
    public final UIElement a;
    public final UIElement b;
    private int aInitial;
    private int bInitial;
    public final boolean vertical;

    // Needs a relayout after being changed.
    public double splitPoint;

    public UISplitterLayout(UIElement aA, UIElement bA, boolean v, int dividend, int divisor) {
        this(aA, bA, v, ((double) dividend) / divisor);
        aInitial = 0;
        bInitial = 0;
    }
    public UISplitterLayout(UIElement aA, UIElement bA, boolean v, double weight) {
        vertical = v;
        a = aA;
        b = bA;
        allElements.add(a);
        allElements.add(b);
        int aStride;
        int bStride;
        if (vertical) {
            aInitial = a.getBounds().height;
            bInitial = b.getBounds().height;
            aStride = a.getBounds().width;
            bStride = b.getBounds().width;
            setBounds(new Rect(0, 0, Math.max(aStride, bStride), aInitial + bInitial));
        } else {
            aInitial = a.getBounds().width;
            bInitial = b.getBounds().width;
            aStride = a.getBounds().height;
            bStride = b.getBounds().height;
            setBounds(new Rect(0, 0, aInitial + bInitial, Math.max(aStride, bStride)));
        }
        splitPoint = weight;
    }

    @Override
    public void setBounds(Rect r) {
        super.setBounds(r);
        int room;
        if (vertical) {
            room = r.height;
        } else {
            room = r.width;
        }
        room -= aInitial + bInitial;
        int aRoom = ((int) (splitPoint * room)) + aInitial;
        int bRoom = (room + aInitial + bInitial) - aRoom;
        if (vertical) {
            a.setBounds(new Rect(0, 0, r.width, aRoom));
            b.setBounds(new Rect(0, aRoom, r.width, bRoom));
        } else {
            a.setBounds(new Rect(0, 0, aRoom, r.height));
            b.setBounds(new Rect(aRoom, 0, bRoom, r.height));
        }
    }
}
