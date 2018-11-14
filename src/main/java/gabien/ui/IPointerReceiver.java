/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import java.util.WeakHashMap;

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

    // Implements the standard click-lock behavior in a multi-touch environment,
    //  WITHOUT CAUSING PEOPLE TO LOSE ALL THEIR HAIR.
    // Additional benefit, actually cleans up UIWindowView's mouse code.
    class PointerConnector implements IPointerReceiver {
        public WeakHashMap<IPointer, IPointerReceiver> receiverMap = new WeakHashMap<IPointer, IPointerReceiver>();
        public IFunction<IPointer, IPointerReceiver> generateReceivers;
        public PointerConnector(IFunction<IPointer, IPointerReceiver> f) {
            generateReceivers = f;
        }

        @Override
        public void handlePointerBegin(IPointer state) {
            if (receiverMap.containsKey(state)) {
                receiverMap.remove(state);
                System.err.println("gabien.ui: Got a pointerbegin from a pointer in use. Be afraid.");
            }
            IPointerReceiver ipr = generateReceivers.apply(state);
            if (ipr != null) {
                ipr.handlePointerBegin(state);
                receiverMap.put(state, ipr);
            }
        }

        @Override
        public void handlePointerUpdate(IPointer state) {
            IPointerReceiver ipr = receiverMap.get(state);
            if (ipr != null)
                ipr.handlePointerUpdate(state);
        }

        @Override
        public void handlePointerEnd(IPointer state) {
            IPointerReceiver ipr = receiverMap.get(state);
            if (ipr != null)
                ipr.handlePointerEnd(state);
        }
    }

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
        public TransformingElementPointerReceiver(UIElement uie) {
            element = uie;
        }

        @Override
        public void handlePointerBegin(IPointer state) {
            Rect r = element.getParentRelativeBounds();
            state.performOffset(-r.x, -r.y);
            element.handlePointerBegin(state);
            state.performOffset(r.x, r.y);
        }

        @Override
        public void handlePointerUpdate(IPointer state) {
            Rect r = element.getParentRelativeBounds();
            state.performOffset(-r.x, -r.y);
            element.handlePointerUpdate(state);
            state.performOffset(r.x, r.y);
        }

        @Override
        public void handlePointerEnd(IPointer state) {
            Rect r = element.getParentRelativeBounds();
            state.performOffset(-r.x, -r.y);
            element.handlePointerEnd(state);
            state.performOffset(r.x, r.y);
        }
    }

    class RelativeResizePointerReceiver implements IPointerReceiver {
        public final Size firstSize;
        public int xSt, ySt;
        public final IConsumer<Size> consumer;
        public Size lastSize;
        public RelativeResizePointerReceiver(int w, int h, IConsumer<Size> iConsumer) {
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
