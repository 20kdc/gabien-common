/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.IGrInDriver;


import java.util.Iterator;
import java.util.LinkedList;

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
            boolean winSelected = selected && (remaining == 0);
            Rect b = uie.getBounds();
            igd.clearRect(0, 64, 192, ox + b.x + b.width - 16, oy + b.y + b.height - 16, 20, 20);
            UILabel.drawLabelx2(igd, b.width, ox + b.x, (oy + b.y) - 18, uie.toString(), winSelected);
            igd.clearRect(128, 64, 64, ox + b.x + b.width - 15, oy + b.y - 15, 12, 12);
            igd.clearRect(0, 0, 0, ox + b.x, oy + b.y, b.width, b.height);
            uie.updateAndRender(ox + b.x, oy + b.y, deltaTime, winSelected, igd);
        }
        windowList.removeAll(wantsDeleting);
    }

    @Override
    public void handleClick(int x, int y, int button) {
        draggingWindow = false;
        dragInWindow = false;
        resizingWindow = false;
        draggingBackend = false;
        lastMX = x;
        lastMY = y;
        int index = windowList.size();
        for (Iterator<UIElement> i = windowList.descendingIterator() ; i.hasNext();) {
            index--;
            UIElement uie = i.next();
            Rect innerWindow = uie.getBounds();
            Rect windowFrame = new Rect(innerWindow.x, innerWindow.y - 20, innerWindow.width, 20);
            Rect windowSz = new Rect(innerWindow.x + innerWindow.width - 16, innerWindow.y + innerWindow.height - 16, 24, 24);
            Rect windowX = new Rect((windowFrame.x + windowFrame.width) - 14, windowFrame.y + 2, 12, 12);
            if (innerWindow.contains(x, y)) {
                clearKeysLater = true;
                backingSelected = false;
                windowList.remove(index);
                windowList.addLast(uie);
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
                    uie.setBounds(new Rect(0, 18, getBounds().width, getBounds().height - 18));
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
            backing.handleClick(x, y, button);
        }
    }

    public void accept(UIElement win) {
        Rect r = win.getBounds();
        win.setBounds(new Rect(64, 64, r.width, r.height));
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
}
