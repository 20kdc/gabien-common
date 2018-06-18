/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IPeripherals;

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
    // How many pixels difference is there between a scroll value of 0 and a scroll value of 1?
    public int scrollLength = 0;
    private double lastScrollPoint = -1;
    private boolean earlyForceRunLayout = false;

    // If nested scroll layouts or such are causing the algorithm to completely break, which I note is rare,
    //  and seems to occur cases of just-not-quite-enough-room on the 'horizontal' (for vertical SVLs),
    //  this forcefully enables scrollbars for consistency.
    private int inconsistentLayoutKillswitch = 0;

    // How much forgiveness given per-element (including for the scrollbar itself, and also a fake addition to give some initial threshold)
    // If the total forgiveness is exceeded, that's it until the next update.
    // This doesn't ensure UI display stability but at least ensures the program won't crash.
    private int inconsistentLayoutKillswitchThresholdPE = 16;

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
        boolean hadScrollbar = layoutContainsElement(scrollbar);
        if (!hadScrollbar)
            layoutAddElement(scrollbar);
        int maxA = 0;
        if (scrollbar.vertical) {
            scrollbar.setForcedBounds(this, new Rect(r.width - sbSize, 0, sbSize, r.height));
            for (UIElement p : layoutGetElements())
                if (p != scrollbar) {
                    Size pw = p.getWantedSize();
                    maxA = Math.max(maxA, pw.width);
                    scrollLength += pw.height;
                }
        } else {
            scrollbar.setForcedBounds(this, new Rect(0, r.height - sbSize, r.width, sbSize));
            for (UIElement p : layoutGetElements())
                if (p != scrollbar) {
                    Size pw = p.getWantedSize();
                    maxA = Math.max(maxA, pw.height);
                    scrollLength += pw.width;
                }
        }

        layoutScrollbounds();

        if (scrollLength != 0)
            scrollbar.wheelScale = (r.height / 4.0d) / (double) scrollLength;

        boolean hasScrollbar = layoutContainsElement(scrollbar);
        if (hasScrollbar)
            maxA += sbSize;

        // This targets elements that switch scrollbar on/off, which is the critical way in which this class can enter an infinite loop.
        // The idea is to limit this haxy half-solution to where it's needed.
        // This ensures a bare minimum of scrollbar-adds required to ensure stability.
        if (hadScrollbar != hasScrollbar)
            inconsistentLayoutKillswitch++;

        if (scrollbar.vertical) {
            setWantedSize(new Size(fullWanted ? maxA : r.width, scrollLength));
        } else {
            setWantedSize(new Size(scrollLength, fullWanted ? maxA : r.height));
        }
    }

    public void setSBSize(int size) {
        sbSize = size;
        scrollbar.setSBSize(size);
        runLayout();
    }

    // Lays out the elements with the current parameters.
    private void layoutScrollbounds() {
        boolean inconsistentLayoutKillswitchLocked = inconsistentLayoutKillswitch > (inconsistentLayoutKillswitchThresholdPE * (layoutGetElements().size() + 1));

        if (lastScrollPoint == scrollbar.scrollPoint)
            return;
        lastScrollPoint = scrollbar.scrollPoint;
        Size bounds = getSize();
        int scrollHeight = scrollLength - (scrollbar.vertical ? bounds.height : bounds.width);
        int appliedScrollbarSz = sbSize;
        if ((scrollHeight <= 0) && !inconsistentLayoutKillswitchLocked) {
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
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        inconsistentLayoutKillswitch = 0;
        if (earlyForceRunLayout) {
            runLayout();
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
