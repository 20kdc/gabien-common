/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package gabien.ui;

/**
 * This class is for an up or down mouse event.
 * Created on 11/08/17.
 */
public class MouseAction {
    // button is indexed based at 0 - events with a button of -1 are synthetic (UIWindowView internal, usually)
    //  and should stay within the class that generated them.
    // Note that button is always 0 for mouse up events (to prevent over-reliance on buttons)
    public final int x, y, button;
    public final boolean down;
    public MouseAction(int tx, int ty, int tb, boolean d) {
        x = tx;
        y = ty;
        button = tb;
        down = d;
    }
    public MouseAction transform(int baseX, int baseY) {
        return new MouseAction(x - baseX, y - baseY, button, down);
    }
}
