/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IGrInDriver;

import java.util.*;

/**
 * NOTE: You have to implement your environment, and stuff like closing a window, on top of this.
 * This *does* implement the root-disconnected callback for all windows attached when it is called.
 * It *does not* implement it for windows being removed,
 *  because this callback also serves to alert windows that they are being closed,
 *  and it might simply be a migration.
 * It *does not* implement request-close.
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
    public int sizerOfs = 16;
    public int sizerSize = 24;

    public UIWindowView() {
        connector = new PointerConnector(new IFunction<IPointer, IPointerReceiver>() {
            @Override
            public IPointerReceiver apply(IPointer iPointer) {
                int x = iPointer.getX();
                int y = iPointer.getY();
                boolean buttonOne = iPointer.getType() == IPointer.PointerType.Generic;
                // We don't have any lifecycle flags, but there is this, and it's rarely true.
                backingSelected = false;
                int index = windowList.size();
                int frameHeight = getWindowFrameHeight();
                for (Iterator<WVWindow> i = windowList.descendingIterator(); i.hasNext(); ) {
                    index--;
                    final WVWindow uie = i.next();
                    Rect innerWindow = uie.contents.getParentRelativeBounds();
                    final Rect windowFrame = new Rect(innerWindow.x, innerWindow.y - frameHeight, innerWindow.width, frameHeight);
                    Rect windowSz = new Rect(innerWindow.x + innerWindow.width - sizerOfs, innerWindow.y + innerWindow.height - sizerOfs, sizerSize, sizerSize);

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
                        // Dragging...
                        return new IPointerReceiver.RelativeResizePointerReceiver(innerWindow.width, innerWindow.height, new IConsumer<Size>() {
                            @Override
                            public void accept(Size size) {
                                if (windowList.contains(uie)) {
                                    Rect r = uie.contents.getParentRelativeBounds();
                                    uie.contents.setForcedBounds(null, new Rect(r.x, r.y, size.width, size.height));
                                    windowBoundsCheck(uie);
                                }
                            }
                        });
                    }
                }
                // didn't hit anything?
                if (!backingSelected)
                    clearKeysLater = true;
                backingSelected = true;
                if (backing != null)
                    return new IPointerReceiver.TransformingElementPointerReceiver(backing);
                return null;
            }
        });
    }

    @Override
    public void update(double deltaTime) {
        if (backing != null)
            backing.update(deltaTime);
        for (WVWindow window : windowList)
            window.contents.update(deltaTime);
    }

    @Override
    public void render(boolean selected, IPointer mouse, IGrInDriver igd) {
        windowList.addAll(upcomingWindowList);
        upcomingWindowList.clear();
        int remaining = windowList.size();
        if (clearKeysLater) {
            igd.clearKeys();
            clearKeysLater = false;
        }
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
            backing.render(selected && backingSelected, mouse, igd);
        } else {
            igd.clearRect(0, 0, 64, 0, 0, bounds.width, bounds.height);
        }

        LinkedList<WVWindow> wantsDeleting = new LinkedList<WVWindow>();
        int windowFrameHeight = getWindowFrameHeight();
        HashSet<UIElement> upcomingManualRemovals2 = upcomingManualRemovals;
        upcomingManualRemovals = new HashSet<UIElement>();
        for (WVWindow uie : windowList) {
            if (upcomingManualRemovals2.contains(uie.contents)) {
                wantsDeleting.add(uie);
                remaining--;
                continue;
            }
            // Just do this, just in case.
            remaining--;
            windowBoundsCheck(uie);
            boolean winSelected = selected && (!backingSelected) && (remaining == 0);
            Rect b = uie.contents.getParentRelativeBounds();

            int sizerSSize = sizerSize - ((sizerSize - sizerOfs) / 2);
            igd.clearRect(0, 32, 96, b.x + b.width - sizerOfs, b.y + b.height - sizerOfs, sizerSize, sizerSize);
            igd.clearRect(0, 64, 192, b.x + b.width - sizerOfs, b.y + b.height - sizerOfs, sizerSSize, sizerSSize);

            TabUtils.drawTab(winSelected ? 192 : 48, 32, b.x, b.y - windowFrameHeight, b.width, windowFrameHeight, igd, uie.contents.toString(), uie.icons);

            UIPanel.scissoredRender(true, uie.contents, winSelected, mouse, igd, bounds.width, bounds.height);
        }
        windowList.removeAll(wantsDeleting);
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
        if ((ox != s.x) || (oy != s.y))
            wv.contents.setForcedBounds(null, new Rect(ox, oy, s.width, s.height));
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

    @Override
    public void handleRootDisconnect() {
        super.handleRootDisconnect();
        for (WVWindow w : windowList)
            if (!upcomingManualRemovals.contains(w.contents))
                w.contents.handleRootDisconnect();
        for (WVWindow w : upcomingWindowList)
            if (!upcomingManualRemovals.contains(w.contents))
                w.contents.handleRootDisconnect();
        if (backing != null)
            backing.handleRootDisconnect();
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
