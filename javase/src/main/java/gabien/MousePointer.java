/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.wsi.IPointer;

/**
 * Extracted from MobilePeripherals, 24th December 2023. 
 */
class MousePointer implements IPointer {
    private final GrInDriver parent;

    MousePointer(GrInDriver parent) {
        this.parent = parent;
    }

    public int button, offsetX, offsetY;

    @Override
    public int getX() {
        return this.parent.mouseX + offsetX;
    }

    @Override
    public int getY() {
        return this.parent.mouseY + offsetY;
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

    void flushOffset() {
        offsetX = 0;
        offsetY = 0;
    }
}