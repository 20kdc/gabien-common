/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

/**
 * NOTE: The layout of this keeps in mind that 1/8th of the text height is spacing (rounding up)
 * Created on 12/28/16. Basically rewritten on December 15th 2017.
 */
public class UITabPane extends UIPanel {
    private LinkedList<UIWindowView.WVWindow> tabs = new LinkedList<UIWindowView.WVWindow>();
    private LinkedList<UIWindowView.WVWindow> incomingTabs = new LinkedList<UIWindowView.WVWindow>();
    private HashSet<UIElement> outgoingTabs = new HashSet<UIElement>();

    // This is used as the basis for calculations.
    public final int tabBarHeight;

    public UIWindowView.WVWindow selectedTab;

    // tabOverheadHeight is the Y position of the selected window.
    // tabBarY is where the bar is in the overhead.
    private int tabBarY, tabOverheadHeight, scrollAreaX;

    private final UIScrollbar tabScroller;

    public boolean currentClickOnBar = false;
    public int currentClickTabOfsX = 0;
    // If this is false, the tab pane should try and actively avoid situations in which no tab is selected by choosing the first tab.
    // That said, it's not impossible the calling application could force the pane into a situation where no tab is selected...
    public final boolean canSelectNone;
    public final boolean canDragTabs;
    // -1 means not shortened, otherwise it's max length.
    private int shortTabs = -1;

    // for if no tab is selected
    private double[] currentNTState = new double[8 * 8];
    private double[] incomingNTState = new double[8 * 8];
    private Random ntRandom = new Random();
    public double visualizationOrange = 0.0d;

    public UITabPane(int h, boolean csn, boolean cdt) {
        this(h, csn, cdt, 0);
    }

    public UITabPane(int h, boolean csn, boolean cdt, int scrollerSize) {
        canSelectNone = csn;
        canDragTabs = cdt;
        useScissoring = true;
        tabBarHeight = TabUtils.getHeight(h);
        if (scrollerSize == 0) {
            tabScroller = null;
        } else {
            tabScroller = new UIScrollbar(false, scrollerSize);
        }
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean select, IGrInDriver igd) {
        boolean willUpdateLater = handleIncoming();
        super.updateAndRender(ox, oy, DeltaTime, select, igd);
        Rect bounds = getBounds();
        igd.clearRect(16, 16, 16, ox, oy, bounds.width, tabBarHeight);
        igd.clearRect(32, 32, 32, ox, oy + ((tabBarHeight / 2) - 1), bounds.width, 2);

        LinkedList<UIWindowView.WVWindow> outgoing2 = new LinkedList<UIWindowView.WVWindow>();
        HashSet<UIElement> outgoingTabs2 = outgoingTabs;
        outgoingTabs = new HashSet<UIElement>();
        for (int pass = 0; pass < ((currentClickOnBar && canDragTabs) ? 2 : 1); pass++) {
            int pos = getScrollOffsetX();
            boolean toggle = false;
            for (UIWindowView.WVWindow w : tabs) {
                if (outgoingTabs2.contains(w.contents)) {
                    willUpdateLater = true;
                    outgoing2.add(w);
                }
                // This is used for all rendering.
                int theDisplayOX = ox + pos;
                int tabW = TabUtils.getTabWidth(w, shortTabs, tabBarHeight);
                int base = toggle ? 64 : 32;
                if (selectedTab == w)
                    base = 128;
                toggle = !toggle;

                // Decide against rendering
                boolean shouldRender = true;
                if (pass == 0) {
                    if (currentClickOnBar && canDragTabs)
                        if (selectedTab == w)
                            shouldRender = false;
                } else {
                    if (selectedTab != w)
                        shouldRender = false;
                    theDisplayOX = igd.getMouseX() - currentClickTabOfsX;
                }
                if (!shouldRender) {
                    pos += tabW;
                    continue;
                }

                TabUtils.drawTab(base, base / 2, theDisplayOX, oy + tabBarY, tabW, tabBarHeight, igd, TabUtils.getVisibleTabName(w, shortTabs), w.icons);

                pos += tabW;
            }
        }
        if (selectedTab != null) {
            if (outgoingTabs2.contains(selectedTab.contents)) {
                if (canSelectNone) {
                    selectTab(null);
                } else if (tabs.size() > 0) {
                    selectTab(tabs.getFirst().contents);
                } else {
                    selectTab(null);
                }
            }
        }
        tabs.removeAll(outgoing2);
        if (willUpdateLater)
            setBounds(getBounds());

        if (selectedTab == null) {
            for (int i = 0; i < currentNTState.length; i++) {
                double delta = DeltaTime / 4.0d;
                if (currentNTState[i] < incomingNTState[i]) {
                    currentNTState[i] = Math.min(currentNTState[i] + delta, incomingNTState[i]);
                } else {
                    currentNTState[i] = Math.max(currentNTState[i] - delta, incomingNTState[i]);
                }
                if (Math.abs(currentNTState[i] - incomingNTState[i]) < 0.05d) {
                    incomingNTState[i] = (ntRandom.nextDouble() / 2.0d) + 0.5d;
                    if (ntRandom.nextDouble() < visualizationOrange)
                        incomingNTState[i] = -incomingNTState[i];
                }
            }

            int w = bounds.width / 8;
            int h = (bounds.height - tabBarHeight) / 8;
            int lw = bounds.width - (w * 7);
            int lh = (bounds.height - tabBarHeight) - (h * 7);
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    int v = (int) (currentNTState[i + (j * 8)] * 255);
                    if (v != 0) {
                        if (v > 0) {
                            igd.clearRect(0, v / 2, v, ox + (i * w), oy + (j * h) + tabBarHeight, i == 7 ? lw : w, j == 7 ? lh : h);
                        } else {
                            igd.clearRect(-v, -v / 2, 0, ox + (i * w), oy + (j * h) + tabBarHeight, i == 7 ? lw : w, j == 7 ? lh : h);
                        }
                    }
                }
            }
        }
    }

    // Used as a base for drawing.
    private int getScrollOffsetX() {
        if (tabScroller != null)
            return -(int) (tabScroller.scrollPoint * scrollAreaX);
        return 0;
    }

    public boolean handleIncoming() {
        if (incomingTabs.size() > 0) {
            tabs.addAll(incomingTabs);
            if (selectedTab == null) {
                allElements.clear();
                selectedTab = incomingTabs.getFirst();
                selectedElement = selectedTab.contents;
                allElements.add(selectedTab.contents);
                setBounds(getBounds());
            }
            incomingTabs.clear();
            return true;
        }
        return false;
    }

    @Override
    public void handleClick(int x, int y, int button) {
        currentClickOnBar = false;
        if (y < (tabBarY + tabBarHeight)) {
            if (y >= tabBarY) {
                currentClickOnBar = true;
                currentClickTabOfsX = 0;
                int pos = getScrollOffsetX();
                for (UIWindowView.WVWindow w : tabs) {
                    if (selectedTab == w)
                        currentClickTabOfsX = x - pos;
                    int tabW = TabUtils.getTabWidth(w, shortTabs, tabBarHeight);
                    if (x < (pos + tabW)) {
                        if (TabUtils.clickInTab(w, x - pos, y - tabBarY, tabW, tabBarHeight))
                            return;
                        currentClickTabOfsX = x - pos;
                        selectTab(w.contents);
                        return;
                    }
                    pos += tabW;
                }
                if (canSelectNone)
                    selectTab(null);
                return;
            }
        }
        super.handleClick(x, y, button);
    }

    @Override
    public void handleDrag(int x, int y) {
        if (currentClickOnBar) {
            if (canDragTabs)
                tabReorderer(x);
            return;
        }
        super.handleDrag(x, y);
    }

    @Override
    public void handleRelease(int x, int y) {
        if (currentClickOnBar) {
            if (canDragTabs)
                tabReorderer(x);
            // currentClickOnBar + canDragTabs = tab stuck to mouse
            currentClickOnBar = false;
            return;
        }
        super.handleRelease(x, y);
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

    private void tabReorderer(int x) {
        // Oscillates if a tab that's being nudged left to make way moves out of range.
        // Since these are always two-frame oscillations, just deal with it the simple way...
        for (int pass = 0; pass < 2; pass++) {
            // Used to reorder tabs
            int expectedIndex = -1;
            int selectedIndex = -1;
            int pos = getScrollOffsetX();
            int i = 0;
            for (UIWindowView.WVWindow w : tabs) {
                int tabW = TabUtils.getTabWidth(w, shortTabs, tabBarHeight);
                pos += tabW;
                if (x < pos)
                    if (expectedIndex == -1)
                        expectedIndex = i;
                if (selectedTab == w)
                    selectedIndex = i;
                i++;
            }
            if (expectedIndex == -1)
                expectedIndex = tabs.size() - 1;
            if (selectedIndex == -1)
                return;
            UIWindowView.WVWindow w = tabs.remove(selectedIndex);
            tabs.add(expectedIndex, w);
        }
    }

    public void selectTab(UIElement target) {
        if (target == null) {
            if (selectedTab != null) {
                allElements.remove(selectedTab.contents);
                selectedTab = null;
                selectedElement = null;
            }
            return;
        }
        for (int i = 0; i < 2; i++) {
            for (UIWindowView.WVWindow wv : tabs) {
                if (wv.contents.equals(target)) {
                    // verified, actually do it
                    allElements.clear();
                    allElements.add(wv.contents);
                    selectedTab = wv;
                    selectedElement = wv.contents;
                    setBounds(getBounds());
                    return;
                }
            }
            // If the application just set up the tabs, we might need to handle incoming early
            // Basically, prefer the chance of concurrent modification to the certainty of complete failure.
            // (Concurrent modification shouldn't really ever happen, but...)
            if (handleIncoming())
                setBounds(getBounds());
        }
        throw new RuntimeException("The tab being selected was not available in this pane.");
    }


    @Override
    public void setBounds(Rect r) {
        super.setBounds(r);
        shortTabs = -1;
        if (tabScroller != null)
            allElements.remove(tabScroller);
        tabBarY = 0;
        tabOverheadHeight = tabBarHeight;
        while (true) {
            int tl = 0;
            int longestTabName = 0;
            for (UIWindowView.WVWindow tab : tabs) {
                longestTabName = Math.max(tab.contents.toString().length(), longestTabName);
                tl += TabUtils.getTabWidth(tab, shortTabs, tabBarHeight);
            }
            int extra = 0;
            // If the user can select nothing, then add extra margin for it (!)
            if (canSelectNone)
                extra = tabBarHeight;
            if ((tl + extra) <= r.width)
                break;
            if (tabScroller != null) {
                tabBarY = 0;
                int tsh = tabScroller.getBounds().height;
                tabOverheadHeight = tabBarHeight + tsh;
                tabScroller.setBounds(new Rect(0, tabBarHeight, r.width, tsh));
                scrollAreaX = (tl + extra) - r.width;
                allElements.add(tabScroller);
                break;
            }
            // advance
            if (shortTabs == -1) {
                shortTabs = longestTabName - 1;
            } else {
                shortTabs--;
            }
            if (shortTabs <= 0) {
                shortTabs = 0;
                break;
            }
        }
        // The reason that it checks if the selected element is about to be closed is because this implies double-presence.
        // Double-presence is fine, but must be carefully controlled.
        // Another note is that this is the only place where the selectedTab bounds are setup.
        // Other stuff goes through this via a setBounds(getBounds()) call, to simplify the logic.
        if (selectedTab != null)
            if (!outgoingTabs.contains(selectedTab.contents))
                selectedTab.contents.setBounds(new Rect(0, tabOverheadHeight, r.width, r.height - tabOverheadHeight));
    }

    public int getTabIndex() {
        int idx = 0;
        for (UIWindowView.WVWindow tab : tabs) {
            if (selectedTab == tab)
                return idx;
            idx++;
        }
        return -1;
    }

    public boolean getShortened() {
        return shortTabs != -1;
    }

    public void addTab(UIWindowView.WVWindow wvWindow) {
        incomingTabs.add(wvWindow);
    }

    public void removeTab(UIElement uiElement) {
        outgoingTabs.add(uiElement);
    }
}
