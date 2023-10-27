/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.layouts;

import org.eclipse.jdt.annotation.Nullable;

import gabien.ui.UIElement;
import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;

/**
 * Covers simple cases where you want to split something into two.
 * With the introduction of IPCRESS,
 *  the algorithm needs to change in order to prevent tons of layout bugs.
 * So, it is now as follows:
 * The dividing line starts off exactly at the position the weight suggests.
 * If this fails (insufficient room), the old ... "weighted-concession algorithm"? is used.
 *
 * Created on 6/17/17. Updated for IPCRESS probably February 16th 2017, it's now February 18th 2017.
 * Updated for Accelerator 27th October 2023.
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

        layoutRecalculateMetrics();
        setForcedBounds(null, new Rect(getWantedSize()));
    }

    public static UIElement produceSideAlignedList(boolean rd, boolean vertical, UIElement... elements) {
        return produceSideAlignedList(rd, vertical, elements, 0, elements.length);
    }

    public static UIElement produceSideAlignedList(boolean rd, boolean vertical, UIElement[] elements, int offset, int length) {
        if (length == 0)
            throw new RuntimeException("no arguments");
        if (length == 1)
            return elements[offset];
        if (rd) {
            // Right/Down
            UIElement remainder = produceSideAlignedList(rd, vertical, elements, offset + 1, length - 1);
            return new UISplitterLayout(elements[offset], remainder, vertical, 1);
        } else {
            // Left/Up
            UIElement remainder = produceSideAlignedList(rd, vertical, elements, offset, length - 1);
            return new UISplitterLayout(remainder, elements[offset + (length - 1)], vertical, 0);
        }
    }

    /**
     * Gets the "ideal split point" between elements A & B for the given length assuming infinite breadth.
     */
    private int getSplitPointPixels(int allSpace) {
        Size aWanted = a.getWantedSize(), bWanted = b.getWantedSize();
        int aInitial;
        int bInitial;
        if (vertical) {
            aInitial = aWanted.height;
            bInitial = bWanted.height;
        } else {
            aInitial = aWanted.width;
            bInitial = bWanted.width;
        }
        return getSplitPointPixelsWithInitial(allSpace, aInitial, bInitial);
    }

    /**
     * Get the actual split point for a finite length/breadth combo.
     */
    private int getSplitPointPixels(int allSpace, int breadth) {
        int aInitial;
        int bInitial;
        if (vertical) {
            aInitial = a.layoutGetHForW(breadth);
            bInitial = b.layoutGetHForW(breadth);
        } else {
            aInitial = a.layoutGetWForH(breadth);
            bInitial = b.layoutGetWForH(breadth);
        }
        return getSplitPointPixelsWithInitial(allSpace, aInitial, bInitial);
    }

    private int getSplitPointPixelsWithInitial(int allSpace, int aInitial, int bInitial) {
        int room = allSpace;
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
        return exactPos;
    }

    @Override
    public int layoutGetWForH(int height) {
        if (vertical) {
            int splitPx = getSplitPointPixels(height);
            int remainder = height - splitPx;
            return Math.max(a.layoutGetWForH(splitPx), b.layoutGetWForH(remainder));
        } else {
            return a.layoutGetWForH(height) + b.layoutGetWForH(height);
        }
    }

    @Override
    public int layoutGetHForW(int width) {
        if (vertical) {
            return a.layoutGetHForW(width) + b.layoutGetHForW(width);
        } else {
            int splitPx = getSplitPointPixels(width);
            int remainder = width - splitPx;
            return Math.max(a.layoutGetHForW(splitPx), b.layoutGetHForW(remainder));
        }
    }

    @Override
    protected @Nullable Size layoutRecalculateMetricsImpl() {
        Size aWanted = a.getWantedSize();
        Size bWanted = b.getWantedSize();
        if (vertical) {
            return new Size(Math.max(aWanted.width, bWanted.width), aWanted.height + bWanted.height);
        } else {
            return new Size(aWanted.width + bWanted.width, Math.max(aWanted.height, bWanted.height));
        }
    }

    @Override
    protected void layoutRunImpl() {
        int allSpace, breadth;
        Size r = getSize();
        if (vertical) {
            allSpace = r.height;
            breadth = r.width;
        } else {
            allSpace = r.width;
            breadth = r.height;
        }
        int exactPos = getSplitPointPixels(allSpace, breadth);
        if (vertical) {
            a.setForcedBounds(this, new Rect(0, 0, r.width, exactPos));
            b.setForcedBounds(this, new Rect(0, exactPos, r.width, allSpace - exactPos));
        } else {
            a.setForcedBounds(this, new Rect(0, 0, exactPos, r.height));
            b.setForcedBounds(this, new Rect(exactPos, 0, allSpace - exactPos, r.height));
        }
    }
}
