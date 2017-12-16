/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;

public abstract class UIElement {
    private Rect elementBounds = new Rect(0, 0, 1, 1);

    // Notably, it's perfectly safe to mess with the inputs and outputs of these functions
    public void setBounds(Rect r) {
        elementBounds = new Rect(r.x, r.y, r.width, r.height);
    }

    public Rect getBounds() {
        return new Rect(elementBounds.x, elementBounds.y, elementBounds.width, elementBounds.height);
    }

    public abstract void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd);

    // Mouse events *always* follow the following order:
    // Click, Drag, Release.
    // Drag may be omitted, but click and release must not.
    // Only one click/drag/release cycle may be active at a time
    // (this is an assumption that is made everywhere, and drastically simplifies implementation)

    public void handleClick(int x, int y, int button) {

    }

    public void handleDrag(int x, int y) {

    }

    public void handleRelease(int x, int y) {

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

    @Override
    public String toString() {
        return getClass().getName();
    }
}
