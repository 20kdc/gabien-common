/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

/**
 * Covers simple cases where you want to split something into two.
 * With the introduction of IPCRESS,
 *  the algorithm needs to change in order to prevent tons of layout bugs.
 * So, it is now as follows:
 * The dividing line starts off exactly at the position the weight suggests.
 * If this fails (insufficient room), the old ... "weighted-concession algorithm"? is used.
 *
 * Created on 6/17/17. Updated for IPCRESS probably February 16th 2017, it's now February 18th 2017.
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
        setForcedBounds(null, new Rect(getWantedSize()));
    }

    @Override
    public void runLayout() {
        int room, allSpace;
        Size r = getSize();
        Size aWanted = a.getWantedSize(), bWanted = b.getWantedSize();
        int aInitial;
        int bInitial;
        if (vertical) {
            allSpace = room = r.height;
            aInitial = aWanted.height;
            bInitial = bWanted.height;
        } else {
            allSpace = room = r.width;
            aInitial = aWanted.width;
            bInitial = bWanted.width;
        }
        room -= aInitial + bInitial;
        // Room is now the amount of spare space available.
        int exactPos = (int) (splitPoint * allSpace);
        if (room >= 0) {
            // If we *can* table-align, do so, but give that up if need be
            boolean newAlg = ((exactPos >= aInitial) && (exactPos <= (allSpace - bInitial)));
            int oldAlg = ((int) (splitPoint * room)) + aInitial;
            if (!newAlg)
                exactPos = oldAlg;
        } else {
            // If the weight is 1/0, just prioritize that,
            //  since 1.0d/0.0d are used on elements that should use exactly what they want and no more/less
            if (splitPoint == 1) {
                exactPos = allSpace - bInitial;
            } else if (splitPoint == 0) {
                exactPos = aInitial;
            }
            // That's not working? go to minimum usability mode
            if ((exactPos < 0) || (exactPos > allSpace))
                exactPos = allSpace / 2;
        }
        Size newWanted;
        if (vertical) {
            a.setForcedBounds(this, new Rect(0, 0, r.width, exactPos));
            b.setForcedBounds(this, new Rect(0, exactPos, r.width, allSpace - exactPos));
            newWanted = new Size(Math.max(aWanted.width, bWanted.width), aInitial + bInitial);
        } else {
            a.setForcedBounds(this, new Rect(0, 0, exactPos, r.height));
            b.setForcedBounds(this, new Rect(exactPos, 0, allSpace - exactPos, r.height));
            newWanted = new Size(aInitial + bInitial, Math.max(aWanted.height, bWanted.height));
        }
        setWantedSize(newWanted);
    }
}
