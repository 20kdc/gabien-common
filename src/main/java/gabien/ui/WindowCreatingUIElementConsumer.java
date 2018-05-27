/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.*;

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

    @Override
    public void accept(UIElement o) {
        accept(o, 1, false);
    }

    public void accept(UIElement o, int scale, boolean fullscreen) {
        accept(o, scale, fullscreen, true);
    }

    public void accept(UIElement o, int scale, boolean fullscreen, boolean resizable) {
        ActiveWindow aw = new ActiveWindow();
        Rect bounds = o.getParentRelativeBounds();
        WindowSpecs ws = GaBIEn.defaultWindowSpecs(o.toString(), bounds.width, bounds.height);
        ws.scale = scale;
        ws.resizable = resizable;
        ws.fullscreen = fullscreen;
        aw.igd = GaBIEn.makeGrIn(o.toString(), bounds.width, bounds.height, ws);
        aw.peripherals = aw.igd.getPeripherals();
        aw.ue = o;
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
            if (!aw.igd.stillRunning()) {
                handleClosedUserWindow(aw.ue, false);
                closeThese.add(aw);
                continue;
            }

            boolean needResize = false;
            int cw = aw.igd.getWidth();
            int ch = aw.igd.getHeight();
            Size s = aw.ue.getSize();
            if (s.width != cw)
                needResize = true;
            if (s.height != ch)
                needResize = true;
            if (needResize)
                aw.ue.setForcedBounds(null, new Rect(0, 0, cw, ch));
            // Init ST & draw
            int[] sti = aw.igd.getLocalST();
            sti[0] = 0;
            sti[1] = 0;
            sti[2] = 0;
            sti[3] = 0;
            sti[4] = cw;
            sti[5] = ch;
            aw.igd.updateST();
            aw.peripherals.clearOffset();
            UIBorderedElement.drawBorder(aw.igd, 5, 0, 0, 0, cw, ch);
            aw.ue.update(dT, true, aw.peripherals);
            aw.ue.render(aw.igd);

            // Handles the global click/drag/release cycle

            aw.peripherals.clearOffset();
            HashSet<IPointer> pointersNext = aw.peripherals.getActivePointers();
            for (IPointer ip : pointersNext) {
                if (!aw.lastActivePointers.contains(ip)) {
                    // New pointer.
                    aw.ue.handlePointerBegin(ip);
                } else {
                    // Continuing pointer.
                    aw.ue.handlePointerUpdate(ip);
                }
            }
            for (IPointer ip : aw.lastActivePointers) {
                if (!pointersNext.contains(ip)) {
                    // Ending pointer.
                    aw.ue.handlePointerEnd(ip);
                }
            }
            aw.lastActivePointers = pointersNext;

            // Mousewheel
            if (aw.peripherals instanceof IDesktopPeripherals) {
                IDesktopPeripherals idp = (IDesktopPeripherals) aw.peripherals;
                int ip = idp.getMousewheelBuffer();
                if (ip != 0)
                    aw.ue.handleMousewheel(idp.getMouseX(), idp.getMouseY(), ip == -1);
            }
            if (aw.igd.stillRunning())
                aw.igd.flush();
            if (aw.ue.requestsUnparenting()) {
                handleClosedUserWindow(aw.ue, true);
                closeThese.add(aw);
                aw.igd.shutdown();
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
            handleClosedUserWindow(aw.ue, false);
            activeWindows.remove(aw);
            incomingWindows.remove(aw);
            aw.igd.shutdown();
        }
    }

    public void handleClosedUserWindow(UIElement wvWindow, boolean selfDestruct) {
        // Default behavior: override as you wish
        wvWindow.onWindowClose();
    }


    private class ActiveWindow {
        IGrInDriver igd;
        UIElement ue;
        IPeripherals peripherals;
        HashSet<IPointer> lastActivePointers = new HashSet<IPointer>();
    }
}
