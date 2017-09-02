/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.GaBIEn;
import gabien.IGrInDriver;
import gabien.WindowSpecs;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Utility class useful for dealing with the boilerplate UI bootstrap code
 * to hook up the IGD and outermost UIElement.
 * If dealing with single-window-mode, use this once (to get a root window),
 * then create a UIWindowView for your root window's element,
 * and take the highest resolution you can get.
 * Created on 12/30/16.
 */
public class WindowCreatingUIElementConsumer implements IConsumer<UIElement> {
    private LinkedList<ActiveWindow> activeWindows = new LinkedList<ActiveWindow>();
    private LinkedList<ActiveWindow> incomingWindows = new LinkedList<ActiveWindow>();
    public int createScale = 1;

    @Override
    public void accept(UIElement o) {
        ActiveWindow aw = new ActiveWindow();
        Rect bounds = o.getBounds();
        WindowSpecs ws = GaBIEn.defaultWindowSpecs(o.toString(), bounds.width, bounds.height);
        ws.scale = createScale;
        ws.resizable = true;
        aw.igd = GaBIEn.makeGrIn(o.toString(), bounds.width, bounds.height, ws);
        aw.ue = o;
        aw.lastKnownWidth = bounds.width;
        aw.lastKnownHeight = bounds.height;
        incomingWindows.add(aw);
    }

    public LinkedList<UIElement> runningWindows() {
        LinkedList<UIElement> w = new LinkedList<UIElement>();
        for (ActiveWindow aw : activeWindows)
            w.add(aw.ue);
        for (ActiveWindow aw : incomingWindows)
            w.add(aw.ue);
        return w;
    }

    public void runTick(double dT) {
        activeWindows.addAll(incomingWindows);
        incomingWindows.clear();
        LinkedList<ActiveWindow> closeThese = new LinkedList<ActiveWindow>();
        for (ActiveWindow aw : new LinkedList<ActiveWindow>(activeWindows)) {
            boolean needResize = false;
            if (aw.lastKnownWidth != aw.igd.getWidth())
                needResize = true;
            if (aw.lastKnownHeight != aw.igd.getHeight())
                needResize = true;
            if (needResize)
                aw.ue.setBounds(new Rect(0, 0, aw.igd.getWidth(), aw.igd.getHeight()));

            if (!aw.igd.stillRunning()) {
                closeThese.add(aw);
                if (aw.ue instanceof IWindowElement)
                    ((IWindowElement) (aw.ue)).windowClosed();
                continue;
            }
            // actually run!
            aw.igd.clearAll(0, 0, 0);
            aw.ue.updateAndRender(0, 0, dT, true, aw.igd);

            // Handles the global click/drag/release cycle

            HashSet<Integer> justDown = aw.igd.getMouseJustDown();
            if (justDown.size() > 0) {
                if (!aw.pendingRelease) {
                    int button = justDown.iterator().next();
                    aw.ue.handleClick(aw.igd.getMouseX(), aw.igd.getMouseY(), button);
                    aw.pendingRelease = true;
                }
            }
            if (aw.pendingRelease && (aw.igd.getMouseDown().size() > 0)) {
                aw.ue.handleDrag(aw.igd.getMouseX(), aw.igd.getMouseY());
            } else {
                if (aw.pendingRelease) {
                    aw.ue.handleRelease(aw.igd.getMouseX(), aw.igd.getMouseY());
                    aw.pendingRelease = false;
                }
            }

            if (aw.igd.getMousewheelJustDown())
                aw.ue.handleMousewheel(aw.igd.getMouseX(), aw.igd.getMouseY(), aw.igd.getMousewheelDir());
            aw.igd.flush();
            if (aw.ue instanceof IWindowElement) {
                if (((IWindowElement) (aw.ue)).wantsSelfClose()) {
                    closeThese.add(aw);
                    ((IWindowElement) (aw.ue)).windowClosed();
                    aw.igd.shutdown();
                }
            }
        }
        activeWindows.removeAll(closeThese);
    }

    public void forceRemove(UIElement uie) {
        LinkedList<ActiveWindow> w = new LinkedList<ActiveWindow>();
        for (ActiveWindow aw : activeWindows)
            if (aw.ue == uie)
                w.add(aw);
        for (ActiveWindow aw : incomingWindows)
            if (aw.ue == uie)
                w.add(aw);
        for (ActiveWindow aw : w) {
            activeWindows.remove(aw);
            incomingWindows.remove(aw);
            aw.igd.shutdown();
            if (aw.ue instanceof IWindowElement)
                ((IWindowElement) (aw.ue)).windowClosed();
        }
    }

    private class ActiveWindow {
        IGrInDriver igd;
        UIElement ue;
        int lastKnownWidth, lastKnownHeight;
        public boolean pendingRelease;
    }
}
