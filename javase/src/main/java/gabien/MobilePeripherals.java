/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.uslx.append.*;
import gabien.text.IFixedSizeFont;

import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Designed to emulate multitouch capability.
 * Create new pointers with the right mouse button,
 * switch between them with Tab, etc.
 * Created on November 17, 2018.
 */
public class MobilePeripherals implements IPeripherals, IGJSEPeripheralsInternal {
    private final GrInDriver parent;
    private int offsetX, offsetY, activePointer;
    private LinkedList<DummyPointer> dummies = new LinkedList<DummyPointer>();
    private DummyPointer mousePointer;
    private IFixedSizeFont font;

    public MobilePeripherals(GrInDriver grInDriver) {
        parent = grInDriver;
        font = GaBIEn.getNativeFontFallback(16, null);
    }

    public void mobilePeripheralsFinishFrame(IImage backBuffer) {
        IGrDriver backBufferGr = null;
        if (backBuffer instanceof IGrDriver)
            backBufferGr = (IGrDriver) backBuffer;
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
        // Debug stuff
        if (backBufferGr != null) {
            int idx = 0;
            for (DummyPointer dp : dummies) {
                backBufferGr.clearRect(idx == activePointer ? 0 : 255, 0, 255, dp.x - 1, dp.y - 1, 3, 3);
                idx++;
            }
            // This is called by GrInDriver because MobilePeripherals do special things.
            String status = "Pointer " + (activePointer + 1) + " of " + dummies.size();
            int statusLen = font.measureLine(status);
            backBufferGr.clearRect(0, 0, 0, 0, 0, statusLen, 16);
            font.renderLine(status, false).renderRoot(backBufferGr, 0, 0);
        }
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
        if (parent.currentEditingSession != null)
            parent.currentEditingSession.endSession();
    }

    @Override
    public ITextEditingSession openTextEditingSession(@NonNull String text, boolean multiLine, int textHeight, @Nullable IFunction<String, String> fun) {
        return parent.openEditingSession(this, multiLine, textHeight, fun);
    }

    @Override
    public String aroundTheBorderworldMaintain(TextboxMaintainer tm, int x, int y, int w, int h, String text) {
        return tm.maintainActual((x - offsetX) * parent.sc, (y - offsetY) * parent.sc, w, h, text);
    }

    @Override
    public void finishRemovingEditingSession() {
        parent.currentEditingSession = null;
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
