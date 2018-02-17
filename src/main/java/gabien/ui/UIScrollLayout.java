/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;

import java.util.LinkedList;

/**
 * Basic scrollable layout.
 * Got moved to gabien.ui on June 9 2017 and redesigned.
 * Created on 12/29/16.
 */
public class UIScrollLayout extends UIElement.UIPanel {
    public final UIScrollbar scrollbar;
    private final int sbSize;
    public int scrollLength = 0;
    private double lastScrollPoint = -1;
    private boolean earlyForceRunLayout = false;

    public UIScrollLayout(boolean vertical, int sc) {
        scrollbar = new UIScrollbar(vertical, sc);
        sbSize = sc;
    }

    public void panelsClear() {
        for (UIElement uie : layoutGetElements())
            layoutRemoveElement(uie);
        runLayout();
        earlyForceRunLayout = true;
    }

    public void panelsAdd(UIElement uie) {
        layoutAddElement(uie);
        layoutSetElementVis(uie, false);
        earlyForceRunLayout = true;
    }

    // NOTE: What we do here is that we *say* we want everything, and then we take what we can get.
    @Override
    public void runLayout() {
        earlyForceRunLayout = false;
        lastScrollPoint = -1;
        Size r = getSize();
        scrollLength = 0;

        // The UIScrollLayout here gives a scenario assuming the scrollbar is not in use.
        // What's possible is that an element or group of elements might flip between the two states,
        //  dependent on width, which is altered indirectly by height via the scrollbar's usage.
        // Now, this shouldn't be an issue so long as a greater width does not lead to a greater height.
        // If a greater width leads to a lesser height, then it stays off.
        // If a greater width leads to a greater height, then it'll loop.
        // (Interchange width/height as makes sense.)

        // The "layoutScrollbounds" at the bottom then fixes positions & allElements.

        // Since the scrollbar is about to be resized, make sure we're allowed to use it
        if (!layoutContainsElement(scrollbar))
            layoutAddElement(scrollbar);
        if (scrollbar.vertical) {
            scrollbar.setForcedBounds(this, new Rect(r.width - sbSize, 0, sbSize, r.height));
            for (UIElement p : layoutGetElements())
                if (p != scrollbar)
                    scrollLength += p.getWantedSize().height;
        } else {
            scrollbar.setForcedBounds(this, new Rect(0, r.height - sbSize, r.width, sbSize));
            for (UIElement p : layoutGetElements())
                if (p != scrollbar)
                    scrollLength += p.getWantedSize().width;
        }

        layoutScrollbounds();

        if (scrollLength != 0)
            scrollbar.wheelScale = (r.height / 4.0d) / (double) scrollLength;
    }

    // Lays out the elements with the current parameters.
    private void layoutScrollbounds() {
        if (lastScrollPoint == scrollbar.scrollPoint)
            return;
        lastScrollPoint = scrollbar.scrollPoint;
        Size bounds = getSize();
        int scrollHeight = scrollLength - (scrollbar.vertical ? bounds.height : bounds.width);
        int appliedScrollbarSz = sbSize;
        if (scrollHeight <= 0) {
            scrollHeight = 0;
            // no need for the scrollbar
            appliedScrollbarSz = 0;
            if (layoutContainsElement(scrollbar))
                layoutRemoveElement(scrollbar);
        } else {
            if (!layoutContainsElement(scrollbar))
                layoutAddElement(scrollbar);
        }
        int rY = (int) (-scrollbar.scrollPoint * scrollHeight);
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
    public void update(double deltaTime) {
        if (earlyForceRunLayout) {
            runLayout();
            earlyForceRunLayout = false;
        } else {
            layoutScrollbounds();
        }
        super.update(deltaTime);
    }

    // Don't even bother thinking about inner scroll views.
    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        Size bounds = getSize();
        int scrollHeight = scrollLength - (scrollbar.vertical ? bounds.height : bounds.width);
        if (scrollHeight <= 0) {
            // No visible scrollbar -> don't scroll
            super.handleMousewheel(x, y, north);
            return;
        }
        scrollbar.handleMousewheel(x, y, north);
    }
}
