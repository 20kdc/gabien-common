/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.layouts;

import gabien.render.IGrDriver;
import gabien.text.TextTools;
import gabien.ui.FontManager;
import gabien.ui.IPointerReceiver;
import gabien.ui.UIElement;
import gabien.ui.UILayer;
import gabien.ui.elements.UIBorderedElement;
import gabien.ui.elements.UIScrollbar;
import gabien.ui.theming.IBorder;
import gabien.ui.theming.Theme;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.append.*;
import gabien.wsi.IPointer;

/**
 * Because frankly, the previous tab/window code sucked.
 * Static methods assist UIWindowView - the actual instance form is for UITabPane.
 *
 * Created on February 13th 2018.
 * A UI element as of February 4th 2019.
 */
public class UITabBar extends UIElement.UIPanel {
    public LinkedList<Tab> tabs = new LinkedList<Tab>();
    public LinkedList<Tab> incomingTabs = new LinkedList<Tab>();

    public HashSet<Tab> outgoingTabs = new HashSet<Tab>();

    public final UITabPane parentView;
    private final UIScrollbar tabScroller;

    // If this is false, the tab pane should try and actively avoid situations in which no tab is selected by choosing the first tab.
    // That said, it's not impossible the calling application could force the pane into a situation where no tab is selected...
    public final boolean canSelectNone;
    public final boolean canDragTabs;

    // -1 means not shortened, otherwise it's max length.
    protected int shortTabs = -1;

    private final int wantedHeight;
    private int effectiveHeight, fullWantedHeight;
    private int scrollAreaX;

    // If true when draggingTabs reaches zero elements, the parent view tabReorderComplete function is called.
    private boolean tabReorderDidSomething = false;
    public WeakHashMap<Tab, IPointerReceiver.RelativeResizePointerReceiver> draggingTabs = new WeakHashMap<Tab, IPointerReceiver.RelativeResizePointerReceiver>();

    public UITabBar(boolean selectNone, boolean canDrag, UITabPane par, int h, int scrollerSize) {
        canSelectNone = selectNone;
        canDragTabs = canDrag;
        parentView = par;
        wantedHeight = getHeight(h);
        if (scrollerSize == 0) {
            tabScroller = null;
        } else {
            tabScroller = new UIScrollbar(false, scrollerSize);
        }
    }

    @Override
    public void renderLayer(IGrDriver igd, UILayer layer) {
        super.renderLayer(igd, layer);
        Theme theme = getTheme();
        if (layer == UILayer.Base) {
            Size bounds = getSize();
            UIBorderedElement.drawBorder(theme, igd, Theme.B_TABA, 0, 0, 0, bounds.width, effectiveHeight);
        }
        if (layer == UILayer.Content) {
            boolean willUpdateLater = parentView.handleIncoming();

            HashSet<Tab> outgoingTabs2 = outgoingTabs;
            outgoingTabs = new HashSet<Tab>();
            for (Tab w : tabs) {
                boolean reqU = w.contents.requestsUnparenting();
                if (outgoingTabs2.contains(w) || reqU) {
                    willUpdateLater = true;
                    outgoingTabs2.add(w);
                    parentView.handleClosedUserTab(w, reqU);
                }
            }
            if (parentView.selectedTab != null)
                if (outgoingTabs2.contains(parentView.selectedTab))
                    findReplacementTab();
            tabs.removeAll(outgoingTabs2);

            if (willUpdateLater)
                parentView.tightlyCoupledLayoutRecalculateMetrics();
        }
        if (layer == UILayer.Base)
            renderTabPass(theme, igd, false, true, false);
        if (layer == UILayer.Content) {
            renderTabPass(theme, igd, false, false, true);
            if ((draggingTabs.size() > 0) && canDragTabs)
                renderTabPass(theme, igd, true, true, true);
        }
    }
    private void renderTabPass(Theme theme, IGrDriver igd, boolean isRenderingDraggedTabs, boolean enBack, boolean enFore) {
        int pos = getScrollOffsetX();
        boolean toggle = false;
        FontManager fm = Theme.FM_GLOBAL.get(theme);
        for (Tab w : tabs) {
            // This is used for all rendering.
            int theDisplayOX = pos;
            int tabW = getTabWidth(fm, w, shortTabs, effectiveHeight);
            Theme.Attr<IBorder> base = toggle ? Theme.B_TABB : Theme.B_TABA;
            if (parentView.selectedTab == w)
                base = Theme.B_TABSEL;
            toggle = !toggle;

            // Decide against rendering
            boolean shouldRender = true;
            if (!isRenderingDraggedTabs) {
                if (draggingTabs.containsKey(w))
                    shouldRender = false;
            } else {
                IPointerReceiver.RelativeResizePointerReceiver rrpr = draggingTabs.get(w);
                if (rrpr != null) {
                    theDisplayOX = rrpr.lastSize.width;
                } else {
                    shouldRender = false;
                }
            }
            if (!shouldRender) {
                pos += tabW;
                continue;
            }

            if (UIBorderedElement.getMoveDownFlag(theme, base)) {
                float[] trs = igd.getTRS();
                int[] scissor = igd.getScissor();
                float oldTY = igd.trsTYS(effectiveHeight / 8);
                int oldCD = scissor[3];
                scissor[3] = (int) Math.min(scissor[3], oldTY + (effectiveHeight * trs[3]));
                drawTab(theme, base, theDisplayOX, 0, tabW, effectiveHeight, igd, getVisibleTabName(w, shortTabs), w, enBack, enFore);
                igd.trsTYE(oldTY);
                scissor[3] = oldCD;
            } else {
                drawTab(theme, base, theDisplayOX, 0, tabW, effectiveHeight, igd, getVisibleTabName(w, shortTabs), w, enBack, enFore);
            }

            pos += tabW;
        }
    }

    private int calculateTabBarWidth(int atHeight) {
        FontManager fm = Theme.FM_GLOBAL.get(this);
        int tl = 0;
        for (UITabBar.Tab tab : tabs)
            tl += getTabWidth(fm, tab, shortTabs, atHeight);
        int extra = canSelectNone ? wantedHeight : 0;
        return tl + extra;
    }

    private int calculateLongestTabName() {
        int longestTabName = 0;
        for (UITabBar.Tab tab : tabs)
            longestTabName = Math.max(tab.contents.toString().length(), longestTabName);
        return longestTabName;
    }

    @Override
    protected void layoutRunImpl() {
        Size bounds = getSize();

        for (int pass = ((tabScroller == null) ? 1 : 0); pass < ((tabScroller == null) ? 2 : 3); pass++) {
            // Pass 0: With scrollbar
            // Pass 1 (if sufficent room estimated, or if no scrollbar): Without scrollbar
            // Pass 2 (if pass 1 fail): Restore scrollbar
            boolean thisPassHasScrollbar = pass != 1;

            fullWantedHeight = wantedHeight;

            effectiveHeight = bounds.height;
            if (thisPassHasScrollbar) {
                // tabScroller can't be null in a situation where passes other than 1 are run.
                if (!layoutContainsElement(tabScroller))
                    layoutAddElement(tabScroller);

                int tsh = tabScroller.getWantedSize().height;

                effectiveHeight -= tsh;
                fullWantedHeight += tsh;

                tabScroller.setForcedBounds(this, new Rect(0, effectiveHeight, bounds.width, tsh));
            } else {
                if (tabScroller != null)
                    if (layoutContainsElement(tabScroller))
                        layoutRemoveElement(tabScroller);
            }

            shortTabs = -1;
            int longestWidth = calculateTabBarWidth(effectiveHeight);
            int eScrollAreaX = Math.max(0, longestWidth - bounds.width);
            if (thisPassHasScrollbar) {
                scrollAreaX = eScrollAreaX;
            } else {
                scrollAreaX = 0;
            }
            if (tabScroller == null) {
                int lastWidth = longestWidth;
                int longestTabName = calculateLongestTabName();
                while (lastWidth > bounds.width) {
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
                    lastWidth = calculateTabBarWidth(effectiveHeight);
                }
            }

            // Implement pass sufficiency rules
            if (pass == 0) {
                // Scroll area > 0: scrollbar definitely needed, break now as one is setup now
                // otherwise, scrollbar may not be needed so do no scrollbar pass
                if (eScrollAreaX > 0)
                    break;
            } else if (pass == 1) {
                // Scroll area == 0: everything contained properly, break now
                // otherwise, scrollbar needed so do final scrollbar pass
                if (eScrollAreaX == 0)
                    break;
            }
        }
    }

    @Override
    public int layoutGetHForW(int width) {
        if (tabScroller != null)
            if (width < calculateTabBarWidth(wantedHeight))
                return wantedHeight + tabScroller.getWantedSize().height;
        return wantedHeight;
    }

    @Override
    protected @Nullable Size layoutRecalculateMetricsImpl() {
        return new Size(calculateTabBarWidth(wantedHeight), wantedHeight);
    }

    // Used as a base for drawing.
    protected int getScrollOffsetX() {
        if (tabScroller != null)
            return -(int) (tabScroller.scrollPoint * scrollAreaX);
        return 0;
    }

    public double getScrollPoint() {
        return tabScroller.scrollPoint;
    }
    public void setScrollPoint(double point) {
        tabScroller.scrollPoint = point;
    }

    public void findReplacementTab() {
        if (canSelectNone) {
            parentView.selectTab(null);
        } else if (tabs.size() > 0) {
            parentView.selectTab(tabs.getFirst().contents);
        } else {
            parentView.selectTab(null);
        }
    }

    public boolean handleIncoming() {
        if (incomingTabs.size() > 0) {
            tabs.addAll(incomingTabs);
            incomingTabs.clear();
            if (parentView.selectedTab == null)
                parentView.selectTab(tabs.getFirst().contents);
            layoutRecalculateMetrics();
            return true;
        }
        return false;
    }

    private void tabReorderer(int x, Tab target) {
        FontManager fm = Theme.FM_GLOBAL.get(this);
        int oldIndex = tabs.indexOf(target);
        // Oscillates if a tab that's being nudged left to make way moves out of range.
        // Since these are always two-frame oscillations, just deal with it the simple way...
        for (int pass = 0; pass < 2; pass++) {
            // Used to reorder tabs
            int expectedIndex = -1;
            int selectedIndex = -1;
            int pos = getScrollOffsetX();
            int i = 0;
            for (Tab w : tabs) {
                int tabW = getTabWidth(fm, w, shortTabs, effectiveHeight);
                pos += tabW;
                if (x < pos)
                    if (expectedIndex == -1)
                        expectedIndex = i;
                if (target == w)
                    selectedIndex = i;
                i++;
            }
            if (expectedIndex == -1)
                expectedIndex = tabs.size() - 1;
            if (selectedIndex == -1)
                return;
            Tab w = tabs.remove(selectedIndex);
            tabs.add(expectedIndex, w);
        }
        int newIndex = tabs.indexOf(target);
        if (newIndex != oldIndex)
            tabReorderDidSomething = true;
    }

    private void allTabReordersComplete() {
        if (tabReorderDidSomething)
            parentView.handleTabReorderComplete();
        tabReorderDidSomething = false;
    }

    public static int getTabWidth(FontManager fm, Tab window, int shortTab, int h) {
        int margin = h / 8;
        int tabExMargin = margin + (margin / 2);
        int textHeight = h - (margin * 2);
        if (shortTab == 0)
            tabExMargin = 0;
        return fm.getLineLength(getVisibleTabName(window, shortTab), textHeight) + (tabExMargin * 2) + (h * window.icons.length);
    }

    public static String getVisibleTabName(Tab w, int shortTab) {
        String name = w.contents.toString();
        if (shortTab != -1)
            return name.substring(0, Math.min(name.length(), shortTab));
        return name;
    }

    /*
     * > function getMargin(i) return math.floor(i / 8) end
     * > function getHeight(i) return i + (math.floor(i / 8) * 2) end
     * > function getRoundtrip(i) return getHeight(i) - (getMargin(i) * 2) end
     * > for i = 1, 128 do print(i, getRoundtrip(i)) end
     */
    public static int getHeight(int h) {
        return h + ((h / 8) * 2);
    }

    public static void drawTab(Theme theme, Theme.Attr<IBorder> border, int x, int y, int w, int h, IGrDriver igd, String text, Tab tab) {
        drawTab(theme, border, x, y, w, h, igd, text, tab, true, true);
    }
    public static void drawTab(Theme theme, Theme.Attr<IBorder> border, int x, int y, int w, int h, IGrDriver igd, String text, Tab tab, boolean enBack, boolean enFore) {
        int margin = h / 8;
        int textHeight = h - (margin * 2);
        int tabExMargin = margin + (margin / 2);
        int tabIcoMargin = h / 4;

        if (enBack)
            UIBorderedElement.drawBorder(theme, igd, border, margin, x, y, w, h);

        if (enFore) {
            tab.titleTextCache.text = text;
            tab.titleTextCache.blackText = UIBorderedElement.getBlackTextFlag(theme, border);
            tab.titleTextCache.font = Theme.FM_GLOBAL.get(theme).getFontForText(text, textHeight);
            tab.titleTextCache.update();
            tab.titleTextCache.getChunk().renderRoot(igd, x + tabExMargin, y + tabExMargin);
    
            int icoBack = h;
            for (TabIcon i : tab.icons) {
                // sometimes too bright, deal with that
                int size = h - (tabIcoMargin * 2);
                int subMargin = tabIcoMargin / 2;
                igd.clearRect(0, 0, 0, x + w - ((icoBack - tabIcoMargin) + subMargin), y + tabIcoMargin - subMargin, size + (subMargin * 2), size + (subMargin * 2));
                i.draw(igd, x + w - (icoBack - tabIcoMargin), y + tabIcoMargin, size);
                icoBack += h;
            }
        }
    }

    public static boolean clickInTab(Tab wn, int x, int y, int w, int h) {
        int tabIcoMargin = h / 4;

        int icoBack = h;
        for (TabIcon i : wn.icons) {
            // sometimes too bright, deal with that
            int size = h - (tabIcoMargin * 2);
            Rect rc = new Rect(w - (icoBack - tabIcoMargin), tabIcoMargin, size, size);
            if (rc.contains(x, y)) {
                i.click(wn);
                return true;
            }
            icoBack += h;
        }
        return false;
    }

    @Override
    public IPointerReceiver handleNewPointer(IPointer pointer) {
        FontManager fm = Theme.FM_GLOBAL.get(this);
        int x = pointer.getX();
        int y = pointer.getY();
        if (y < effectiveHeight) {
            int pos = getScrollOffsetX();
            for (final Tab w : tabs) {
                final int tabW = getTabWidth(fm, w, shortTabs, effectiveHeight);
                if (x < (pos + tabW)) {
                    if (clickInTab(w, x - pos, y, tabW, effectiveHeight))
                        return null;
                    parentView.selectTab(w.contents);
                    if (canDragTabs) {
                        IPointerReceiver.RelativeResizePointerReceiver rrpr = new IPointerReceiver.RelativeResizePointerReceiver(pos, 0, new Consumer<Size>() {
                            @Override
                            public void accept(Size size) {
                                if (tabs.contains(w)) {
                                    tabReorderer(size.width + (tabW / 2), w);
                                } else {
                                    draggingTabs.remove(w);
                                    if (draggingTabs.isEmpty())
                                        allTabReordersComplete();
                                }
                            }
                        }) {
                            @Override
                            public void handlePointerEnd(IPointer state) {
                                super.handlePointerEnd(state);
                                draggingTabs.remove(w);
                                if (draggingTabs.isEmpty())
                                    allTabReordersComplete();
                            }
                        };
                        draggingTabs.put(w, rrpr);
                        return rrpr;
                    } else {
                        return super.handleNewPointer(pointer);
                    }
                }
                pos += tabW;
            }
            if (canSelectNone)
                parentView.selectTab(null);
        }
        return super.handleNewPointer(pointer);
    }

    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        // Please don't throw computer monitors at me for this.
        if (tabScroller != null) {
            if (y < fullWantedHeight) {
                tabScroller.handleMousewheel(x, y, north);
                return;
            }
        }
        super.handleMousewheel(x, y, north);
    }

    public interface TabIcon {
        void draw(IGrDriver igd, int x, int y, int size);

        // The 'parent' instance provided must be usable to remove the tab.
        void click(Tab parent);
    }

    public static class Tab {
        // requestsUnparenting and onWindowClosed are called on this.
        // The bounds position is controlled by the tab host (such as a UIWindowView.TabShell).
        public final UIElement contents;
        public final TabIcon[] icons;
        public final TextTools.PlainCached titleTextCache = new TextTools.PlainCached();

        public Tab(UIElement con, TabIcon[] ico) {
            contents = con;
            icons = ico;
        }
    }
}
