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
public class UISplitterLayout extends UIElement.UIPanel {
    public final UIElement a;
    public final UIElement b;

    public final boolean vertical;
    public final double splitPoint;

    public UISplitterLayout(UIElement aA, UIElement bA, boolean v, int dividend, int divisor) {
        this(aA, bA, v, ((double) dividend) / divisor);
    }

    public UISplitterLayout(UIElement aA, UIElement bA, boolean v, double weight) {
        vertical = v;
        a = aA;
        b = bA;
        layoutAddElement(a);
        layoutAddElement(b);
        splitPoint = weight;
        runLayout();
        Size gws = getWantedSize();
        setForcedBounds(null, new Rect(0, 0, gws.width, gws.height));
    }

    @Override
    public void runLayout() {
        int room;
        Size r = getSize();
        Size aWanted = a.getWantedSize(), bWanted = b.getWantedSize();
        int aInitial;
        int bInitial;
        if (vertical) {
            room = r.height;
            aInitial = aWanted.height;
            bInitial = bWanted.height;
        } else {
            room = r.width;
            aInitial = aWanted.width;
            bInitial = bWanted.width;
        }
        room -= aInitial + bInitial;
        int aRoom = ((int) (splitPoint * room)) + aInitial;
        int bRoom = (room + aInitial + bInitial) - aRoom;
        Size newWanted;
        if (vertical) {
            a.setForcedBounds(this, new Rect(0, 0, r.width, aRoom));
            b.setForcedBounds(this, new Rect(0, aRoom, r.width, bRoom));
            newWanted = new Size(Math.max(aWanted.width, bWanted.width), aInitial + bInitial);
        } else {
            a.setForcedBounds(this, new Rect(0, 0, aRoom, r.height));
            b.setForcedBounds(this, new Rect(aRoom, 0, bRoom, r.height));
            newWanted = new Size(aInitial + bInitial, Math.max(aWanted.height, bWanted.height));
        }
        setWantedSize(newWanted);
    }
}
