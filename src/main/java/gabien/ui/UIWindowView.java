/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IGrInDriver;
import gabien.ScissorGrInDriver;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

/**
 * NOTE: This does not support IWindowElement anymore (you have to implement your environment, and stuff like closing a window, on top of this)
 * Created on 12/27/16. Revamped on December 15th, 2017
 */
public class UIWindowView extends UIElement implements IConsumer<UIWindowView.WVWindow> {
    public UIElement backing;
    public LinkedList<WVWindow> windowList = new LinkedList<WVWindow>();
    private HashSet<UIElement> upcomingManualRemovals = new HashSet<UIElement>();
    private LinkedList<WVWindow> upcomingWindowList = new LinkedList<WVWindow>();
    private boolean draggingBackend = false;
    private boolean resizingWindow = false;
    private boolean draggingWindow = false;
    private boolean dragInWindow = false;
    private int lastMX, lastMY;
    private boolean clearKeysLater = false;
    private boolean backingSelected = false;

    // This ought to be used for frame calculations
    public int windowTextHeight = 12;
    public int sizerOfs = 16;
    public int sizerSize = 24;

    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd) {
        windowList.addAll(upcomingWindowList);
        upcomingWindowList.clear();
        int remaining = windowList.size();
        if (clearKeysLater) {
            igd.clearKeys();
            clearKeysLater = false;
        }
        Rect bounds = getBounds();
        Rect backOldBounds = backing.getBounds();
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
            backing.setBounds(new Rect(0, 0, bounds.width, bounds.height));
        backing.updateAndRender(ox, oy, deltaTime, selected && backingSelected, igd);
        LinkedList<WVWindow> wantsDeleting = new LinkedList<WVWindow>();
        int windowFrameHeight = getWindowFrameHeight();
        ScissorGrInDriver wIgd = new ScissorGrInDriver();
        wIgd.inner = igd;
        HashSet<UIElement> upcomingManualRemovals2 = upcomingManualRemovals;
        upcomingManualRemovals = new HashSet<UIElement>();
        for (WVWindow uie : windowList) {
            if (upcomingManualRemovals2.contains(uie.contents)) {
                wantsDeleting.add(uie);
                remaining--;
                continue;
            }
            remaining--;
            boolean winSelected = selected && (!backingSelected) && (remaining == 0);
            Rect b = uie.contents.getBounds();

            igd.clearRect(0, 64, 192, ox + b.x + b.width - sizerOfs, oy + b.y + b.height - sizerOfs, sizerSize, sizerSize);

            wIgd.workTop = (oy + b.y) - windowFrameHeight;
            wIgd.workBottom = (oy + b.y) + b.height;
            wIgd.workLeft = ox + b.x;
            wIgd.workRight = (ox + b.x) + b.width;

            UILabel.drawLabel(wIgd, b.width, ox + b.x, (oy + b.y) - windowFrameHeight, uie.contents.toString(), winSelected ? 2 : 1, windowTextHeight);
            // icons
            for (int i = 0; i < uie.icons.length; i++) {
                Rect ico = getWindowIcon(new Rect(ox + b.x, (oy + b.y) - windowFrameHeight, b.width, windowFrameHeight), i);
                uie.icons[i].draw(wIgd, ico.x, ico.y, ico.height);
            }

            wIgd.clearRect(0, 0, 0, ox + b.x, oy + b.y, b.width, b.height);
            uie.contents.updateAndRender(ox + b.x, oy + b.y, deltaTime, winSelected, wIgd);
        }
        windowList.removeAll(wantsDeleting);
    }

    // Note: -1 is a special parameter to this which means "do not actually do anything other than selection"
    @Override
    public void handleClick(int x, int y, int button) {
        // Just in case something goes wrong, this will disable all the lifecycle flags and make sure we have a fresh start.
        // Will do nothing otherwise.
        handleRelease(x, y);
        int index = windowList.size();
        int frameHeight = getWindowFrameHeight();
        for (Iterator<WVWindow> i = windowList.descendingIterator(); i.hasNext(); ) {
            index--;
            WVWindow uie = i.next();
            Rect innerWindow = uie.contents.getBounds();
            Rect windowFrame = new Rect(innerWindow.x, innerWindow.y - frameHeight, innerWindow.width, frameHeight);
            Rect windowSz = new Rect(innerWindow.x + innerWindow.width - sizerOfs, innerWindow.y + innerWindow.height - sizerOfs, sizerSize, sizerSize);

            if (innerWindow.contains(x, y)) {
                clearKeysLater = true;
                backingSelected = false;
                windowList.remove(index);
                windowList.addLast(uie);
                if (button != -1)
                    uie.contents.handleClick(x - innerWindow.x, y - innerWindow.y, button);
                dragInWindow = true;
                return;
            }
            if (windowFrame.contains(x, y)) {
                clearKeysLater = true;
                backingSelected = false;
                windowList.remove(index);
                windowList.addLast(uie);
                if (button == 1) {
                    for (int j = 0; j < uie.icons.length; j++) {
                        if (getWindowIcon(windowFrame, j).contains(x, y)) {
                            uie.icons[j].click();
                            return;
                        }
                    }
                    draggingWindow = true;
                }
                return;
            }
            // if it hasn't hit the other two, check for the sizer
            if (windowSz.contains(x, y)) {
                clearKeysLater = true;
                backingSelected = false;
                windowList.remove(index);
                windowList.addLast(uie);
                resizingWindow = true;
                return;
            }
        }
        // didn't hit anything?
        if (!backingSelected)
            clearKeysLater = true;
        backingSelected = true;
        draggingBackend = true;
        if (button != -1)
            backing.handleClick(x, y, button);
    }

    private Rect getWindowIcon(Rect windowFrame, int j) {
        int iconTotalSize = windowFrame.height;
        int iconMargin = iconTotalSize / 6;
        int iconSubsize = iconTotalSize - (iconMargin * 2);
        int iconX = windowFrame.x + windowFrame.width;
        iconX -= iconTotalSize * (j + 1);
        return new Rect(iconX + iconMargin, windowFrame.y + iconMargin, iconSubsize, iconSubsize);
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
        Rect r = win.contents.getBounds();
        Rect g = getBounds();
        int area = g.width - r.width;
        if (area < 0)
            area = 0;
        int cX = new Random().nextInt(area + 1);
        if ((g.height - 64) < r.height)
            win.contents.setBounds(new Rect(cX, 0, r.width, r.height));
        win.contents.setBounds(new Rect(cX, 64, r.width, r.height));
        upcomingWindowList.add(win);
    }

    @Override
    public void handleDrag(int x, int y) {
        if (draggingBackend) {
            backing.handleDrag(x, y);
            return;
        }
        if (windowList.size() > 0) {
            WVWindow lastWindow = windowList.getLast();
            Rect r = lastWindow.contents.getBounds();
            Rect scr = getBounds();
            if (draggingWindow) {
                int ox = r.x + (x - lastMX);
                int oy = r.y + (y - lastMY);
                if (ox < 0)
                    ox = 0;
                int fh = getWindowFrameHeight();
                if (oy < fh)
                    oy = fh;
                if (ox > (scr.width - r.width))
                    ox = (scr.width - r.width);
                if (oy > scr.height)
                    oy = scr.height;
                lastWindow.contents.setBounds(new Rect(ox, oy, r.width, r.height));
            } else if (dragInWindow) {
                lastWindow.contents.handleDrag(x - r.x, y - r.y);
            } else if (resizingWindow) {
                int ox = r.width + (x - lastMX);
                int oy = r.height + (y - lastMY);
                lastWindow.contents.setBounds(new Rect(r.x, r.y, ox, oy));
            }
        }
        lastMX = x;
        lastMY = y;
    }

    @Override
    public void handleRelease(int x, int y) {
        if (draggingBackend) {
            backing.handleRelease(x, y);
        } else if (dragInWindow) {
            if (windowList.size() > 0) {
                WVWindow lastWindow = windowList.getLast();
                Rect r = lastWindow.contents.getBounds();
                lastWindow.contents.handleRelease(x - r.x, y - r.y);
            }
        }
        // Disable all the lifecycle flags
        draggingWindow = false;
        dragInWindow = false;
        resizingWindow = false;
        draggingBackend = false;
        lastMX = x;
        lastMY = y;
    }

    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        // Firstly, simulate a click.
        handleClick(x, y, -1);
        // Use the currently selected whatever it is.
        if (backingSelected) {
            backing.handleMousewheel(x, y, north);
        } else {
            if (windowList.size() > 0) {
                WVWindow window = windowList.getLast();
                Rect b = window.contents.getBounds();
                window.contents.handleMousewheel(x - b.x, y - b.y, north);
            }
        }
    }

    public int getWindowFrameHeight() {
        return UILabel.getRecommendedSize("", windowTextHeight).height;
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
