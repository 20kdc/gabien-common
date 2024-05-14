/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.layouts;

import gabien.render.IGrDriver;
import gabien.ui.UIElement;
import gabien.ui.UILayer;
import gabien.ui.theming.Theme;
import gabien.uslx.append.Block;
import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;
import gabien.wsi.IPeripherals;

import java.util.LinkedList;

import org.eclipse.jdt.annotation.Nullable;

/**
 * NOTE: You have to implement your environment, and stuff like closing a window, on top of this.
 * However, this does implement the root-disconnected callback, and request-close.
 * The request-close triggers a blank method for extra post-close behavior for... reasons.
 * Created on 12/28/16. Basically rewritten on December 15th 2017.
 */
public class UITabPane extends UIElement.UIPanel {

    private final UIElement thbrLeft, thbrRight;
    private final UITabBar tabManager;

    // tabOverheadHeight is the Y position of the selected window.
    private int tabOverheadHeight;

    protected UITabBar.Tab selectedTab;

    public UITabPane(int h, boolean csn, boolean cdt) {
        this(h, csn, cdt, 0);
    }

    public UITabPane(int h, boolean csn, boolean cdt, int scrollerSize) {
        this(h, csn, cdt, scrollerSize, null, null);
    }

    public UITabPane(int h, boolean csn, boolean cdt, int scrollerSize, UIElement tl, UIElement tr) {
        tabManager = new UITabBar(csn, cdt, this, h, scrollerSize);
        layoutAddElement(tabManager);
        thbrLeft = tl;
        thbrRight = tr;
        if (thbrLeft != null)
            layoutAddElement(thbrLeft);
        if (thbrRight != null)
            layoutAddElement(thbrRight);
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        super.update(deltaTime, selected, peripherals);
    }

    @Override
    public void renderLayer(IGrDriver igd, UILayer layer) {
        super.renderLayer(igd, layer);
        if (layer != UILayer.Base)
            return;
        if (selectedTab == null) {
            // uuuhhh let's just scissor this just to be sure
            Size bounds = getSize();
            try (Block b = igd.openScissor(0, tabOverheadHeight, bounds.width, bounds.height - tabOverheadHeight)) {
                renderNoTabPanel(igd, 0, tabOverheadHeight, bounds.width, bounds.height - tabOverheadHeight);
            }
        }
    }

    /**
     * Here to be overridden.
     */
    public void renderNoTabPanel(IGrDriver igd, int x, int y, int w, int h) {
        Theme.B_NOTABPANEL.get(this).draw(igd, tabOverheadHeight / 2, x, y, w, h);
    }

    public void handleClosedUserTab(UITabBar.Tab wvWindow, boolean selfDestruct) {
        // Same reasoning as in UIWindowView: If it was manually removed, responsibility goes to remover.
        if (selfDestruct)
            wvWindow.contents.onWindowClose();
    }

    public void handleTabReorderComplete() {
        // Default behavior: override as you wish
    }

    public boolean handleIncoming() {
        return tabManager.handleIncoming();
    }

    private int getTMLeftMargin() {
        if (thbrLeft != null)
            return thbrLeft.getWantedSize().width;
        return 0;
    }

    private int getTMRightMargin() {
        if (thbrRight != null)
            return thbrRight.getWantedSize().width;
        return 0;
    }

    private int getTabOverheadHeightForW(int width) {
        Size thbrLeftWS = Size.ZERO;
        Size thbrRightWS = Size.ZERO;
        if (thbrLeft != null)
            thbrLeftWS = thbrLeft.getWantedSize();
        if (thbrRight != null)
            thbrRightWS = thbrRight.getWantedSize();
        int toh = tabManager.layoutGetHForW(width - (thbrLeftWS.width + thbrRightWS.width));
        return Math.max(toh, Math.max(thbrLeftWS.height, thbrRightWS.height));
    }

    @Override
    protected void layoutRunImpl() {
        Size r = getSize();

        int toh = getTabOverheadHeightForW(r.width);
        int tmLeftMargin = getTMLeftMargin();
        int tmRightMargin = getTMRightMargin();
        tabOverheadHeight = toh;

        if (thbrLeft != null)
            thbrLeft.setForcedBounds(this, new Rect(0, 0, tmLeftMargin, tabOverheadHeight));
        if (thbrRight != null)
            thbrRight.setForcedBounds(this, new Rect(r.width - tmRightMargin, 0, tmRightMargin, tabOverheadHeight));

        tabManager.setForcedBounds(this, new Rect(tmLeftMargin, 0, r.width - (tmLeftMargin + tmRightMargin), tabOverheadHeight));


        if (selectedTab != null) {
            UIElement uie = selectedTab.contents;
            uie.setForcedBounds(this, new Rect(0, tabOverheadHeight, r.width, r.height - tabOverheadHeight));
        }
    }

    @Override
    protected @Nullable Size layoutRecalculateMetricsImpl() {
        Size w = tabManager.getWantedSize();

        Size uhoh = new Size(0, 0);
        if (selectedTab != null) {
            UIElement uie = selectedTab.contents;
            uhoh = uie.getWantedSize();
        }
        return new Size(Math.max(w.width + getTMLeftMargin() + getTMRightMargin(), uhoh.width), w.height + uhoh.height);
    }

    @Override
    public int layoutGetHForW(int width) {
        int toh = getTabOverheadHeightForW(width);
        if (selectedTab != null) {
            UIElement uie = selectedTab.contents;
            return toh + uie.layoutGetHForW(width);
        }
        return toh;
    }

    public void selectTab(UIElement target) {
        if (target == null) {
            if (selectedTab != null) {
                layoutRemoveElement(selectedTab.contents);
                selectedTab = null;
            }
            return;
        }
        for (int i = 0; i < 2; i++) {
            for (UITabBar.Tab wv : tabManager.tabs) {
                if (wv.contents == target) {
                    // verified, actually do it
                    for (UIElement uie : layoutGetElements())
                        if ((uie != tabManager) && (uie != thbrLeft) && (uie != thbrRight))
                            layoutRemoveElement(uie);
                    selectedTab = wv;
                    layoutAddElement(selectedTab.contents);
                    layoutSelect(selectedTab.contents);
                    layoutRecalculateMetrics();
                    return;
                }
            }
            // If the application just set up the tabs, we might need to handle incoming early
            // Basically, prefer the chance of concurrent modification to the certainty of complete failure.
            // (Concurrent modification shouldn't really ever happen, but...)
            if (handleIncoming())
                layoutRecalculateMetrics();
        }
        throw new RuntimeException("The tab being selected was not available in this pane.");
    }

    public int getTabIndex() {
        int idx = 0;
        for (UITabBar.Tab tab : tabManager.tabs) {
            if (selectedTab == tab)
                return idx;
            idx++;
        }
        return -1;
    }

    public double getScrollPoint() {
        return tabManager.getScrollPoint();
    }
    public void setScrollPoint(double point) {
        tabManager.setScrollPoint(point);
    }

    public boolean getShortened() {
        return tabManager.shortTabs != -1;
    }

    public void addTab(UITabBar.Tab wvWindow) {
        tabManager.incomingTabs.add(wvWindow);
    }

    public void removeTab(UITabBar.Tab tab) {
        // Possible double-presence if we don't get rid of it NOW.
        // On next render of tabManager, the outgoing-tab is processed.
        // Other stuff should be prepared for this case.
        if (selectedTab != null) {
            if (selectedTab.equals(tab)) {
                layoutRemoveElement(selectedTab.contents);
                selectedTab = null;
                tabManager.findReplacementTab();
            }
        }
        tabManager.outgoingTabs.add(tab);
    }

    public LinkedList<UITabBar.Tab> getTabs() {
        LinkedList<UITabBar.Tab> wv = new LinkedList<UITabBar.Tab>();
        wv.addAll(tabManager.tabs);
        wv.addAll(tabManager.incomingTabs);
        return wv;
    }

    /**
     * From UITabBar
     */
    void tightlyCoupledLayoutRecalculateMetrics() {
        layoutRecalculateMetrics();
    }
}
