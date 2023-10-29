/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.layouts;

import org.eclipse.jdt.annotation.Nullable;

import gabien.ui.UIElement;
import gabien.ui.elements.UIScrollbar;
import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;
import gabien.wsi.IPeripherals;

/**
 * Basic scrollable layout.
 * Got moved to gabien.ui on June 9 2017 and redesigned.
 * Created on 12/29/16.
 * Updated for Accelerator 27th October 2023.
 */
public class UIScrollLayout extends UIElement.UIPanel {
    public final UIScrollbar scrollbar;
    // This is set to the scrollbar size, in full.
    private int sbSize;
    // The total 'vertical' (for a vscrollbar) area that the contents cover.
    // This is dynamic (adjusted based on current breadth!!!).
    // Thus it is calculated in layoutRunImpl.
    private int scrollLength = 0;

    private double lastScrollPoint = -1;
    private boolean earlyForceRunLayout = false;

    public UIScrollLayout(boolean vertical, int sc) {
        scrollbar = new UIScrollbar(vertical, sc);
        sbSize = sc;
    }

    public void panelsClear() {
        for (UIElement uie : layoutGetElements())
            layoutRemoveElement(uie);
        layoutRecalculateMetrics();
    }

    public void panelsAdd(UIElement uie) {
        // Store these offscreen to prevent accidental clicking.
        Size s = uie.getSize();
        uie.setForcedBounds(null, new Rect(-s.width, -s.height, s.width, s.height));
        layoutAddElement(uie);
        layoutSetElementVis(uie, false);
        earlyForceRunLayout = true;
    }

    public void panelsFinished() {
        layoutRecalculateMetrics();
    }

    /**
     * Used in the two layout handlers below. 
     */
    private int getLengthForBreadth(int breadth) {
        int total = 0;
        Iterable<UIElement> contentsIterable = layoutGetElementsIterable();
        if (scrollbar.vertical) {
            for (UIElement p : contentsIterable)
                if (p != scrollbar)
                    total += p.layoutGetHForW(breadth);
        } else {
            for (UIElement p : contentsIterable)
                if (p != scrollbar)
                    total += p.layoutGetWForH(breadth);
        }
        return total;
    }

    /**
     * Used in the two layout handlers below. 
     */
    private int getBreadthForLength(int length) {
        int defaultLength = scrollbar.vertical ? getWantedSize().height : getWantedSize().width;
        int defaultBreadth = scrollbar.vertical ? getWantedSize().width : getWantedSize().height;
        return length < defaultLength ? (defaultBreadth + sbSize) : defaultBreadth;
    }

    @Override
    public int layoutGetHForW(int width) {
        if (scrollbar.vertical) {
            return getLengthForBreadth(width);
        } else {
            return getBreadthForLength(width);
        }
    }

    @Override
    public int layoutGetWForH(int height) {
        if (scrollbar.vertical) {
            return getBreadthForLength(height);
        } else {
            return getLengthForBreadth(height);
        }
    }

    @Override
    protected @Nullable Size layoutRecalculateMetricsImpl() {
        earlyForceRunLayout = false;
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

        if (scrollbar.vertical) {
            for (UIElement p : layoutGetElementsIterable())
                if (p != scrollbar) {
                    Size pw = p.getWantedSize();
                    idealBreadth = Math.max(idealBreadth, pw.width);
                    idealLength += pw.height;
                }
        } else {
            for (UIElement p : layoutGetElementsIterable())
                if (p != scrollbar) {
                    Size pw = p.getWantedSize();
                    idealBreadth = Math.max(idealBreadth, pw.height);
                    idealLength += pw.width;
                }
        }

        if (scrollbar.vertical) {
            return new Size(idealBreadth, idealLength);
        } else {
            return new Size(idealLength, idealBreadth);
        }
    }

    @Override
    protected void layoutRunImpl() {
        Size r = getSize();
        int fullBreadth = scrollbar.vertical ? r.width : r.height;
        int fullLength = scrollbar.vertical ? r.height : r.width;
        scrollLength = getLengthForBreadth(fullBreadth); 

        boolean hasScrollbar = fullLength < scrollLength;
        if (hasScrollbar) {
            scrollLength = getLengthForBreadth(fullBreadth - sbSize);
            if (!layoutContainsElement(scrollbar))
                layoutAddElement(scrollbar);
            if (scrollbar.vertical) {
                scrollbar.setForcedBounds(this, new Rect(r.width - sbSize, 0, sbSize, r.height));
            } else {
                scrollbar.setForcedBounds(this, new Rect(0, r.height - sbSize, r.width, sbSize));
            }
        } else {
            if (layoutContainsElement(scrollbar))
                layoutRemoveElement(scrollbar);
        }

        if (hasScrollbar)
            scrollbar.wheelScale = (fullLength / 4.0d) / (double) scrollLength;

        layoutScrollbounds();
    }

    public void setSBSize(int size) {
        sbSize = size;
        scrollbar.setSBSize(size);
        layoutRecalculateMetrics();
    }

    public int calcScrollHeight(Size bounds) {
        return Math.max(scrollLength - (scrollbar.vertical ? bounds.height : bounds.width), 0);
    }

    // Lays out the elements with the current parameters.
    // DOES NOT add/remove scrollbar as it did previously. That was a bad idea
    // DOES update scrollLength.
    private void layoutScrollbounds() {
        lastScrollPoint = scrollbar.scrollPoint;

        Size bounds = getSize();
        int boundsLength = scrollbar.vertical ? bounds.height : bounds.width;
        int scrollHeight = calcScrollHeight(bounds);
        int appliedScrollbarSz = sbSize;

        int rY = (int) (-scrollbar.scrollPoint * scrollHeight);
        // System.out.println("scrollHeight: " + scrollHeight + "; rY: " + rY);
        if (!layoutContainsElement(scrollbar)) {
            rY = 0;
            appliedScrollbarSz = 0;
        }

        for (UIElement p : layoutGetElements()) {
            if (p == scrollbar)
                continue;
            layoutSetElementVis(p, false);
            int oRY = rY;
            int elmLength;
            if (scrollbar.vertical) {
                int breadth = bounds.width - appliedScrollbarSz;
                elmLength = p.layoutGetHForW(breadth);
                p.setForcedBounds(this, new Rect(0, rY, breadth, elmLength));
            } else {
                int breadth = bounds.height - appliedScrollbarSz;
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

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        if (earlyForceRunLayout) {
            // System.out.println("A SCROLL LAYOUT DID THE THING, IT HAS " + layoutGetElements().size() + " ELEMS AND A PARENT OF " + getParent());
            layoutRecalculateMetrics();
        } else if (lastScrollPoint != scrollbar.scrollPoint) {
            // System.out.println("DOING LAYOUT SCROLLBOUNDS DUE TO STUFF AND THINGS");
            layoutScrollbounds();
        }
        super.update(deltaTime, selected, peripherals);
    }

    // Don't even bother thinking about inner scroll views.
    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        if (!layoutContainsElement(scrollbar)) {
            // No visible scrollbar -> don't scroll
            super.handleMousewheel(x, y, north);
            return;
        }
        scrollbar.handleMousewheel(x, y, north);
    }
}
