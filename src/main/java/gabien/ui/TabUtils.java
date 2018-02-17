/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.FontManager;
import gabien.IGrDriver;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.WeakHashMap;

/**
 * Because frankly, the previous tab/window code sucked.
 * Static methods assist UIWindowView - the actual instance form is for UITabPane.
 *
 * Created on February 13th 2018.
 */
public class TabUtils {
    public LinkedList<UIWindowView.WVWindow> tabs = new LinkedList<UIWindowView.WVWindow>();
    public LinkedList<UIWindowView.WVWindow> incomingTabs = new LinkedList<UIWindowView.WVWindow>();

    public HashSet<UIElement> outgoingTabs = new HashSet<UIElement>();

    public final UITabPane parentView;

    // If this is false, the tab pane should try and actively avoid situations in which no tab is selected by choosing the first tab.
    // That said, it's not impossible the calling application could force the pane into a situation where no tab is selected...
    public final boolean canSelectNone;
    public final boolean canDragTabs;

    // This is used as the basis for calculations.
    public final int tabBarHeight;

    // -1 means not shortened, otherwise it's max length.
    protected int shortTabs = -1;

    public WeakHashMap<UIWindowView.WVWindow, IPointerReceiver.RelativeResizePointerReceiver> draggingTabs = new WeakHashMap<UIWindowView.WVWindow, IPointerReceiver.RelativeResizePointerReceiver>();

    public IPointerReceiver.PointerConnector connector = new IPointerReceiver.PointerConnector(new IFunction<IPointer, IPointerReceiver>() {
        @Override
        public IPointerReceiver apply(IPointer pointer) {
            int x = pointer.getX();
            int y = pointer.getY() - parentView.tabBarY;
            if (y < tabBarHeight) {
                if (y >= 0) {
                    int pos = parentView.getScrollOffsetX();
                    for (final UIWindowView.WVWindow w : tabs) {
                        final int tabW = TabUtils.getTabWidth(w, shortTabs, tabBarHeight);
                        if (x < (pos + tabW)) {
                            if (TabUtils.clickInTab(w, x - pos, y - parentView.tabBarY, tabW, tabBarHeight))
                                return null;
                            parentView.selectTab(w.contents);
                            if (canDragTabs) {
                                IPointerReceiver.RelativeResizePointerReceiver rrpr = new IPointerReceiver.RelativeResizePointerReceiver(pos, 0, new IConsumer<Size>() {
                                    @Override
                                    public void accept(Size size) {
                                        if (tabs.contains(w)) {
                                            tabReorderer(size.width + (tabW / 2), w);
                                        } else {
                                            draggingTabs.remove(w);
                                        }
                                    }
                                }) {
                                    @Override
                                    public void handlePointerEnd(IPointer state) {
                                        super.handlePointerEnd(state);
                                        draggingTabs.remove(w);
                                    }
                                };
                                draggingTabs.put(w, rrpr);
                                return rrpr;
                            } else {
                                return null;
                            }
                        }
                        pos += tabW;
                    }
                    if (canSelectNone)
                        parentView.selectTab(null);
                }
            }
            return null;
        }
    });

    public TabUtils(boolean selectNone, boolean canDrag, UITabPane par, int h) {
        canSelectNone = selectNone;
        canDragTabs = canDrag;
        parentView = par;
        tabBarHeight = getHeight(h);
    }

    public void render(Size bounds, int tabBarY, IGrDriver igd) {
        boolean willUpdateLater = parentView.handleIncoming();

        igd.clearRect(16, 16, 16, 0, tabBarY, bounds.width, tabBarHeight);
        igd.clearRect(32, 32, 32, 0, tabBarY + ((tabBarHeight / 2) - 1), bounds.width, 2);

        LinkedList<UIWindowView.WVWindow> outgoing2 = new LinkedList<UIWindowView.WVWindow>();
        HashSet<UIElement> outgoingTabs2 = outgoingTabs;
        outgoingTabs = new HashSet<UIElement>();
        for (int pass = 0; pass < (((draggingTabs.size() > 0) && canDragTabs) ? 2 : 1); pass++) {
            int pos = parentView.getScrollOffsetX();
            boolean toggle = false;
            for (UIWindowView.WVWindow w : tabs) {
                if (outgoingTabs2.contains(w.contents)) {
                    willUpdateLater = true;
                    outgoing2.add(w);
                }
                // This is used for all rendering.
                int theDisplayOX = pos;
                int tabW = TabUtils.getTabWidth(w, shortTabs, tabBarHeight);
                int base = toggle ? 64 : 32;
                if (parentView.selectedTab == w)
                    base = 128;
                toggle = !toggle;

                // Decide against rendering
                boolean shouldRender = true;
                if (pass == 0) {
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

                TabUtils.drawTab(base, base / 2, theDisplayOX, tabBarY, tabW, tabBarHeight, igd, TabUtils.getVisibleTabName(w, shortTabs), w.icons);

                pos += tabW;
            }
        }
        if (parentView.selectedTab != null)
            if (outgoingTabs2.contains(parentView.selectedTab.contents))
                findReplacementTab();
        tabs.removeAll(outgoing2);

        if (willUpdateLater)
            parentView.runLayout();
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
            return true;
        }
        return false;
    }

    private void tabReorderer(int x, UIWindowView.WVWindow target) {
        // Oscillates if a tab that's being nudged left to make way moves out of range.
        // Since these are always two-frame oscillations, just deal with it the simple way...
        for (int pass = 0; pass < 2; pass++) {
            // Used to reorder tabs
            int expectedIndex = -1;
            int selectedIndex = -1;
            int pos = parentView.getScrollOffsetX();
            int i = 0;
            for (UIWindowView.WVWindow w : tabs) {
                int tabW = TabUtils.getTabWidth(w, shortTabs, tabBarHeight);
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
            UIWindowView.WVWindow w = tabs.remove(selectedIndex);
            tabs.add(expectedIndex, w);
        }
    }

    public static int getTabWidth(UIWindowView.WVWindow window, int shortTab, int h) {
        int margin = h / 8;
        int tabExMargin = margin + (margin / 2);
        int textHeight = h - (margin * 2);
        if (shortTab == 0)
            tabExMargin = 0;
        return FontManager.getLineLength(getVisibleTabName(window, shortTab), textHeight) + (tabExMargin * 2) + (h * window.icons.length);
    }

    public static String getVisibleTabName(UIWindowView.WVWindow w, int shortTab) {
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

    public static void drawTab(int base, int inner, int x, int y, int w, int h, IGrDriver igd, String text, UIWindowView.IWVWindowIcon[] icons) {
        int margin = h / 8;
        int textHeight = h - (margin * 2);
        int tabExMargin = margin + (margin / 2);
        int tabIcoMargin = h / 4;

        igd.clearRect(base, base, base, x, y, w, h);
        // use a margin to try and still provide a high-contrast display despite the usability 'improvements' making the tabs brighter supposedly provides
        igd.clearRect(inner, inner, inner, x + margin, y + margin, w - (margin * 2), h - (margin * 2));

        FontManager.drawString(igd, x + tabExMargin, y + tabExMargin, text, true, textHeight);

        int icoBack = h;
        for (UIWindowView.IWVWindowIcon i : icons) {
            // sometimes too bright, deal with that
            int size = h - (tabIcoMargin * 2);
            int subMargin = tabIcoMargin / 2;
            igd.clearRect(0, 0, 0, x + w - ((icoBack - tabIcoMargin) + subMargin), y + tabIcoMargin - subMargin, size + (subMargin * 2), size + (subMargin * 2));
            i.draw(igd, x + w - (icoBack - tabIcoMargin), y + tabIcoMargin, size);
            icoBack += h;
        }
    }

    public static boolean clickInTab(UIWindowView.WVWindow wn, int x, int y, int w, int h) {
        int tabIcoMargin = h / 4;

        int icoBack = h;
        for (UIWindowView.IWVWindowIcon i : wn.icons) {
            // sometimes too bright, deal with that
            int size = h - (tabIcoMargin * 2);
            Rect rc = new Rect(w - (icoBack - tabIcoMargin), tabIcoMargin, size, size);
            if (rc.contains(x, y)) {
                i.click();
                return true;
            }
            icoBack += h;
        }
        return false;
    }
}
