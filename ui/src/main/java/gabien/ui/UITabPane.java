/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.render.IGrDriver;
import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;
import gabien.wsi.IPeripherals;

import java.util.LinkedList;
import java.util.Random;

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

    // for if no tab is selected
    private double[] currentNTState = new double[8 * 8];
    private double[] currentVOState = new double[8 * 8];
    private double[] incomingNTState = new double[8 * 8];
    private double[] incomingVOState = new double[8 * 8];
    private Random ntRandom = new Random();
    public double visualizationOrange = 0.0d;

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
        if (selectedTab == null) {
            for (int i = 0; i < currentNTState.length; i++) {
                double delta = deltaTime / 4.0d;
                if (currentVOState[i] < incomingVOState[i]) {
                    currentVOState[i] = Math.min(currentVOState[i] + delta, incomingVOState[i]);
                } else {
                    currentVOState[i] = Math.max(currentVOState[i] - delta, incomingVOState[i]);
                }
                if (currentNTState[i] < incomingNTState[i]) {
                    currentNTState[i] = Math.min(currentNTState[i] + delta, incomingNTState[i]);
                } else {
                    currentNTState[i] = Math.max(currentNTState[i] - delta, incomingNTState[i]);
                }
                if (Math.abs(currentNTState[i] - incomingNTState[i]) < 0.05d)
                    incomingNTState[i] = (ntRandom.nextDouble() / 2.0d) + 0.5d;
                if (Math.abs(currentVOState[i] - incomingVOState[i]) < 0.05d) {
                    double d = (i + 1) - ((1 - visualizationOrange) * currentNTState.length);
                    incomingVOState[i] = Math.max(0, Math.min(1, d));
                }
            }
        }
    }

    @Override
    public void renderLayer(IGrDriver igd, UILayer layer) {
        super.renderLayer(igd, layer);
        if (layer != UILayer.Content)
            return;
        if (selectedTab == null) {
            Size bounds = getSize();
            int w = bounds.width / 8;
            int h = (bounds.height - tabOverheadHeight) / 8;
            int lw = bounds.width - (w * 7);
            int lh = (bounds.height - tabOverheadHeight) - (h * 7);
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    double nt = currentNTState[i + (j * 8)];
                    double vo = currentVOState[i + (j * 8)];
                    double vDO = nt * vo;
                    double vDB = nt * (1 - vo);
                    int vR = (int) (vDO * 255);
                    int vG = (int) (nt * 128);
                    int vB = (int) (vDB * 255);
                    igd.clearRect(vR, vG, vB, i * w, (j * h) + tabOverheadHeight, i == 7 ? lw : w, j == 7 ? lh : h);
                }
            }
        }
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

    @Override
    public void runLayout() {
        Size r = getSize();
        Size w = tabManager.getWantedSize();
        tabOverheadHeight = w.height;

        int tmLeftMargin = 0;
        int tmRightMargin = 0;
        if (thbrLeft != null) {
            Size sz = thbrLeft.getWantedSize();
            tmLeftMargin = sz.width;
            tabOverheadHeight = Math.max(tabOverheadHeight, sz.height);
        }
        if (thbrRight != null) {
            Size sz = thbrRight.getWantedSize();
            tmRightMargin = sz.width;
            tabOverheadHeight = Math.max(tabOverheadHeight, sz.height);
        }
        if (thbrLeft != null)
            thbrLeft.setForcedBounds(this, new Rect(0, 0, tmLeftMargin, tabOverheadHeight));
        if (thbrRight != null)
            thbrRight.setForcedBounds(this, new Rect(r.width - tmRightMargin, 0, tmRightMargin, tabOverheadHeight));

        tabManager.setForcedBounds(this, new Rect(tmLeftMargin, 0, r.width - (tmLeftMargin + tmRightMargin), tabOverheadHeight));


        Size uhoh = new Size(0, 0);
        if (selectedTab != null) {
            UIElement uie = selectedTab.contents;
            uhoh = uie.getWantedSize();
            uie.setForcedBounds(this, new Rect(0, tabOverheadHeight, r.width, r.height - tabOverheadHeight));
        }
        setWantedSize(new Size(Math.max(w.width, uhoh.width), w.height + uhoh.height));
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
                    runLayout();
                    return;
                }
            }
            // If the application just set up the tabs, we might need to handle incoming early
            // Basically, prefer the chance of concurrent modification to the certainty of complete failure.
            // (Concurrent modification shouldn't really ever happen, but...)
            if (handleIncoming())
                runLayout();
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
}
