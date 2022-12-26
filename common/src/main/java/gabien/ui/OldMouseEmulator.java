/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

/**
 * Used to speed up porting work or for elements that shouldn't ever need to properly support multi-touch.
 * Created on February 17th, 2018
 */
public class OldMouseEmulator implements IPointerReceiver {
    public final IOldMouseReceiver receiver;
    public IPointer pointer = null;
    public int mouseX, mouseY;

    public OldMouseEmulator(IOldMouseReceiver r) {
        receiver = r;
    }

    @Override
    public void handlePointerBegin(IPointer state) {
        if (pointer != null)
            receiver.handleRelease(pointer.getX(), pointer.getY());
        pointer = state;
        mouseX = state.getX();
        mouseY = state.getY();

        IPointer.PointerType pt = pointer.getType();
        int button = 0;
        if (pt == IPointer.PointerType.Generic)
            button = 1;
        if (pt == IPointer.PointerType.Middle)
            button = 2;
        if (pt == IPointer.PointerType.Right)
            button = 3;
        if (pt == IPointer.PointerType.X1)
            button = 4;
        if (pt == IPointer.PointerType.X2)
            button = 5;
        receiver.handleClick(pointer.getX(), pointer.getY(), button);
    }

    @Override
    public void handlePointerUpdate(IPointer state) {
        if (pointer == state) {
            mouseX = state.getX();
            mouseY = state.getY();
            receiver.handleDrag(state.getX(), state.getY());
        }
    }

    @Override
    public void handlePointerEnd(IPointer state) {
        if (pointer == state) {
            mouseX = state.getX();
            mouseY = state.getY();
            receiver.handleRelease(state.getX(), state.getY());
            pointer = null;
        }
    }

    public interface IOldMouseReceiver {
        void handleClick(int x, int y, int button);
        void handleDrag(int x, int y);
        void handleRelease(int x, int y);
    }

}
