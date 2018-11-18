/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;


import gabien.IPeripherals;

/**
 * This was actually a totally different class at one point.
 * Now it's a superclass of UITextButton.
 * Unknown creation date.
 */
public abstract class UIButton<ThisClass extends UIButton> extends UIBorderedElement {
    public Runnable onClick;
    public double pressedTime = 0;
    public boolean state = false;
    public boolean toggle = false;

    public UIButton(int bw) {
        super(0, bw);
    }

    public ThisClass togglable(boolean st) {
        state = st;
        toggle = true;
        return (ThisClass) this;
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        if (pressedTime > 0) {
            pressedTime -= deltaTime;
            if (pressedTime <= 0)
                state = false;
        }
        borderType = state ? 1 : 0;
    }

    @Override
    public IPointerReceiver handleNewPointer(IPointer stat) {
        if (stat.getType() == IPointer.PointerType.Generic) {
            if (toggle) {
                state = !state;
            } else {
                enableStateForClick();
            }
            if (onClick != null)
                onClick.run();
        }
        return super.handleNewPointer(stat);
    }

    public void enableStateForClick() {
        state = true;
        pressedTime = 0.5;
    }
}
