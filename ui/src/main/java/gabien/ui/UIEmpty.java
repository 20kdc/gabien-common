/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.wsi.IPeripherals;

/**
 * Used for places where no element really needs to be.
 * Created 9th August, 2023.
 */
public class UIEmpty extends UIElement {
    public UIEmpty() {
        super(0, 0);
    }

    public UIEmpty(int w, int h) {
        super(w, h);
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
    }
}
