/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.*;
import gabien.ui.theming.Theme;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.WeakHashMap;
import gabien.uslx.append.*;

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

    protected WindowSpecs setupSpecs(UIElement o, int scale, boolean fullscreen, boolean resizable) {
        Rect bounds = o.getParentRelativeBounds();
        WindowSpecs ws = GaBIEn.defaultWindowSpecs(o.toString(), bounds.width, bounds.height);
        ws.scale = scale;
        ws.resizable = resizable;
        ws.fullscreen = fullscreen;
        ws.backgroundLight = UIBorderedElement.getBlackTextFlagWindowRoot(o.getTheme());
        return ws;
    }
    
    public void accept(UIElement o, int scale, boolean fullscreen, boolean resizable) {
        final ActiveWindow aw = new ActiveWindow();
        Rect bounds = o.getParentRelativeBounds();
        aw.igd = GaBIEn.makeGrIn(o.toString(), bounds.width, bounds.height, setupSpecs(o, scale, fullscreen, resizable));
        aw.peripherals = aw.igd.getPeripherals();
        aw.ue = o;
        incomingWindows.add(aw);
        o.setAttachedToRoot(true);
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
        LinkedList<ActiveWindow> closeTheseSD = new LinkedList<ActiveWindow>();
        LinkedList<ActiveWindow> closeTheseNS = new LinkedList<ActiveWindow>();
        for (ActiveWindow aw : new LinkedList<ActiveWindow>(activeWindows)) {
            if (!aw.igd.stillRunning()) {
                aw.ue.setAttachedToRoot(false);
                closeTheseNS.add(aw);
                continue;
            }

            // Cycle through backbuffers. This is important for async readPixels.
            aw.nextBackBuffer++;
            aw.nextBackBuffer = aw.nextBackBuffer % aw.backBuffers.length;
            IGrDriver backbuffer = aw.backBuffers[aw.nextBackBuffer] = aw.igd.ensureBackBuffer(aw.backBuffers[aw.nextBackBuffer]);

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
            float[] trs = backbuffer.getTRS();
            trs[0] = 0;
            trs[1] = 0;
            trs[2] = 1;
            trs[3] = 1;
            int[] sti = backbuffer.getScissor();
            sti[0] = 0;
            sti[1] = 0;
            sti[2] = cw;
            sti[3] = ch;
            aw.peripherals.clearOffset();
            UIBorderedElement.drawBorder(aw.ue.getTheme(), backbuffer, Theme.B_WINDOW, 0, 0, 0, cw, ch);
            aw.ue.update(dT, true, aw.peripherals);
            aw.ue.renderAllLayers(backbuffer);

            // Handles the global click/drag/release cycle

            aw.peripherals.clearOffset();
            HashSet<IPointer> pointersNext = aw.peripherals.getActivePointers();
            for (IPointer ip : pointersNext) {
                if (!aw.lastActivePointers.contains(ip)) {
                    // New pointer.
                    IPointerReceiver ipr = aw.ue.handleNewPointer(ip);
                    if (ipr != null) {
                        ipr.handlePointerBegin(ip);
                        aw.receiverMap.put(ip, ipr);
                    }
                } else {
                    // Continuing pointer.
                    IPointerReceiver ipr = aw.receiverMap.get(ip);
                    if (ipr != null)
                        ipr.handlePointerUpdate(ip);
                }
            }
            for (IPointer ip : aw.lastActivePointers) {
                if (!pointersNext.contains(ip)) {
                    // Ending pointer.
                    IPointerReceiver ipr = aw.receiverMap.get(ip);
                    if (ipr != null) {
                        ipr.handlePointerEnd(ip);
                        aw.receiverMap.remove(ip);
                    }
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
            if (aw.igd.stillRunning()) {
                aw.igd.flush(backbuffer);
            }
            if (aw.ue.requestsUnparenting()) {
                closeTheseSD.add(aw);
                aw.igd.shutdown();
                aw.ue.setAttachedToRoot(false);
            }
        }
        activeWindows.removeAll(closeTheseSD);
        activeWindows.removeAll(closeTheseNS);
        for (ActiveWindow aw : closeTheseSD)
            handleClosedUserWindow(aw.ue, true);
        for (ActiveWindow aw : closeTheseNS)
            handleClosedUserWindow(aw.ue, false);
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
            aw.ue.setAttachedToRoot(false);
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
        int nextBackBuffer = 0;
        IGrDriver[] backBuffers = new IGrDriver[2];
        UIElement ue;
        IPeripherals peripherals;
        HashSet<IPointer> lastActivePointers = new HashSet<IPointer>();
        WeakHashMap<IPointer, IPointerReceiver> receiverMap = new WeakHashMap<IPointer, IPointerReceiver>();
    }
}
