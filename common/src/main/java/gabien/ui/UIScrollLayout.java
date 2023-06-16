/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.wsi.IPeripherals;

/**
 * Basic scrollable layout.
 * Got moved to gabien.ui on June 9 2017 and redesigned.
 * Created on 12/29/16.
 */
public class UIScrollLayout extends UIElement.UIPanel {
    public final UIScrollbar scrollbar;
    // This is set to the scrollbar size, in full.
    private int sbSize;
    // In most cases you want this on, but sometimes you don't.
    public boolean fullWanted = true;
    // The total 'vertical' (for a vscrollbar) area that the contents cover.
    private int scrollLength = 0;
    // Doesn't control any actual scrollbar, just used to control wanted size.
    // Includes scrollbar.
    private int scrollBreadth = 0;

    private double lastScrollPoint = -1;
    private boolean earlyForceRunLayout = false;
    private boolean currentlyPerformingTriwayLayout = false;

    public UIScrollLayout(boolean vertical, int sc) {
        scrollbar = new UIScrollbar(vertical, sc);
        sbSize = sc;
    }

    public void panelsClear() {
        for (UIElement uie : layoutGetElements())
            layoutRemoveElement(uie);
        runLayoutLoop();
    }

    public void panelsAdd(UIElement uie) {
        // Store these offscreen to prevent accidental clicking.
        Size s = uie.getSize();
        uie.setForcedBounds(null, new Rect(-s.width, -s.height, s.width, s.height));
        layoutAddElement(uie);
        layoutSetElementVis(uie, false);
        earlyForceRunLayout = true;
    }

    // The reason for making runLayoutLoop overridable is because:
    // 1. You should be calling it anyway. If you are calling runLayout and you do not have a complete understanding of
    //  your element's layout, then you really, really need to switch to runLayoutLoop.
    // 2. It allows pulling off tricks like this:
    @Override
    public void runLayoutLoop() {
        if (currentlyPerformingTriwayLayout) {
            super.runLayoutLoop();
            return;
        }
        currentlyPerformingTriwayLayout = true;

        Size gs = getSize();

        // This concept is based off of how it was done for the tab bar.
        // Basically, what we need to do is try without and with scrollbar, in that order.
        // These have their own layout loop calls, which ensure the inner.
        // This mechanism allows responsive designs to completely resolve the inner UI before confirming a result to the outer UI.
        if (layoutContainsElement(scrollbar))
            layoutRemoveElement(scrollbar);
        super.runLayoutLoop();

        int screenLength = scrollbar.vertical ? gs.height : gs.width;
        if (scrollLength > screenLength) {
            if (!layoutContainsElement(scrollbar))
                layoutAddElement(scrollbar);
            super.runLayoutLoop();
        }

        currentlyPerformingTriwayLayout = false;

        // This could have side-effects needed to properly size the layout.
        if (scrollbar.vertical) {
            setWantedSize(new Size(fullWanted ? scrollBreadth : gs.width, scrollLength));
        } else {
            setWantedSize(new Size(scrollLength, fullWanted ? scrollBreadth : gs.height));
        }
    }

    @Override
    public void runLayout() {
        earlyForceRunLayout = false;
        lastScrollPoint = -1;
        Size r = getSize();

        scrollLength = 0;
        scrollBreadth = 0;

        // The UIScrollLayout here gives a scenario assuming the scrollbar is not in use.
        // What's possible is that an element or group of elements might flip between the two states,
        //  dependent on width, which is altered indirectly by height via the scrollbar's usage.
        // Now, this shouldn't be an issue so long as a greater width does not lead to a greater height.
        // If a greater width leads to a lesser height, then it stays off.
        // If a greater width leads to a greater height, then it'll loop.
        // (Interchange width/height as makes sense.)

        // The "layoutScrollbounds" at the bottom then fixes positions & allElements.

        // Since the scrollbar is about to be resized, make sure we're allowed to use it
        boolean hasScrollbar = layoutContainsElement(scrollbar);
        if (scrollbar.vertical) {
            if (hasScrollbar)
                scrollbar.setForcedBounds(this, new Rect(r.width - sbSize, 0, sbSize, r.height));
            for (UIElement p : layoutGetElements())
                if (p != scrollbar) {
                    Size pw = p.getWantedSize();
                    scrollBreadth = Math.max(scrollBreadth, pw.width);
                    scrollLength += pw.height;
                }
        } else {
            if (hasScrollbar)
                scrollbar.setForcedBounds(this, new Rect(0, r.height - sbSize, r.width, sbSize));
            for (UIElement p : layoutGetElements())
                if (p != scrollbar) {
                    Size pw = p.getWantedSize();
                    scrollBreadth = Math.max(scrollBreadth, pw.height);
                    scrollLength += pw.width;
                }
        }

        int screenLength = scrollbar.vertical ? r.height : r.width;

        if (hasScrollbar) {
            scrollbar.wheelScale = (screenLength / 4.0d) / (double) scrollLength;
            scrollBreadth += sbSize;
        }

        layoutScrollbounds();
    }

    public void setSBSize(int size) {
        sbSize = size;
        scrollbar.setSBSize(size);
        runLayoutLoop();
    }

    protected int calcScrollHeight(Size bounds) {
        return Math.max(scrollLength - (scrollbar.vertical ? bounds.height : bounds.width), 0);
    }

    // Lays out the elements with the current parameters.
    // DOES NOT add/remove scrollbar as it did previously. That was a bad idea
    // DOES update scrollLength.
    private void layoutScrollbounds() {
        if (lastScrollPoint == scrollbar.scrollPoint)
            return;
        lastScrollPoint = scrollbar.scrollPoint;

        Size bounds = getSize();
        int scrollHeight = calcScrollHeight(bounds);
        int appliedScrollbarSz = sbSize;

        int rY = (int) (-scrollbar.scrollPoint * scrollHeight);
        if (!layoutContainsElement(scrollbar)) {
            rY = 0;
            appliedScrollbarSz = 0;
        }

        for (UIElement p : layoutGetElements()) {
            if (p == scrollbar)
                continue;
            layoutSetElementVis(p, false);
            Size b = p.getWantedSize();
            int oRY = rY;
            if (scrollbar.vertical) {
                p.setForcedBounds(this, new Rect(0, rY, bounds.width - appliedScrollbarSz, b.height));
                rY += b.height;
                if (oRY <= -b.height)
                    continue;
                if (oRY >= bounds.height)
                    continue;
            } else {
                p.setForcedBounds(this, new Rect(rY, 0, b.width, bounds.height - appliedScrollbarSz));
                rY += b.width;
                if (oRY <= -b.width)
                    continue;
                if (oRY >= bounds.width)
                    continue;
            }
            layoutSetElementVis(p, true);
        }
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        if (earlyForceRunLayout) {
            runLayoutLoop();
        } else {
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
