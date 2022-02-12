/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.ui.IFunction;
import gabien.ui.IPointer;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Implements the IDesktopPeripherals interface for an IGrInDriver to use.
 * Created on February 17th, 2018.
 */
public class DesktopPeripherals implements IDesktopPeripherals {
    private GrInDriver parent;

    private int shadowScissorX, shadowScissorY;
    private HashMap<Integer, MousePointer> activePointers = new HashMap<Integer, MousePointer>();

    public DesktopPeripherals(GrInDriver par) {
        parent = par;
    }

    @Override
    public int getMouseX() {
        return parent.mouseX + shadowScissorX;
    }

    @Override
    public int getMouseY() {
        return parent.mouseY + shadowScissorY;
    }

    @Override
    public int getMousewheelBuffer() {
        parent.mouseLock.lock();
        int d = parent.mousewheelMovements;
        if (d < 0) {
            d = -1;
            parent.mousewheelMovements++;
        } else if (d > 0) {
            d = 1;
            parent.mousewheelMovements--;
        }
        parent.mouseLock.unlock();
        return d;
    }

    @Override
    public boolean isKeyDown(int key) {
        return parent.keys[key];
    }

    @Override
    public boolean isKeyJustPressed(int KEYID) {
        boolean b = parent.keysjd[KEYID];
        parent.keysjd[KEYID] = false;
        return b;
    }

    @Override
    public void clearKeys() {
        for (int p = 0; p < parent.keysjd.length; p++) {
            parent.keysjd[p] = false;
            parent.keys[p] = false;
        }
        parent.mouseLock.lock();
        parent.mouseJustDown.clear();
        parent.mouseJustUp.clear();
        parent.mouseLock.unlock();
        parent.tm.clear();
    }

    @Override
    public HashSet<Integer> activeKeys() {
        HashSet<Integer> keysH = new HashSet<Integer>();
        for (int i = 0; i < parent.keys.length; i++)
            if (parent.keys[i])
                keysH.add(i);
        return keysH;
    }

    @Override
    public String maintain(int x, int y, int width, String text, IFunction<String, String> fun) {
        return parent.tm.maintain((x - shadowScissorX) * parent.sc, (y - shadowScissorY) * parent.sc, width * parent.sc, text, fun);
    }

    @Override
    public void performOffset(int x, int y) {
        shadowScissorX += x;
        shadowScissorY += y;
    }

    @Override
    public void clearOffset() {
        shadowScissorX = 0;
        shadowScissorY = 0;
        for (MousePointer mp : activePointers.values())
            mp.flushOffset();
    }

    @Override
    public HashSet<IPointer> getActivePointers() {
        parent.mouseLock.lock();
        HashSet<Integer> effectiveDown = new HashSet<Integer>(parent.mouseDown);
        effectiveDown.addAll(parent.mouseJustDown);
        parent.mouseJustDown.clear();
        // effectiveDown is the actual emulated mouse set.
        for (Integer i : new HashSet<Integer>(activePointers.keySet()))
            if (!effectiveDown.contains(i))
                activePointers.remove(i);
        HashSet<IPointer> f = new HashSet<IPointer>();
        for (Integer i : effectiveDown) {
            MousePointer mp = activePointers.get(i);
            if (mp == null) {
                mp = new MousePointer();
                mp.button = i;
                activePointers.put(i, mp);
            }
            f.add(mp);
        }
        parent.mouseLock.unlock();
        return f;
    }

    @Override
    public boolean isEnterJustPressed() {
        return isKeyJustPressed(IGrInDriver.VK_ENTER);
    }

    private class MousePointer implements IPointer {
        public int button, offsetX, offsetY;
        @Override
        public int getX() {
            return parent.mouseX + offsetX;
        }

        @Override
        public int getY() {
            return parent.mouseY + offsetY;
        }

        @Override
        public PointerType getType() {
            if (button == 1)
                return PointerType.Generic;
            if (button == 2)
                return PointerType.Middle;
            if (button == 3)
                return PointerType.Right;
            if (button == 4)
                return PointerType.X1;
            if (button == 5)
                return PointerType.X2;
            return PointerType.Mouse;
        }

        @Override
        public void performOffset(int x, int y) {
            offsetX += x;
            offsetY += y;
        }

        private void flushOffset() {
            offsetX = 0;
            offsetY = 0;
        }
    }
}
