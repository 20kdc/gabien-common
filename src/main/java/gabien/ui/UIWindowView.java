/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.IGrInDriver;
import gabien.ScissorGrInDriver;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

/**
 * Created on 12/27/16.
 */
public class UIWindowView extends UIElement implements IConsumer<UIElement> {
    public UIElement backing;
    public LinkedList<UIElement> windowList = new LinkedList<UIElement>();
    public LinkedList<UIElement> upcomingWindowList = new LinkedList<UIElement>();
    public boolean draggingBackend = false;
    public boolean resizingWindow = false;
    public boolean draggingWindow = false;
    public boolean dragInWindow = false;
    public int lastMX, lastMY;
    public Rect currentlyFullscreen = null;
    public boolean clearKeysLater = false;
    public boolean backingSelected = false;

    // This ought to be used for frame calculations
    public int windowTextHeight = 12;

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
        if (currentlyFullscreen == null) {
            backing.setBounds(new Rect(0, 0, bounds.width, bounds.height));
            backing.updateAndRender(ox, oy, deltaTime, selected && backingSelected, igd);
        }
        LinkedList<UIElement> wantsDeleting = new LinkedList<UIElement>();
        int closeButtonMargin = getCloseButtonMargin();
        int closeButtonSize = getCloseButtonSize();
        int windowFrameHeight = getWindowFrameHeight();
        ScissorGrInDriver wIgd = new ScissorGrInDriver();
        wIgd.inner = igd;
        for (UIElement uie : windowList) {
            if (uie instanceof IWindowElement)
                if (((IWindowElement) uie).wantsSelfClose()) {
                    if (remaining == windowList.size())
                        currentlyFullscreen = null;
                    wantsDeleting.add(uie);
                    ((IWindowElement) uie).windowClosed();
                    remaining--;
                    continue;
                }
            remaining--;
            if (currentlyFullscreen != null)
                if (remaining != 0)
                    continue;
            boolean winSelected = selected && (!backingSelected) && (remaining == 0);
            Rect b = uie.getBounds();

            igd.clearRect(0, 64, 192, ox + b.x + b.width - 16, oy + b.y + b.height - 16, 20, 20);

            wIgd.workTop = (oy + b.y) - windowFrameHeight;
            wIgd.workBottom = (oy + b.y) + b.height;
            wIgd.workLeft = ox + b.x;
            wIgd.workRight = (ox + b.x) + b.width;

            UILabel.drawLabel(wIgd, b.width, ox + b.x, (oy + b.y) - windowFrameHeight, uie.toString(), winSelected ? 2 : 1, windowTextHeight);
            wIgd.clearRect(128, 64, 64, ox + b.x + b.width - (closeButtonSize + closeButtonMargin), (oy + b.y) - (closeButtonSize + closeButtonMargin), closeButtonSize, closeButtonSize);
            wIgd.clearRect(0, 0, 0, ox + b.x, oy + b.y, b.width, b.height);
            uie.updateAndRender(ox + b.x, oy + b.y, deltaTime, winSelected, wIgd);
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
        int closeSize = getCloseButtonSize();
        int closeMargin = getCloseButtonMargin();
        for (Iterator<UIElement> i = windowList.descendingIterator(); i.hasNext(); ) {
            index--;
            UIElement uie = i.next();
            Rect innerWindow = uie.getBounds();
            Rect windowFrame = new Rect(innerWindow.x, innerWindow.y - frameHeight, innerWindow.width, frameHeight);
            Rect windowSz = new Rect(innerWindow.x + innerWindow.width - 16, innerWindow.y + innerWindow.height - 16, 24, 24);
            Rect windowX = new Rect((windowFrame.x + windowFrame.width) - (closeMargin + closeSize), innerWindow.y - (closeMargin + closeSize), closeSize, closeSize);
            if (innerWindow.contains(x, y)) {
                clearKeysLater = true;
                backingSelected = false;
                windowList.remove(index);
                windowList.addLast(uie);
                if (button != -1)
                    uie.handleClick(x - innerWindow.x, y - innerWindow.y, button);
                dragInWindow = true;
                return;
            }
            if (windowFrame.contains(x, y)) {
                clearKeysLater = true;
                backingSelected = false;
                windowList.remove(index);
                if (button == 1) {
                    if (windowX.contains(x, y)) {
                        if (currentlyFullscreen != null)
                            currentlyFullscreen = null;
                        if (uie instanceof IWindowElement)
                            ((IWindowElement) uie).windowClosed();
                        return;
                    } else {
                        draggingWindow = true;
                    }
                }
                windowList.addLast(uie);
                if (button == 3) {
                    if (currentlyFullscreen != null) {
                        uie.setBounds(currentlyFullscreen);
                        currentlyFullscreen = null;
                        return;
                    }
                    currentlyFullscreen = innerWindow;
                    uie.setBounds(new Rect(0, frameHeight, getBounds().width, getBounds().height - frameHeight));
                }
                return;
            }
            // if it hasn't hit the other two, check for the sizer
            if (windowSz.contains(x, y)) {
                clearKeysLater = true;
                backingSelected = false;
                windowList.remove(index);
                windowList.addLast(uie);
                if (currentlyFullscreen == null)
                    resizingWindow = true;
                return;
            }
        }
        // didn't hit anything?
        if (currentlyFullscreen == null) {
            if (!backingSelected)
                clearKeysLater = true;
            backingSelected = true;
            draggingBackend = true;
            if (button != -1)
                backing.handleClick(x, y, button);
        }
    }

    public void accept(UIElement win) {
        if (upcomingWindowList.contains(win)) {
            System.out.println("Warning: Window already in upcoming window list, this would just break stuff");
            return;
        } else if (windowList.contains(win)) {
            System.out.println("Warning: Window already in window list, this would just break stuff");
            return;
        }
        Rect r = win.getBounds();
        Rect g = getBounds();
        int area = g.width - r.width;
        if (area < 0)
            area = 0;
        int cX = new Random().nextInt(area + 1);
        if ((g.height - 64) < r.height)
            win.setBounds(new Rect(cX, 0, r.width, r.height));
        win.setBounds(new Rect(cX, 64, r.width, r.height));
        upcomingWindowList.add(win);
    }

    @Override
    public void handleDrag(int x, int y) {
        if (draggingBackend) {
            backing.handleDrag(x, y);
            return;
        }
        if (windowList.size() > 0) {
            UIElement lastWindow = windowList.getLast();
            Rect r = lastWindow.getBounds();
            Rect scr = getBounds();
            if (draggingWindow) {
                if (currentlyFullscreen == null) {
                    int ox = r.x + (x - lastMX);
                    int oy = r.y + (y - lastMY);
                    if (ox < 0)
                        ox = 0;
                    if (oy < 18)
                        oy = 18;
                    if (ox > (scr.width - r.width))
                        ox = (scr.width - r.width);
                    if (oy > scr.height)
                        oy = scr.height;
                    lastWindow.setBounds(new Rect(ox, oy, r.width, r.height));
                }
            } else if (dragInWindow) {
                lastWindow.handleDrag(x - r.x, y - r.y);
            } else if (resizingWindow) {
                if (currentlyFullscreen == null) {
                    int ox = r.width + (x - lastMX);
                    int oy = r.height + (y - lastMY);
                    lastWindow.setBounds(new Rect(r.x, r.y, ox, oy));
                }
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
                UIElement lastWindow = windowList.getLast();
                Rect r = lastWindow.getBounds();
                lastWindow.handleRelease(x - r.x, y - r.y);
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
                UIElement window = windowList.getLast();
                Rect b = window.getBounds();
                window.handleMousewheel(x - b.x, y - b.y, north);
            }
        }
    }

    public int getWindowFrameHeight() {
        return UILabel.getRecommendedSize("", windowTextHeight).height;
    }

    public int getCloseButtonMargin() {
        return 3;
    }

    public int getCloseButtonSize() {
        return getWindowFrameHeight() - (getCloseButtonMargin() * 2);
    }
}
