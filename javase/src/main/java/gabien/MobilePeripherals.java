/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.ui.IFunction;
import gabien.ui.IPointer;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Designed to emulate multitouch capability.
 * Create new pointers with the right mouse button,
 * switch between them with Tab, etc.
 * Created on November 17, 2018.
 */
public class MobilePeripherals implements IPeripherals {
    private final GrInDriver parent;
    private int offsetX, offsetY, activePointer;
    private LinkedList<DummyPointer> dummies = new LinkedList<DummyPointer>();
    private DummyPointer mousePointer;

    public MobilePeripherals(GrInDriver grInDriver) {
        parent = grInDriver;
    }

    public void mobilePeripheralsFinishFrame() {
        // Initial safeties
        if (activePointer < 0)
            activePointer = dummies.size() - 1;
        if (activePointer >= dummies.size())
            activePointer = 0;
        // In case dummies.size() - 1 == -1.
        // Basically, ensure that the 'valid pointer check' is just x < dummies.size()
        if (activePointer < 0)
            activePointer = 0;
        parent.mouseLock.lock();
        if (parent.mouseJustDown.contains(3)) {
            parent.mouseJustDown.remove(3);
            dummies.add(new DummyPointer(parent.mouseX, parent.mouseY));
        } else if (activePointer < dummies.size()) {
            if (parent.keysjd[IGrInDriver.VK_BACK_SPACE]) {
                dummies.remove(activePointer);
            } else if (parent.mousewheelMovements != 0) {
                // Will cause wrap/etc. on next call
                activePointer++;
                parent.mousewheelMovements = 0;
            } else if (parent.mouseDown.contains(2)) {
                DummyPointer dp = dummies.get(activePointer);
                dp.x = parent.mouseX;
                dp.y = parent.mouseY;
            }
        }
        parent.keysjd[IGrInDriver.VK_BACK_SPACE] = false;

        if (parent.mouseDown.contains(1)) {
            if (mousePointer == null) {
                mousePointer = new DummyPointer(0, 0);
                dummies.add(mousePointer);
            }
            mousePointer.x = parent.mouseX;
            mousePointer.y = parent.mouseY;
        } else {
            dummies.remove(mousePointer);
            mousePointer = null;
        }
        parent.mouseLock.unlock();
        int idx = 0;
        for (DummyPointer dp : dummies) {
            parent.clearRect(idx == activePointer ? 0 : 255, 0, 255, dp.x - 1, dp.y - 1, 3, 3);
            idx++;
        }
        // This is called by GrInDriver because MobilePeripherals do special things.
        String status = "Pointer " + (activePointer + 1) + " of " + dummies.size();
        int statusLen = GaBIEn.measureText(16, status);
        parent.clearRect(0, 0, 0, 0, 0, statusLen, 16);
        parent.drawText(0, 0, 255, 255, 255, 16, status);
    }

    @Override
    public void performOffset(int x, int y) {
        offsetX += x;
        offsetY += y;
    }

    @Override
    public void clearOffset() {
        offsetX = 0;
        offsetY = 0;
        for (DummyPointer dp : dummies) {
            dp.ox = 0;
            dp.oy = 0;
        }
    }

    @Override
    public HashSet<IPointer> getActivePointers() {
        return new HashSet<IPointer>(dummies);
    }

    @Override
    public void clearKeys() {
        for (int p = 0; p < parent.keysjd.length; p++) {
            parent.keysjd[p] = false;
            parent.keys[p] = false;
        }
        parent.tm.clear();
    }

    @Override
    public String maintain(int x, int y, int width, String text, IFunction<String, String> feedback) {
        return parent.tm.maintain((x - offsetX) * parent.sc, (y - offsetY) * parent.sc, width, text, feedback);
    }

    @Override
    public boolean isEnterJustPressed() {
        boolean b = parent.keysjd[IGrInDriver.VK_ENTER];
        parent.keysjd[IGrInDriver.VK_ENTER] = false;
        return b;
    }

    public static class DummyPointer implements IPointer {
        public int x, y, ox, oy;

        public DummyPointer(int mouseX, int mouseY) {
            x = mouseX;
            y = mouseY;
        }

        @Override
        public int getX() {
            return x + ox;
        }

        @Override
        public int getY() {
            return y + oy;
        }

        @Override
        public PointerType getType() {
            return PointerType.Generic;
        }

        @Override
        public void performOffset(int x, int y) {
            ox += x;
            oy += y;
        }
    }
}
