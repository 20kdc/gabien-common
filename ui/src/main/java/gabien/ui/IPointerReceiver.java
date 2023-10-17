/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.uslx.append.*;
import gabien.wsi.IPointer;

/**
 * Another part of multi-touch stuff.
 * Imagine, if you will, the scene in Elephant's Dream where there are many plugs, and many sockets,
 *  and the plugs fly across the room to connect to the sockets.
 * This is used like that. Particularly, note there is only ever one plug in a given socket.
 */
public interface IPointerReceiver {
    void handlePointerBegin(IPointer state);
    void handlePointerUpdate(IPointer state);
    void handlePointerEnd(IPointer state);

    class NopPointerReceiver implements IPointerReceiver {
        @Override
        public void handlePointerBegin(IPointer state) {
        }

        @Override
        public void handlePointerUpdate(IPointer state) {
        }

        @Override
        public void handlePointerEnd(IPointer state) {
        }
    }

    class TransformingElementPointerReceiver implements IPointerReceiver {
        public final UIElement element;
        public final IPointerReceiver ipr;

        public TransformingElementPointerReceiver(UIElement uie, IPointerReceiver i) {
            element = uie;
            ipr = i;
        }

        @Override
        public void handlePointerBegin(IPointer state) {
            Rect r = element.getParentRelativeBounds();
            state.performOffset(-r.x, -r.y);
            ipr.handlePointerBegin(state);
            state.performOffset(r.x, r.y);
        }

        @Override
        public void handlePointerUpdate(IPointer state) {
            Rect r = element.getParentRelativeBounds();
            state.performOffset(-r.x, -r.y);
            ipr.handlePointerUpdate(state);
            state.performOffset(r.x, r.y);
        }

        @Override
        public void handlePointerEnd(IPointer state) {
            Rect r = element.getParentRelativeBounds();
            state.performOffset(-r.x, -r.y);
            ipr.handlePointerEnd(state);
            state.performOffset(r.x, r.y);
        }
    }

    class RelativeResizePointerReceiver implements IPointerReceiver {
        public final Size firstSize;
        public int xSt, ySt;
        public final Consumer<Size> consumer;
        public Size lastSize;
        public RelativeResizePointerReceiver(int w, int h, Consumer<Size> iConsumer) {
            lastSize = firstSize = new Size(w, h);
            consumer = iConsumer;
        }

        @Override
        public void handlePointerBegin(IPointer state) {
            xSt = state.getX();
            ySt = state.getY();
        }

        @Override
        public void handlePointerUpdate(IPointer state) {
            consumer.accept(lastSize = new Size((state.getX() - xSt) + firstSize.width, (state.getY() - ySt) + firstSize.height));
        }

        @Override
        public void handlePointerEnd(IPointer state) {
            handlePointerUpdate(state);
        }
    }
}
