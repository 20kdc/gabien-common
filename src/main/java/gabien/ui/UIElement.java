/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.IGrInDriver;

public abstract class UIElement {
    protected Rect elementBounds = new Rect(0, 0, 1, 1);

    public void setBounds(Rect r) {
        elementBounds = new Rect(r.x, r.y, r.width, r.height);
    }

    public Rect getBounds() {
        return new Rect(elementBounds.x, elementBounds.y, elementBounds.width, elementBounds.height);
    }

    public abstract void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd);

    public abstract void handleClick(MouseAction action);

    public void handleDrag(int x, int y) {

    }

    // Almost never used.
    public void handleMousewheel(int x, int y, boolean north) {

    }

    // Useful for various things. 'y' renamed to 'i' to shut up warnings.
    public static int sensibleCellDiv(int i, int sz) {
        int r = i / sz;
        if (i < 0)
            r--;
        return r;
    }
}
