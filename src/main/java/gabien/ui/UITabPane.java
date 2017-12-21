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

    private final int tabTextHeight;
    // This is used as the basis for calculations.
    public final int tabBarHeight;
    private final int padding;

    public boolean currentClickOnBar = false;
    public int currentClickTabOfsX = 0;
    // If this is false, the tab pane should try and actively avoid situations in which no tab is selected by choosing the first tab.
    public final boolean canSelectNone;
    public final boolean canDragTabs;
    // -1 means not shortened, otherwise it's max length.
    private int shortTabs = -1;

    // width margin handling
    private final int tabExMargin;
    private final int tabIcoMargin;

    // for if no tab is selected
    private double[] currentNTState = new double[8 * 8];
    private double[] incomingNTState = new double[8 * 8];
    private Random ntRandom = new Random();
    public double visualizationOrange = 0.0d;

    public UITabPane(int h, boolean csn, boolean cdt) {
        canSelectNone = csn;
        canDragTabs = cdt;
        tabTextHeight = h;
        useScissoring = true;
        padding = ((h + 7) / 8);
        tabBarHeight = tabTextHeight + padding + 2; // +2 for additional border
        tabExMargin = h / 8;
        tabIcoMargin = h / 4;
    }

    @Override
    public void setBounds(Rect r) {
        // The reason that it checks if the selected element is about to be closed is because this implies double-presence.
        // Double-presence is fine, but must be carefully controlled.
        if (selectedElement != null)
            if (!outgoingTabs.contains(selectedElement))
                selectedElement.setBounds(new Rect(0, tabBarHeight, r.width, r.height - tabBarHeight));
        super.setBounds(r);
        updateShortTabs();
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean select, IGrInDriver igd) {
        boolean willUpdateLater = handleIncoming();
        super.updateAndRender(ox, oy, DeltaTime, select, igd);
        Rect bounds = getBounds();
        igd.clearRect(16, 16, 16, ox, oy, bounds.width, tabBarHeight);
        // There would be "oy + 1" instead of just "oy", but ofc it has to be centred, so it would just be -1'd again...
        igd.clearRect(32, 32, 32, ox, oy + ((tabTextHeight + padding) / 2), bounds.width, 2);

        LinkedList<UIWindowView.WVWindow> outgoing2 = new LinkedList<UIWindowView.WVWindow>();
        HashSet<UIElement> outgoingTabs2 = outgoingTabs;
        outgoingTabs = new HashSet<UIElement>();
        for (int pass = 0; pass < ((currentClickOnBar && canDragTabs) ? 2 : 1); pass++) {
            int pos = 0;
            boolean toggle = false;
            for (UIWindowView.WVWindow w : tabs) {
                if (outgoingTabs2.contains(w.contents)) {
                    willUpdateLater = true;
                    outgoing2.add(w);
                }
                // This is used for all rendering.
                int theDisplayOX = ox + pos;
                int tabW = getTabWidth(w, shortTabs);
                int base = toggle ? 64 : 32;
                if (selectedElement == w.contents)
                    base = 128;
                toggle = !toggle;

                // Decide against rendering
                boolean shouldRender = true;
                if (pass == 0) {
                    if (currentClickOnBar && canDragTabs)
                        if (selectedElement == w.contents)
                            shouldRender = false;
                } else {
                    if (selectedElement != w.contents)
                        shouldRender = false;
                    theDisplayOX = igd.getMouseX() - currentClickTabOfsX;
                }
                if (!shouldRender) {
                    pos += tabW;
                    continue;
                }

                int margin = tabTextHeight / 6;
                igd.clearRect(base, base, base, theDisplayOX, oy, tabW, tabBarHeight);
                // use a margin to try and still provide a high-contrast display despite the usability 'improvements' making the tabs brighter supposedly provides
                igd.clearRect(base / 2, base / 2, base / 2, theDisplayOX + margin, oy + margin, tabW - (margin * 2), (tabTextHeight + padding + 2) - (margin * 2));

                UILabel.drawString(igd, theDisplayOX + tabExMargin, oy + 1 + padding, getVisibleTabName(w, shortTabs), true, tabTextHeight);

                int icoBack = tabBarHeight;
                for (UIWindowView.IWVWindowIcon i : w.icons) {
                    // sometimes too bright, deal with that
                    int size = tabBarHeight - (tabIcoMargin * 2);
                    int subMargin = tabIcoMargin / 2;
                    igd.clearRect(0, 0, 0, theDisplayOX + tabW - ((icoBack - tabIcoMargin) + subMargin), oy + tabIcoMargin - subMargin, size + (subMargin * 2), size + (subMargin * 2));

                    i.draw(igd, theDisplayOX + tabW - (icoBack - tabIcoMargin), oy + tabIcoMargin, size);
                    icoBack += tabBarHeight;
                }

                pos += tabW;
            }
        }
        if (outgoingTabs2.contains(selectedElement)) {
            if (canSelectNone) {
                selectTab(null);
            } else if (tabs.size() > 0) {
                selectTab(tabs.getFirst().contents);
            } else {
                selectTab(null);
            }
        }
        tabs.removeAll(outgoing2);
        if (willUpdateLater)
            updateShortTabs();

        if (selectedElement == null) {
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

    private boolean handleIncoming() {
        if (incomingTabs.size() > 0) {
            tabs.addAll(incomingTabs);
            if (selectedElement == null) {
                allElements.clear();
                selectedElement = incomingTabs.getFirst().contents;
                allElements.add(selectedElement);
                setBounds(getBounds());
            }
            incomingTabs.clear();
            return true;
        }
        return false;
    }

    private String getVisibleTabName(UIWindowView.WVWindow w, int shortTab) {
        String name = w.contents.toString();
        if (shortTab != -1)
            return name.substring(0, Math.min(name.length(), shortTab));
        return name;
    }

    private int getTabWidth(UIWindowView.WVWindow window, int shortTab) {
        return UILabel.getTextLength(getVisibleTabName(window, shortTab), tabTextHeight) + (tabExMargin * 2) + (tabBarHeight * window.icons.length);
    }

    @Override
    public void handleClick(int x, int y, int button) {
        if (y < tabBarHeight) {
            currentClickOnBar = true;
            currentClickTabOfsX = 0;
            int pos = 0;
            for (UIWindowView.WVWindow w : tabs) {
                if (w.contents == selectedElement)
                    currentClickTabOfsX = x - pos;
                int oldPos = pos;
                pos += getTabWidth(w, shortTabs);
                if (x < pos) {
                    int icoBack = tabBarHeight;
                    for (UIWindowView.IWVWindowIcon i : w.icons) {
                        // sometimes too bright, deal with that
                        int size = tabBarHeight - (tabIcoMargin * 2);
                        Rect rc = new Rect(pos - (icoBack - tabIcoMargin), tabIcoMargin, size, size);
                        if (rc.contains(x, y)) {
                            i.click();
                            return;
                        }
                        icoBack += tabBarHeight;
                    }
                    currentClickTabOfsX = x - oldPos;
                    selectTab(w.contents);
                    return;
                }
            }
            if (canSelectNone)
                selectTab(null);
        } else {
            currentClickOnBar = false;
            if (selectedElement != null) {
                selectedElement.handleClick(x, y - tabBarHeight, button);
            } else {
                Rect bounds = getBounds();
                // slight interactivity w/ NT
                int w = bounds.width / 8;
                int h = (bounds.height - tabBarHeight) / 8;
                int tX = x / w;
                int tY = y / h;
                if (tX == 8)
                    tX--;
                if (tY == 8)
                    tY--;
                incomingNTState[tX + (tY * 8)] = 1;
            }
        }
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

    private void tabReorderer(int x) {
        // Oscillates if a tab that's being nudged left to make way moves out of range.
        // Since these are always two-frame oscillations, just deal with it the simple way...
        for (int pass = 0; pass < 2; pass++) {
            // Used to reorder tabs
            int expectedIndex = -1;
            int selectedIndex = -1;
            int pos = 0;
            int i = 0;
            for (UIWindowView.WVWindow w : tabs) {
                int tabW = getTabWidth(w, shortTabs);
                pos += tabW;
                if (x < pos)
                    if (expectedIndex == -1)
                        expectedIndex = i;
                if (w.contents == selectedElement)
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
            selectedElement = null;
            allElements.clear();
            return;
        }
        for (int i = 0; i < 2; i++) {
            for (UIWindowView.WVWindow wv : tabs) {
                if (wv.contents.equals(target)) {
                    // verified, actually do it
                    allElements.clear();
                    allElements.add(wv.contents);
                    selectedElement = wv.contents;
                    setBounds(getBounds());
                    return;
                }
            }
            // It'll throw an exception if something is not done immediately,
            //  and this is usually called after setting up the tabs.
            if (handleIncoming())
                updateShortTabs();
        }
        throw new RuntimeException("The tab being selected was not available in this pane.");
    }

    private void updateShortTabs() {
        shortTabs = -1;
        while (true) {
            int tl = 0;
            int longestTabName = 0;
            for (UIWindowView.WVWindow tab : tabs) {
                longestTabName = Math.max(tab.contents.toString().length(), longestTabName);
                tl += getTabWidth(tab, shortTabs);
            }
            if (tl <= getBounds().width)
                return;
            // advance
            if (shortTabs == -1) {
                shortTabs = longestTabName - 1;
            } else {
                shortTabs--;
            }
            if (shortTabs <= 0) {
                shortTabs = 0;
                return;
            }
        }
    }

    public int getTabIndex() {
        int idx = 0;
        for (UIWindowView.WVWindow tab : tabs) {
            if (selectedElement == tab.contents)
                return idx;
            idx++;
        }
        return idx;
    }

    public void addTab(UIWindowView.WVWindow wvWindow) {
        incomingTabs.add(wvWindow);
    }

    public void removeTab(UIElement uiElement) {
        outgoingTabs.add(uiElement);
    }
}
