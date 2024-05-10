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
 * 'List layout'.
 * Beware that this layout assumes it has infinite space in which to operate.
 * Copied from UIScrollLayout 10th May, 2024.
 */
public class UIListLayout extends UIBaseListOfStuffLayout {
    public final boolean vertical;

    public UIListLayout(boolean vertical) {
        super(false);
        this.vertical = vertical;
    }

    public UIListLayout(boolean vertical, UIElement... contents) {
        this(vertical);
        panelsSet(contents);
        forceToRecommended();
    }

    public UIListLayout(boolean vertical, Iterable<UIElement> contents) {
        this(vertical);
        panelsSet(contents);
        forceToRecommended();
    }

    /**
     * Used in the two layout handlers below. 
     */
    private int getLengthForBreadth(int breadth) {
        int total = 0;
        Iterable<UIElement> contentsIterable = layoutGetElementsIterable();
        if (vertical) {
            for (UIElement p : contentsIterable)
                total += p.layoutGetHForW(breadth);
        } else {
            for (UIElement p : contentsIterable)
                total += p.layoutGetWForH(breadth);
        }
        return total;
    }

    /**
     * Used in the two layout handlers below. 
     */
    private int getBreadthForLength(int length) {
        return vertical ? getWantedSize().width : getWantedSize().height;
    }

    @Override
    public int layoutGetHForW(int width) {
        if (vertical) {
            return getLengthForBreadth(width);
        } else {
            return getBreadthForLength(width);
        }
    }

    @Override
    public int layoutGetWForH(int height) {
        if (vertical) {
            return getBreadthForLength(height);
        } else {
            return getLengthForBreadth(height);
        }
    }

    @Override
    protected @Nullable Size layoutRecalculateMetricsImpl() {
        int idealLength = 0;
        int idealBreadth = 0;

        // The UIScrollLayout here gives a scenario assuming the scrollbar is not in use.
        // What's possible is that an element or group of elements might flip between the two states,
        //  dependent on width, which is altered indirectly by height via the scrollbar's usage.
        // Now, this shouldn't be an issue so long as a greater width does not lead to a greater height.
        // If a greater width leads to a lesser height, then it stays off.
        // If a greater width leads to a greater height, then it'll loop.
        // (Interchange width/height as makes sense.)

        // The "layoutScrollbounds" at the bottom then fixes positions & allElements.

        if (vertical) {
            for (UIElement p : layoutGetElementsIterable()) {
                Size pw = p.getWantedSize();
                idealBreadth = Math.max(idealBreadth, pw.width);
                idealLength += pw.height;
            }
        } else {
            for (UIElement p : layoutGetElementsIterable()) {
                Size pw = p.getWantedSize();
                idealBreadth = Math.max(idealBreadth, pw.height);
                idealLength += pw.width;
            }
        }

        if (vertical) {
            return new Size(idealBreadth, idealLength);
        } else {
            return new Size(idealLength, idealBreadth);
        }
    }

    @Override
    protected void layoutRunImpl() {
        Size bounds = getSize();
        int boundsLength = vertical ? bounds.height : bounds.width;

        int rY = 0;

        for (UIElement p : layoutGetElements()) {
            layoutSetElementVis(p, false);
            int oRY = rY;
            int elmLength;
            if (vertical) {
                int breadth = bounds.width;
                elmLength = p.layoutGetHForW(breadth);
                p.setForcedBounds(this, new Rect(0, rY, breadth, elmLength));
            } else {
                int breadth = bounds.height;
                elmLength = p.layoutGetWForH(breadth);
                p.setForcedBounds(this, new Rect(rY, 0, elmLength, breadth));
            }
            rY += elmLength;
            if (oRY <= -elmLength)
                continue;
            if (oRY >= boundsLength)
                continue;
            layoutSetElementVis(p, true);
        }
    }
}
