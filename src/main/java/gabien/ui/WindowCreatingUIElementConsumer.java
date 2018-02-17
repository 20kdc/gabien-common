/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
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

    @Override
    public void accept(UIElement o) {
        accept(o, 1, false);
    }

    public void accept(UIElement o, int scale, boolean fullscreen) {
        ActiveWindow aw = new ActiveWindow();
        Rect bounds = o.getParentRelativeBounds();
        WindowSpecs ws = GaBIEn.defaultWindowSpecs(o.toString(), bounds.width, bounds.height);
        ws.scale = scale;
        ws.resizable = true;
        ws.fullscreen = fullscreen;
        aw.igd = GaBIEn.makeGrIn(o.toString(), bounds.width, bounds.height, ws);
        aw.hoverer = new IGDPointer(aw.igd, IPointer.PointerType.Mouse);
        aw.clicker = new IGDPointer(aw.igd, IPointer.PointerType.Mouse);
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
                closeThese.add(aw);
                aw.ue.handleRootDisconnect();
                continue;
            }

            aw.hoverer.xo = 0;
            aw.hoverer.yo = 0;

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
            // actually run!
            aw.igd.clearScissoring();
            UIBorderedElement.drawBorder(aw.igd, 5, 4, cw, ch);
            aw.ue.update(dT);
            aw.ue.render(true, aw.hoverer, aw.igd);
            aw.hoverer.check();

            // Handles the global click/drag/release cycle

            HashSet<Integer> justDown = aw.igd.getMouseJustDown();
            if (justDown.size() > 0) {
                if (!aw.pendingRelease) {
                    int button = justDown.iterator().next();
                    aw.clicker.type = IPointer.PointerType.Generic;
                    if (button == 2)
                        aw.clicker.type = IPointer.PointerType.Middle;
                    if (button == 3)
                        aw.clicker.type = IPointer.PointerType.Right;
                    if (button == 4)
                        aw.clicker.type = IPointer.PointerType.X1;
                    if (button == 5)
                        aw.clicker.type = IPointer.PointerType.X2;
                    aw.clicker.xo = 0;
                    aw.clicker.yo = 0;
                    aw.ue.handlePointerBegin(aw.clicker);
                    aw.clicker.check();
                    aw.pendingRelease = true;
                }
            }
            if (aw.pendingRelease && (aw.igd.getMouseDown().size() > 0)) {
                aw.ue.handlePointerUpdate(aw.clicker);
                aw.clicker.check();
            } else {
                if (aw.pendingRelease) {
                    aw.ue.handlePointerEnd(aw.clicker);
                    aw.clicker.check();
                    aw.pendingRelease = false;
                }
            }

            if (aw.igd.getMousewheelJustDown())
                aw.ue.handleMousewheel(aw.igd.getMouseX(), aw.igd.getMouseY(), aw.igd.getMousewheelDir());
            if (aw.igd.stillRunning())
                aw.igd.flush();
            if (aw.ue.requestsUnparenting()) {
                closeThese.add(aw);
                aw.ue.handleRootDisconnect();
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
            activeWindows.remove(aw);
            incomingWindows.remove(aw);
            aw.igd.shutdown();
            aw.ue.handleRootDisconnect();
        }
    }

    private class ActiveWindow {
        IGrInDriver igd;
        UIElement ue;
        public boolean pendingRelease;
        public IGDPointer hoverer;
        public IGDPointer clicker;
    }

    private class IGDPointer implements IPointer {
        public int xo, yo;
        public IGrInDriver base;
        public PointerType type;
        public IGDPointer(IGrInDriver src, PointerType t) {
            base = src;
            type = t;
        }

        @Override
        public int getX() {
            return base.getMouseX() + xo;
        }

        @Override
        public int getY() {
            return base.getMouseY() + yo;
        }

        @Override
        public PointerType getType() {
            return type;
        }

        @Override
        public void performOffset(int x, int y) {
            xo += x;
            yo += y;
        }

        public void check() {
            if ((xo != 0) || (yo != 0))
                throw new RuntimeException("Offset " + xo + " " + yo);

        }
    }
}
