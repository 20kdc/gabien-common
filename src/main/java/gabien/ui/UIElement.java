/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IGrInDriver;
import gabien.IPeripherals;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.WeakHashMap;

/**
 * Once a simple class. Now it's not.
 *
 *  R48 2018 UI system redesign notes:
 *  This code is in a state of continual war with all other code.
 *  It assumes total consistency with itself, and complete inconsistency of everything around it.
 *  This is intentional.
 *
 * Redesigned on February 16th, 2018.
 */
public abstract class UIElement implements IPointerReceiver {
    private Rect elementBounds = new Rect(0, 0, 0, 0);

    private UIElement parent;
    private Size wantedSize = new Size(0, 0);
    // Used during construction & setForcedBounds.
    // In the first case, this prevents accidentally calling runLayout before the object is ready.
    // In the second case, this prevents a sub-element from calling a parent element's runLayout...
    //  during that same runLayout.
    private boolean duringSetForcedBounds = false;

    public UIElement() {
        // This is sufficient.
    }

    public UIElement(int width, int height) {
        // Simplifies things.
        Rect sz = new Rect(0, 0, width, height);
        duringSetForcedBounds = true;
        setWantedSize(sz);
        setForcedBounds(null, sz);
        duringSetForcedBounds = false;
    }

    /*
     * Sets boundaries that this element MUST comply with.
     */
    public final void setForcedBounds(UIElement mustBeThis, Rect r) {
        if (mustBeThis != parent)
            throw new RuntimeException("You aren't allowed to do that!");
        elementBounds = r;
        // Oh, *this* is a mindbender.
        if (!duringSetForcedBounds) {
            duringSetForcedBounds = true;
            runLayout();
            duringSetForcedBounds = false;
        }
    }

    public final Rect getParentRelativeBounds() {
        return elementBounds;
    }

    public final Size getSize() {
        return elementBounds;
    }

    public void setWantedSize(Size size) {
        boolean relayout = !wantedSize.sizeEquals(size);
        wantedSize = size;
        if (!duringSetForcedBounds)
            if (relayout)
                if (parent != null)
                    parent.runLayout();
    }

    public final Size getWantedSize() {
        return wantedSize;
    }

    public abstract void update(double deltaTime);
    public abstract void render(boolean selected, IPeripherals peripherals, IGrDriver igd);

    public void runLayout() {

    }

    // Only processed for window-level elements.
    public boolean requestsUnparenting() {
        return false;
    }

    // If this occurs, the containing window has been closed.
    public void handleRootDisconnect() {

    }

    // Almost never used. Doesn't follow the normal system, shouldn't have to.
    public void handleMousewheel(int x, int y, boolean north) {

    }

    @Override
    public void handlePointerBegin(IPointer state) {

    }

    @Override
    public void handlePointerUpdate(IPointer state) {

    }

    @Override
    public void handlePointerEnd(IPointer state) {

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

    /**
     * UIPanel is the basis of all layouts.
     */
    public static abstract class UIPanel extends UIElement {
        private UIElement selectedElement;
        private LinkedList<UIElement> allElements = new LinkedList<UIElement>();
        private HashSet<UIElement> visElements = new HashSet<UIElement>();
        private WeakHashMap<IPointer, UIElement> pointerClickMapping = new WeakHashMap<IPointer, UIElement>();

        public UIPanel() {

        }

        public UIPanel(int w, int h) {
            super(w, h);
        }

        protected final void layoutAddElement(UIElement uie) {
            if (uie.parent != null)
                throw new RuntimeException("UIE " + uie + " already has parent " + uie.parent + " in " + this);
            uie.parent = this;
            allElements.add(uie);
            visElements.add(uie);
        }

        protected final void layoutRemoveElement(UIElement uie) {
            if (uie.parent == null)
                throw new RuntimeException("UIE " + uie + " already lost parent somehow in " + this);
            uie.parent = null;
            if (selectedElement == uie)
                selectedElement = null;
            allElements.remove(uie);
            visElements.remove(uie);
            uie.handleRootDisconnect();
        }

        protected final void layoutSetElementVis(UIElement uie, boolean visible) {
            if (!allElements.contains(uie))
                throw new RuntimeException("Can't set visibility of an element we don't have in " + this);
            if (visible) {
                visElements.add(uie);
            } else {
                visElements.remove(uie);
            }
        }

        protected final boolean layoutContainsElement(UIElement uie) {
            return allElements.contains(uie);
        }

        protected final boolean layoutElementVisible(UIElement uie) {
            return visElements.contains(uie);
        }

        protected final void layoutSelect(UIElement uie) {
            if (uie != null)
                if (!allElements.contains(uie))
                    throw new RuntimeException("Can't select something we don't have.");
            selectedElement = uie;
        }

        protected final LinkedList<UIElement> layoutGetElements() {
            return new LinkedList<UIElement>(allElements);
        }

        @Override
        public void update(double deltaTime) {
            for (UIElement uie : new LinkedList<UIElement>(allElements))
                uie.update(deltaTime);
        }

        @Override
        public void render(boolean selected, IPeripherals peripherals, IGrDriver igd) {
            // javac appears to be having conflicting memories.
            // elementBounds: invalid (something about not being static)
            // this.elementBounds: invalid (access error)
            // myself.elementBounds: ok
            UIElement myself = this;
            for (UIElement uie : new LinkedList<UIElement>(visElements))
                scissoredRender(false, uie, selected && (uie == selectedElement), peripherals, igd, myself.elementBounds.width, myself.elementBounds.height);
        }

        public static void scissoredRender(boolean asWindow, UIElement uie, boolean selected, IPeripherals mouse, IGrDriver igd, int w, int h) {
            int x = uie.elementBounds.x;
            int y = uie.elementBounds.y;
            // Scissoring. The maths here is painful, and breaking it leads to funky visbugs.
            // YOU HAVE BEEN WARNED.
            int left = x;
            int top = y;
            int right = left + uie.elementBounds.width;
            int bottom = top + uie.elementBounds.height;

            int[] localBuffer = igd.getLocalST();
            int osTX = localBuffer[0];
            int osTY = localBuffer[1];
            int osLeft = localBuffer[2];
            int osTop = localBuffer[3];
            int osRight = localBuffer[4];
            int osBottom = localBuffer[5];

            left = Math.max(osTX + left, Math.max(osLeft, 0));
            top = Math.max(osTY + top, Math.max(osTop, 0));
            right = Math.min(osTX + right, Math.min(osTX + w, osRight));
            bottom = Math.min(osTY + bottom, Math.min(osTY + h, osBottom));

            mouse.performOffset(-x, -y);

            localBuffer[0] += x;
            localBuffer[1] += y;
            localBuffer[2] = left;
            localBuffer[3] = top;
            localBuffer[4] = right;
            localBuffer[5] = bottom;
            igd.updateST();
            if (asWindow)
                UIBorderedElement.drawBorder(igd, 5, 4, 0, 0, uie.elementBounds.width, uie.elementBounds.height);
            uie.render(selected, mouse, igd);

            localBuffer[0] = osTX;
            localBuffer[1] = osTY;
            localBuffer[2] = osLeft;
            localBuffer[3] = osTop;
            localBuffer[4] = osRight;
            localBuffer[5] = osBottom;
            igd.updateST();

            mouse.performOffset(x, y);
        }

        // This is quite an interesting one, because I've made it abstract here but not abstract in the parent.
        @Override
        public abstract void runLayout();

        @Override
        public void handleRootDisconnect() {
            super.handleRootDisconnect();
            for (UIElement uie : allElements)
                uie.handleRootDisconnect();
        }

        @Override
        public void handleMousewheel(int x, int y, boolean north) {
            for (UIElement uie : allElements) {
                Rect r = uie.getParentRelativeBounds();
                if (r.contains(x, y))
                    uie.handleMousewheel(x - r.x, y - r.y, north);
            }
        }

        @Override
        public void handlePointerBegin(IPointer state) {
            selectedElement = null;
            for (UIElement uie : allElements) {
                if (uie.elementBounds.contains(state.getX(), state.getY())) {
                    selectedElement = uie;
                    int x = selectedElement.elementBounds.x;
                    int y = selectedElement.elementBounds.y;
                    state.performOffset(-x, -y);
                    selectedElement.handlePointerBegin(state);
                    pointerClickMapping.put(state, selectedElement);
                    state.performOffset(x, y);
                    return;
                }
            }
        }

        @Override
        public void handlePointerUpdate(IPointer state) {
            UIElement uie = pointerClickMapping.get(state);
            if (uie != null) {
                if (allElements.contains(uie)) {
                    int x = uie.elementBounds.x;
                    int y = uie.elementBounds.y;
                    state.performOffset(-x, -y);
                    uie.handlePointerUpdate(state);
                    state.performOffset(x, y);
                }
            }
        }

        @Override
        public void handlePointerEnd(IPointer state) {
            UIElement uie = pointerClickMapping.get(state);
            if (uie != null) {
                if (allElements.contains(uie)) {
                    int x = uie.elementBounds.x;
                    int y = uie.elementBounds.y;
                    state.performOffset(-x, -y);
                    uie.handlePointerEnd(state);
                    state.performOffset(x, y);
                }
            }
            pointerClickMapping.remove(state);
        }
    }

    /**
     * UIProxy is the basis of "controller" layouts, where the actual layout is provided by something else.
     * A UIProxy thus has one and only one element in it, ever.
     * However, this is not set up as *final*, as final variable constructor + inheritance...
     * ...is incredibly inconvenient to use.
     */
    public static class UIProxy extends UIElement {
        private UIElement currentElement;

        protected final void proxySetElement(UIElement element, boolean wanted) {
            if (currentElement != null)
                throw new RuntimeException("Cannot ever add an element more than once to a proxy.");
            if (element == null)
                throw new RuntimeException("Cannot add null as the element of a proxy.");
            currentElement = element;
            currentElement.parent = this;
            if (wanted) {
                currentElement.runLayout();
                currentElement.setForcedBounds(this, new Rect(currentElement.getWantedSize()));
                currentElement.runLayout();
            }
            // As this is meant to be called during the constructor, we take on the proxy's size,
            //  not the other way around. Also, if this fails with a "you can't do that",
            //  I'm not sure what you're trying to do, but it sounds fun!
            setForcedBounds(null, new Rect(wanted ? currentElement.getWantedSize() : element.getSize()));
            setWantedSize(currentElement.getWantedSize());
        }

        protected final UIElement proxyGetElement() {
            return currentElement;
        }

        @Override
        public void update(double deltaTime) {
            currentElement.update(deltaTime);
        }

        @Override
        public void render(boolean selected, IPeripherals peripherals, IGrDriver igd) {
            currentElement.render(selected, peripherals, igd);
        }

        @Override
        public void runLayout() {
            currentElement.setForcedBounds(this, new Rect(getSize()));
            setWantedSize(currentElement.getWantedSize());
        }

        @Override
        public boolean requestsUnparenting() {
            return currentElement.requestsUnparenting();
        }

        @Override
        public void handleRootDisconnect() {
            currentElement.handleRootDisconnect();
        }

        @Override
        public void handleMousewheel(int x, int y, boolean north) {
            currentElement.handleMousewheel(x, y, north);
        }

        @Override
        public void handlePointerBegin(IPointer state) {
            currentElement.handlePointerBegin(state);
        }

        @Override
        public void handlePointerUpdate(IPointer state) {
            currentElement.handlePointerUpdate(state);
        }

        @Override
        public void handlePointerEnd(IPointer state) {
            currentElement.handlePointerEnd(state);
        }
    }
}
