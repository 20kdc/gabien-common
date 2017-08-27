/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.IGrInDriver;
import gabien.ui.Rect;
import gabien.ui.UIElement;
import gabien.ui.UIPanel;
import gabien.ui.UIScrollbar;

import java.util.LinkedList;

/**
 * Basic scrollable layout.
 * Got moved to gabien.ui on June 9 2017 and redesigned.
 * Created on 12/29/16.
 */
public class UIScrollLayout extends UIPanel {
    public UIScrollbar scrollbar;
    public LinkedList<UIElement> panels = new LinkedList<UIElement>();
    public int scrollLength = 0;
    private double lastScrollPoint = -1;

    public UIScrollLayout(boolean vertical) {
        scrollbar = new UIScrollbar(vertical);
    }

    public void runLayout() {
        lastScrollPoint = -1;
        Rect r = getBounds();
        allElements.clear();
        allElements.add(scrollbar);
        scrollLength = 0;

        // Notably, this still has to do a setBounds for elements that are actively resizing themselves based on W
        // The "layoutScrollbounds" at the bottom then fixes positions & allElements
        if (scrollbar.vertical) {
            int sbSize = scrollbar.getBounds().width;
            scrollbar.setBounds(new Rect(r.width - sbSize, 0, sbSize, r.height));
            for (UIElement p : panels) {
                p.setBounds(new Rect(0, 0, r.width, p.getBounds().height));
                scrollLength += p.getBounds().height;
            }
        } else {
            int sbSize = scrollbar.getBounds().height;
            scrollbar.setBounds(new Rect(0, r.height - sbSize, r.width, sbSize));
            for (UIElement p : panels) {
                p.setBounds(new Rect(0, 0, p.getBounds().width, r.height));
                scrollLength += p.getBounds().width;
            }
        }

        useScissoring = true;
        layoutScrollbounds();
    }

    // Lays out the elements with the current parameters.
    private void layoutScrollbounds() {
        if (lastScrollPoint == scrollbar.scrollPoint)
            return;
        lastScrollPoint = scrollbar.scrollPoint;
        allElements.clear();
        Rect bounds = getBounds();
        int scrollHeight = scrollLength - (scrollbar.vertical ? bounds.height : bounds.width);
        int appliedScrollbarSz = scrollbar.vertical ? scrollbar.getBounds().width : scrollbar.getBounds().height;
        if (scrollHeight <= 0) {
            scrollHeight = 0;
            // no need for the scrollbar
            appliedScrollbarSz = 0;
        } else {
            allElements.add(scrollbar);
        }
        int rY = (int) (-scrollbar.scrollPoint * scrollHeight);
        for (UIElement p : panels) {
            Rect b = p.getBounds();
            int oRY = rY;
            if (scrollbar.vertical) {
                p.setBounds(new Rect(0, rY, bounds.width - appliedScrollbarSz, b.height));
                rY += b.height;
                if (oRY <= -b.height)
                    continue;
                if (oRY >= bounds.height)
                    continue;
            } else {
                p.setBounds(new Rect(rY, 0, b.width, bounds.height - appliedScrollbarSz));
                rY += b.width;
                if (oRY <= -b.width)
                    continue;
                if (oRY >= bounds.width)
                    continue;
            }
            allElements.add(p);
        }
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean select, IGrInDriver igd) {
        layoutScrollbounds();
        super.updateAndRender(ox, oy, DeltaTime, select, igd);
    }

    @Override
    public void setBounds(Rect r) {
        super.setBounds(r);
        runLayout();
    }

    // Don't even bother thinking about inner scroll views.
    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        Rect bounds = getBounds();
        int scrollHeight = scrollLength - (scrollbar.vertical ? bounds.height : bounds.width);
        if (scrollHeight <= 0) {
            // No visible scrollbar -> don't scroll
            super.handleMousewheel(x, y, north);
            return;
        }
        scrollbar.handleMousewheel(x, y, north);
    }
}
