/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IPeripherals;

import java.util.LinkedList;
import java.util.Random;

/**
 * NOTE: You have to implement your environment, and stuff like closing a window, on top of this.
 * However, this does implement the root-disconnected callback, and request-close.
 * The request-close triggers a blank method for extra post-close behavior for... reasons.
 * Created on 12/28/16. Basically rewritten on December 15th 2017.
 */
public class UITabPane extends UIElement.UIPanel {

    private final TabUtils tabManager;

    // tabOverheadHeight is the Y position of the selected window.
    // tabBarY is where the bar is in the overhead.
    protected int tabBarY;
    private int tabOverheadHeight;
    private int scrollAreaX;

    protected TabUtils.Tab selectedTab;

    private final UIScrollbar tabScroller;

    // for if no tab is selected
    private double[] currentNTState = new double[8 * 8];
    private double[] currentVOState = new double[8 * 8];
    private double[] incomingNTState = new double[8 * 8];
    private double[] incomingVOState = new double[8 * 8];
    private Random ntRandom = new Random();
    public double visualizationOrange = 0.0d;

    // Not to actually be used.
    public final int tabBarHeight;

    public UITabPane(int h, boolean csn, boolean cdt) {
        this(h, csn, cdt, 0);
    }

    public UITabPane(int h, boolean csn, boolean cdt, int scrollerSize) {
        tabManager = new TabUtils(csn, cdt, this, h);
        if (scrollerSize == 0) {
            tabScroller = null;
        } else {
            tabScroller = new UIScrollbar(false, scrollerSize);
        }
        tabBarHeight = tabManager.tabBarHeight;
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
    public void render(IGrDriver igd) {
        super.render(igd);
        Size bounds = getSize();
        tabManager.render(bounds, tabBarY, igd);
        if (selectedTab == null) {
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

    public void handleClosedUserTab(TabUtils.Tab wvWindow, boolean selfDestruct) {
        // Same reasoning as in UIWindowView: If it was manually removed, responsibility goes to remover.
        if (selfDestruct)
            wvWindow.contents.onWindowClose();
    }

    public void handleTabReorderComplete() {
        // Default behavior: override as you wish
    }


    // Used as a base for drawing.
    protected int getScrollOffsetX() {
        if (tabScroller != null)
            return -(int) (tabScroller.scrollPoint * scrollAreaX);
        return 0;
    }

    public boolean handleIncoming() {
        return tabManager.handleIncoming();
    }

    @Override
    public void runLayout() {
        Size r = getSize();
        if (tabScroller != null)
            if (layoutContainsElement(tabScroller))
                layoutRemoveElement(tabScroller);
        tabBarY = 0;
        tabManager.shortTabs = -1;
        tabOverheadHeight = tabManager.tabBarHeight;
        // IDE warning being silly again.
        int longestWidth = tabOverheadHeight;
        while (true) {
            int tl = 0;
            int longestTabName = 0;
            for (TabUtils.Tab tab : tabManager.tabs) {
                longestTabName = Math.max(tab.contents.toString().length(), longestTabName);
                tl += TabUtils.getTabWidth(tab, tabManager.shortTabs, tabManager.tabBarHeight);
            }
            int extra = 0;
            // If the user can select nothing, then add extra margin for it (!)
            if (tabManager.canSelectNone)
                extra = tabManager.tabBarHeight;
            longestWidth = Math.max(longestWidth, tl + extra);
            if ((tl + extra) <= r.width)
                break;
            if (tabScroller != null) {
                tabBarY = 0;
                int tsh = tabScroller.getWantedSize().height;
                tabOverheadHeight = tabManager.tabBarHeight + tsh;
                tabScroller.setForcedBounds(null, new Rect(0, tabManager.tabBarHeight, r.width, tsh));
                scrollAreaX = (tl + extra) - r.width;
                layoutAddElement(tabScroller);
                break;
            }
            // advance
            if (tabManager.shortTabs == -1) {
                tabManager.shortTabs = longestTabName - 1;
            } else {
                tabManager.shortTabs--;
            }
            if (tabManager.shortTabs <= 0) {
                tabManager.shortTabs = 0;
                break;
            }
        }
        if (selectedTab != null) {
            selectedTab.contents.setForcedBounds(this, new Rect(0, tabOverheadHeight, r.width, r.height - tabOverheadHeight));
            Size gws = selectedTab.contents.getWantedSize();
            setWantedSize(new Size(Math.max(longestWidth, gws.width), tabOverheadHeight + gws.height));
        } else {
            setWantedSize(new Size(longestWidth, longestWidth));
        }
    }

    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        // Please don't throw computer monitors at me for this.
        if (tabScroller != null) {
            if (y < tabOverheadHeight) {
                tabScroller.handleMousewheel(x, y, north);
                return;
            }
        }
        super.handleMousewheel(x, y, north);
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
            for (TabUtils.Tab wv : tabManager.tabs) {
                if (wv.contents == target) {
                    // verified, actually do it
                    for (UIElement uie : layoutGetElements())
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
        for (TabUtils.Tab tab : tabManager.tabs) {
            if (selectedTab == tab)
                return idx;
            idx++;
        }
        return -1;
    }

    public boolean getShortened() {
        return tabManager.shortTabs != -1;
    }

    public void addTab(TabUtils.Tab wvWindow) {
        tabManager.incomingTabs.add(wvWindow);
    }

    public void removeTab(TabUtils.Tab tab) {
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

    public LinkedList<TabUtils.Tab> getTabs() {
        LinkedList<TabUtils.Tab> wv = new LinkedList<TabUtils.Tab>();
        wv.addAll(tabManager.tabs);
        wv.addAll(tabManager.incomingTabs);
        return wv;
    }

    @Override
    public IPointerReceiver handleNewPointer(IPointer state) {
        IPointerReceiver ipr = tabManager.apply(state);
        if (ipr != null)
            return ipr;
        return super.handleNewPointer(state);
    }
}
