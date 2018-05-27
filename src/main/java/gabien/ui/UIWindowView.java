/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IPeripherals;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

/**
 * NOTE: You have to implement your environment, and stuff like closing a window, on top of this.
 * However, this does implement the root-disconnected callback, and request-close.
 * The request-close triggers a blank method for extra post-close behavior for... reasons.
 * Created on 12/27/16. Revamped on December 15th, 2017.
 * Ported on February 17th, 2018, as part of what I'm now calling "Project IPCRESS" for no discernible reason.
 * (Oh, shush, if you were doing this you'd go mad too.)
 */
public class UIWindowView extends UIElement implements IConsumer<UIWindowView.WVWindow> {
    public UIElement backing;
    public final LinkedList<WVWindow> windowList = new LinkedList<WVWindow>();
    private HashSet<UIElement> upcomingManualRemovals = new HashSet<UIElement>();
    private final LinkedList<WVWindow> upcomingWindowList = new LinkedList<WVWindow>();
    private final IPointerReceiver.PointerConnector connector;
    private boolean clearKeysLater = false;
    private boolean backingSelected = false;

    // This ought to be used for frame calculations
    public int windowTextHeight = 12;
    public int sizerVisual = 3;
    public int sizerActual = 8;

    public UIWindowView() {
        connector = new PointerConnector(new IFunction<IPointer, IPointerReceiver>() {
            @Override
            public IPointerReceiver apply(IPointer iPointer) {
                int x = iPointer.getX();
                int y = iPointer.getY();
                boolean buttonOne = iPointer.getType() == IPointer.PointerType.Generic;
                boolean wasBackingSelected = backingSelected;
                backingSelected = false;
                int index = windowList.size();
                int frameHeight = getWindowFrameHeight();
                for (Iterator<WVWindow> i = windowList.descendingIterator(); i.hasNext(); ) {
                    index--;
                    final WVWindow uie = i.next();
                    Rect innerWindow = uie.contents.getParentRelativeBounds();
                    final Rect windowFrame = new Rect(innerWindow.x, innerWindow.y - frameHeight, innerWindow.width, frameHeight);

                    // ABC
                    // D E
                    // FGH
                    Rect windowSz = new Rect(innerWindow.x - sizerActual, innerWindow.y - (frameHeight + sizerActual), innerWindow.width + (sizerActual * 2), frameHeight + innerWindow.height + (sizerActual * 2));

                    if (innerWindow.contains(x, y)) {
                        clearKeysLater = true;
                        windowList.remove(index);
                        windowList.addLast(uie);
                        return new IPointerReceiver.TransformingElementPointerReceiver(uie.contents);
                    }
                    if (windowFrame.contains(x, y)) {
                        clearKeysLater = true;
                        windowList.remove(index);
                        windowList.addLast(uie);
                        if (buttonOne)
                            if (TabUtils.clickInTab(uie, x - windowFrame.x, y - windowFrame.y, innerWindow.width, windowFrame.height))
                                return null;
                        // Dragging...
                        return new IPointerReceiver.RelativeResizePointerReceiver(innerWindow.x, innerWindow.y, new IConsumer<Size>() {
                            @Override
                            public void accept(Size size) {
                                if (windowList.contains(uie)) {
                                    Rect r = uie.contents.getParentRelativeBounds();
                                    uie.contents.setForcedBounds(null, new Rect(size.width, size.height, r.width, r.height));
                                    windowBoundsCheck(uie);
                                }
                            }
                        });
                    }
                    // if it hasn't hit the other two, check for the sizer
                    if (windowSz.contains(x, y)) {
                        clearKeysLater = true;
                        windowList.remove(index);
                        windowList.addLast(uie);
                        int px = 0;
                        int py = 0;
                        int tX = innerWindow.width / 3;
                        int tY = (innerWindow.height + frameHeight) / 3;
                        if (x < (innerWindow.x + tX))
                            px--;
                        if (x >= (innerWindow.x + innerWindow.width - tX))
                            px++;
                        if (y < ((innerWindow.y - frameHeight) + tY))
                            py--;
                        if (y >= ((innerWindow.y - frameHeight) + innerWindow.height - tY))
                            py++;
                        final int fpx = px;
                        final int fpy = py;
                        // Dragging...
                        return new IPointerReceiver.RelativeResizePointerReceiver(fpx == -1 ? innerWindow.x : innerWindow.width, fpy == -1 ? innerWindow.y : innerWindow.height, new IConsumer<Size>() {
                            @Override
                            public void accept(Size size) {
                                if (windowList.contains(uie)) {
                                    Rect r = uie.contents.getParentRelativeBounds();
                                    int a = r.x;
                                    int b = r.y;
                                    int c = r.width;
                                    int d = r.height;
                                    a = processFPA(fpx, size.width, r.x, r.width);
                                    c = processFPB(fpx, size.width, r.x, r.width);
                                    b = processFPA(fpy, size.height, r.y, r.height);
                                    d = processFPB(fpy, size.height, r.y, r.height);
                                    uie.contents.setForcedBounds(null, new Rect(a, b, c, d));
                                    windowBoundsCheck(uie);
                                }
                            }

                            private int processFPA(int fp, int sz, int l, int s) {
                                if (fp == -1) {
                                    return sz;
                                } else if (fp == 1) {
                                    return l;
                                }
                                return l;
                            }

                            private int processFPB(int fp, int sz, int l, int s) {
                                if (fp == -1) {
                                    return s - (sz - l);
                                } else if (fp == 1) {
                                    return sz;
                                }
                                return s;
                            }
                        });
                    }
                }
                // didn't hit anything?
                if (!wasBackingSelected)
                    clearKeysLater = true;
                backingSelected = true;
                if (backing != null)
                    return new IPointerReceiver.TransformingElementPointerReceiver(backing);
                return null;
            }
        });
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        if (clearKeysLater) {
            peripherals.clearKeys();
            clearKeysLater = false;
        }
        if (backing != null)
            backing.update(deltaTime, selected && backingSelected, peripherals);
        int remaining = windowList.size();
        for (WVWindow window : windowList) {
            remaining--;
            Rect p = window.contents.getParentRelativeBounds();
            peripherals.performOffset(-p.x, -p.y);
            window.contents.update(deltaTime, selected && (!backingSelected) && (remaining == 0), peripherals);
            peripherals.performOffset(p.x, p.y);
        }
    }

    @Override
    public void render(IGrDriver igd) {
        windowList.addAll(upcomingWindowList);
        upcomingWindowList.clear();
        int remaining = windowList.size();
        Size bounds = getSize();
        if (backing != null) {
            Rect backOldBounds = backing.getParentRelativeBounds();
            boolean backingNeedsRefresh = false;
            if (backOldBounds.x != 0)
                backingNeedsRefresh = true;
            if (backOldBounds.y != 0)
                backingNeedsRefresh = true;
            if (backOldBounds.width != bounds.width)
                backingNeedsRefresh = true;
            if (backOldBounds.height != bounds.height)
                backingNeedsRefresh = true;
            if (backingNeedsRefresh)
                backing.setForcedBounds(null, new Rect(0, 0, bounds.width, bounds.height));
            backing.render(igd);
        } else {
            igd.clearRect(0, 0, 64, 0, 0, bounds.width, bounds.height);
        }

        LinkedList<WVWindow> wantsDeleting = new LinkedList<WVWindow>();
        int windowFrameHeight = getWindowFrameHeight();
        HashSet<UIElement> upcomingManualRemovals2 = upcomingManualRemovals;
        upcomingManualRemovals = new HashSet<UIElement>();
        for (WVWindow uie : windowList) {
            boolean requestedUnparenting = uie.contents.requestsUnparenting();
            if (upcomingManualRemovals2.contains(uie.contents) || requestedUnparenting) {
                wantsDeleting.add(uie);
                remaining--;
                handleClosedUserWindow(uie, requestedUnparenting);
                continue;
            }
            // Just do this, just in case.
            remaining--;
            windowBoundsCheck(uie);
            boolean winSelected = (!backingSelected) && (remaining == 0);
            Rect b = uie.contents.getParentRelativeBounds();

            UIBorderedElement.drawBorder(igd, 5, sizerVisual, b.x - sizerVisual,b.y - (windowFrameHeight + sizerVisual), b.width + (sizerVisual * 2), b.height + (windowFrameHeight + (sizerVisual * 2)));

            TabUtils.drawTab(winSelected ? 12 : 11, b.x, b.y - windowFrameHeight, b.width, windowFrameHeight, igd, uie.contents.toString(), uie.icons);

            UIPanel.scissoredRender(uie.contents, igd, bounds.width, bounds.height);
        }
        windowList.removeAll(wantsDeleting);
    }

    public void handleClosedUserWindow(WVWindow wvWindow, boolean selfDestruct) {
        // Default behavior: override as you wish
        wvWindow.contents.onWindowClose();
    }

    @Override
    public void accept(WVWindow win) {
        if (upcomingWindowList.contains(win)) {
            System.out.println("Warning: Window already in upcoming window list, this would just break stuff");
            return;
        } else if (windowList.contains(win)) {
            System.out.println("Warning: Window already in window list, this would just break stuff");
            return;
        }
        Rect r = win.contents.getParentRelativeBounds();
        Size g = getSize();
        int area = g.width - r.width;
        if (area < 0)
            area = 0;
        int cX = new Random().nextInt(area + 1);
        if ((g.height - 64) < r.height)
            win.contents.setForcedBounds(null, new Rect(cX, 0, r.width, r.height));
        win.contents.setForcedBounds(null, new Rect(cX, 64, r.width, r.height));
        upcomingWindowList.add(win);
    }

    private void windowBoundsCheck(WVWindow wv) {
        int fh = getWindowFrameHeight();
        Rect scr = getParentRelativeBounds();
        Rect s = wv.contents.getParentRelativeBounds();
        int ox = s.x;
        int oy = s.y;
        if (ox < 0)
            ox = 0;
        if (ox > (scr.width - s.width))
            ox = scr.width - s.width;
        if (oy < fh)
            oy = fh;
        if (oy > scr.height)
            oy = scr.height;
        int ow = s.width;
        int oh = s.height;
        // Can only happen at this point if forced left by above code?
        if (ox < 0) {
            ow += ox;
            ox = 0;
        }
        ow = Math.max(ow, TabUtils.getTabWidth(wv, 0, fh));
        oh = Math.max(oh, 0);
        if ((ox != s.x) || (oy != s.y) || (ow != s.width) || (oh != s.height))
            wv.contents.setForcedBounds(null, new Rect(ox, oy, ow, oh));
    }

    @Override
    public void handlePointerBegin(IPointer state) {
        connector.handlePointerBegin(state);
    }

    @Override
    public void handlePointerUpdate(IPointer state) {
        connector.handlePointerUpdate(state);
    }

    @Override
    public void handlePointerEnd(IPointer state) {
        connector.handlePointerEnd(state);
    }

    @Override
    public void handleMousewheel(final int x, final int y, boolean north) {
        // This should select the window it's used over, so give an Obviously Fake Mouse to the generateReceivers callback,
        //  which won't have any *undesired* side-effects if given an Obviously Fake Mouse.
        connector.generateReceivers.apply(new IPointer() {
            @Override
            public int getX() {
                return x;
            }

            @Override
            public int getY() {
                return y;
            }

            @Override
            public PointerType getType() {
                return null;
            }

            @Override
            public void performOffset(int x, int y) {
                System.err.println("gabien.ui: If you're encountering this message, the Obviously Fake Mouse got run through a strip-search in airport inspection.");
            }
        });
        // Use the currently selected whatever it is.
        if (backingSelected) {
            if (backing != null)
                backing.handleMousewheel(x, y, north);
        } else {
            if (windowList.size() > 0) {
                WVWindow window = windowList.getLast();
                Rect b = window.contents.getParentRelativeBounds();
                window.contents.handleMousewheel(x - b.x, y - b.y, north);
            }
        }
    }

    public int getWindowFrameHeight() {
        return TabUtils.getHeight(windowTextHeight);
    }

    public void removeByUIE(UIElement uiElement) {
        upcomingManualRemovals.add(uiElement);
    }

    public interface IWVWindowIcon {
        void draw(IGrDriver igd, int x, int y, int size);

        void click();
    }

    public static class WVWindow {
        // bounds is relevant, and this may be a IWindowElement
        public final UIElement contents;
        public final IWVWindowIcon[] icons;

        public WVWindow(UIElement con, IWVWindowIcon[] ico) {
            contents = con;
            icons = ico;
        }
    }
}
