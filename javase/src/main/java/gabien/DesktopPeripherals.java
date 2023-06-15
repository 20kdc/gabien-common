/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.uslx.append.*;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Implements the IDesktopPeripherals interface for an IGrInDriver to use.
 * Created on February 17th, 2018.
 */
public class DesktopPeripherals implements IDesktopPeripherals, IGJSEPeripheralsInternal {
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
        if (parent.currentEditingSession != null)
            parent.currentEditingSession.endSession();
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
    public ITextEditingSession openTextEditingSession(@NonNull String text, boolean multiLine, int textHeight, @Nullable IFunction<String, String> fun) {
        return parent.openEditingSession(this, multiLine, textHeight, fun);
    }

    @Override
    public String aroundTheBorderworldMaintain(TextboxMaintainer tm, int x, int y, int w, int h, String text) {
        return tm.maintainActual((x - shadowScissorX) * parent.sc, (y - shadowScissorY) * parent.sc, w * parent.sc, h * parent.sc, text);
    }

    @Override
    public void finishRemovingEditingSession() {
        parent.currentEditingSession = null;
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
