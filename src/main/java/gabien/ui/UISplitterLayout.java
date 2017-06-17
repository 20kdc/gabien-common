/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package gabien.ui;

/**
 * Covers simple cases where you want to split something into two.
 * The split is between A's initial value and B's initial value.
 * Created on 6/17/17.
 */
public class UISplitterLayout extends UIPanel {
    public final UIElement a;
    public final UIElement b;
    public final int aInitial;
    public final int bInitial;
    public final boolean vertical;
    // Needs a relayout after being changed.
    public double splitPoint;
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
