/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IPeripherals;

import java.util.LinkedList;

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
public abstract class UIElement {
    private Rect elementBounds = new Rect(0, 0, 0, 0);

    private UIElement parent;
    private Size wantedSize = new Size(0, 0);
    // Used during construction & setForcedBounds.
    // In the first case, this prevents accidentally calling runLayout before the object is ready.
    // In the second case, this prevents a sub-element from calling a parent element's runLayout...
    //  during that same runLayout.
    private boolean currentlyLayouting = false;
    // Fun fact: This confuses IDEA and/or the compiler. It thinks this is convertable to a local,
    //  but what happens here is that a runLayoutLoop indirectly (via setWantedSize in a subelement) causes another runLayoutLoop call
    //  in the same instance, which sets the variable to true, signalling to the first call that we need to keep going.
    private boolean weNeedToKeepLayouting = false;
    // Used rather than a HashSet.
    private boolean visibleFlag = false;

    public UIElement() {
        // This is sufficient.
    }

    public UIElement(int width, int height) {
        // Simplifies things.
        Rect sz = new Rect(0, 0, width, height);
        currentlyLayouting = true;
        setWantedSize(sz);
        setForcedBounds(null, sz);
        currentlyLayouting = false;
    }

    /*
     * Sets boundaries that this element MUST comply with.
     */
    public final void setForcedBounds(UIElement mustBeThis, Rect r) {
        if (mustBeThis != parent)
            throw new RuntimeException("You aren't allowed to do that, must be " + parent);
        boolean relayout = !r.sizeEquals(elementBounds);
        elementBounds = r;
        // Oh, *this* is a mindbender.
        if (relayout)
            runLayoutLoop();
    }

    /*
     * Forces the element to be at 0, 0 and with the optimal size.
     * Since this messes with layout, it can't be used while a parent is allowed.
     */
    public final void forceToRecommended() {
        runLayoutLoop();
        // Now that the wanted size is non-zero, let it stabilize
        Size lastWantedSize = getWantedSize();
        for (int i = 0; i < 16; i++) {
            setForcedBounds(null, new Rect(lastWantedSize));
            Size nextWantedSize = getWantedSize();
            if (lastWantedSize.sizeEquals(nextWantedSize))
                break;
            lastWantedSize = nextWantedSize;
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
        // The !currentlyLayouting is balanced out in runLayoutLoop
        if (relayout && !currentlyLayouting)
            if (parent != null)
                parent.runLayoutLoop();
    }

    public final Size getWantedSize() {
        return wantedSize;
    }

    public abstract void update(double deltaTime, boolean selected, IPeripherals peripherals);
    public abstract void render(IGrDriver igd);

    // How you should call runLayout.
    // Failure to call it this way can result in *stuff* not getting updated properly.
    // In particular, this is now NON-FINAL.
    public void runLayoutLoop() {
        Size oldWS = getWantedSize();
        if (currentlyLayouting) {
            weNeedToKeepLayouting = true;
            return;
        }
        currentlyLayouting = true;
        weNeedToKeepLayouting = true;
        for (int i = 0; i < 16; i++) {
            if (!weNeedToKeepLayouting)
                break;
            weNeedToKeepLayouting = false;
            runLayout();
        }
        if (weNeedToKeepLayouting) {
            System.err.println("UI: weNeedToKeepLayouting overload @ " + this.getClass() + " ( " + this + " )!");
            weNeedToKeepLayouting = false;
        }
        currentlyLayouting = false;
        if (!oldWS.sizeEquals(getWantedSize()))
            if (parent != null)
                parent.runLayoutLoop();
    }

    // This method *SHOULD* work like this:
    // 1. Get all wanted sizes
    // 2. Adjust controls inside
    // Step 2 will trigger "pleaseContinueLayingOut" to be set if necessary.
    // UNDER NO CIRCUMSTANCES should this be non-super-called anywhere except from UIElement,
    //  or classes with an understanding of what they're calling, i.e. their own runLayout method.
    public void runLayout() {

    }

    // Only processed for window-level elements.
    public boolean requestsUnparenting() {
        return false;
    }

    // Also only processed for window-level elements.
    public void onWindowClose() {

    }

    // Almost never used. Doesn't follow the normal system, shouldn't have to.
    public void handleMousewheel(int x, int y, boolean north) {

    }

    // Attempts to assign the pointer or returns null otherwise.
    // Null will cause further element checks, so be sure this is what you want.
    public IPointerReceiver handleNewPointer(IPointer state) {
        return new IPointerReceiver.NopPointerReceiver();
    }

    // Useful for various things. 'y' renamed to 'i' to shut up warnings.
    public static int sensibleCellDiv(int i, int sz) {
        if (i < 0)
            i -= (sz - 1);
        return i / sz;
    }

    // Java's definition of % is weird.
    // Right now not optimizing since I don't know which operators I can trust.
    public static int sensibleCellMod(int i, int sz) {
        return i - (sensibleCellDiv(i, sz) * sz);
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    public UIElement getParent() {
        return parent;
    }

    /**
     * UIPanel is the basis of all layouts.
     */
    public static abstract class UIPanel extends UIElement {
        private UIElement selectedElement;
        // DO NOT CHANGE WITHOUT SETTING ALLELEMENTSCHANGED. This will cause all sorts of fun and interesting bugs.
        // Much like how stepping in a particle accelerator will cause all sorts of fun and interesting effects on your body.
        private LinkedList<UIElement> allElements = new LinkedList<UIElement>();
        // As this can under some circumstances change, 'lock it in' with for (I i : a) syntax or a local variable before usage,
        //  and use recacheElements before that because it's a cache and might be out of date otherwise.
        private UIElement[] cachedAllElements;
        private boolean allElementsChanged = true;
        private boolean released = false;

        public UIPanel() {

        }

        public UIPanel(int w, int h) {
            super(w, h);
        }

        private void recacheElements() {
            if (allElementsChanged) {
                cachedAllElements = allElements.toArray(new UIElement[0]);
                allElementsChanged = false;
            }
        }

        protected final void layoutAddElement(UIElement uie) {
            if (uie.parent != null)
                throw new RuntimeException("UIE " + uie + " already has parent " + uie.parent + " in " + this);
            uie.parent = this;
            uie.visibleFlag = true;
            allElements.add(uie);
            allElementsChanged = true;
        }

        protected final void layoutRemoveElement(UIElement uie) {
            if (uie.parent != this)
                throw new RuntimeException("UIE " + uie + " already lost parent somehow in " + this);
            uie.parent = null;
            if (selectedElement == uie)
                selectedElement = null;
            allElements.remove(uie);
            allElementsChanged = true;
        }

        protected final void layoutSetElementVis(UIElement uie, boolean visible) {
            if (uie.parent != this)
                throw new RuntimeException("Can't set visibility of an element " + uie + " we don't have in " + this);
            uie.visibleFlag = visible;
        }

        protected final boolean layoutContainsElement(UIElement uie) {
            return uie.parent == this;
        }

        protected final boolean layoutElementVisible(UIElement uie) {
            if (uie.parent != this)
                throw new RuntimeException("Can't get visibility of an element " + uie + " we don't have in " + this);
            return uie.visibleFlag;
        }

        protected final void layoutSelect(UIElement uie) {
            if (uie != null)
                if (uie.parent != this)
                    throw new RuntimeException("Can't select something " + uie + " we " + this + " don't have.");
            selectedElement = uie;
        }

        protected final LinkedList<UIElement> layoutGetElements() {
            return new LinkedList<UIElement>(allElements);
        }

        @Override
        public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
            if (released)
                throw new RuntimeException("Trying to use released panel.");
            recacheElements();
            for (UIElement uie : cachedAllElements) {
                int x = uie.elementBounds.x;
                int y = uie.elementBounds.y;
                peripherals.performOffset(-x, -y);
                uie.update(deltaTime, selected && (selectedElement == uie), peripherals);
                peripherals.performOffset(x, y);
            }
        }

        @Override
        public void render(IGrDriver igd) {
            if (released)
                throw new RuntimeException("Trying to use released panel.");
            // javac appears to be having conflicting memories.
            // elementBounds: invalid (something about not being static)
            // this.elementBounds: invalid (access error)
            // myself.elementBounds: ok
            recacheElements();
            for (UIElement uie : cachedAllElements)
                if (uie.visibleFlag)
                    scissoredRender(uie, igd);
        }

        public static void scissoredRender(UIElement uie, IGrDriver igd) {
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
            right = Math.min(osTX + right, osRight);
            bottom = Math.min(osTY + bottom, osBottom);

            localBuffer[0] += x;
            localBuffer[1] += y;
            localBuffer[2] = left;
            localBuffer[3] = top;
            localBuffer[4] = Math.max(left, right);
            localBuffer[5] = Math.max(top, bottom);
            igd.updateST();
            uie.render(igd);

            localBuffer[0] = osTX;
            localBuffer[1] = osTY;
            localBuffer[2] = osLeft;
            localBuffer[3] = osTop;
            localBuffer[4] = osRight;
            localBuffer[5] = osBottom;
            igd.updateST();
        }

        // This is quite an interesting one, because I've made it abstract here but not abstract in the parent.
        @Override
        public abstract void runLayout();

        @Override
        public void handleMousewheel(int x, int y, boolean north) {
            recacheElements();
            for (UIElement uie : cachedAllElements) {
                Rect r = uie.elementBounds;
                if (r.contains(x, y)) {
                    uie.handleMousewheel(x - r.x, y - r.y, north);
                    return;
                }
            }
        }

        @Override
        public IPointerReceiver handleNewPointer(IPointer state) {
            selectedElement = null;
            recacheElements();
            for (UIElement uie : cachedAllElements) {
                if (uie.elementBounds.contains(state.getX(), state.getY())) {
                    int x = uie.elementBounds.x;
                    int y = uie.elementBounds.y;
                    state.performOffset(-x, -y);
                    IPointerReceiver ipr = uie.handleNewPointer(state);
                    state.performOffset(x, y);
                    if (ipr != null) {
                        selectedElement = uie;
                        return new IPointerReceiver.TransformingElementPointerReceiver(selectedElement, ipr);
                    }
                }
            }
            // Returns null so that UIGrid & such can use that to mean 'not handled by existing elements'.
            return null;
        }

        // Used by some UI stuff that needs to reuse elements.
        public void release() {
            for (UIElement uie : allElements)
                uie.parent = null;
            allElements.clear();
            allElementsChanged = true;
            released = true;
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

        public UIProxy() {

        }

        public UIProxy(UIElement element, boolean wanted) {
            proxySetElement(element, wanted);
        }

        protected final void proxySetElement(UIElement element, boolean wanted) {
            if (currentElement != null)
                throw new RuntimeException("Cannot ever add an element more than once to a proxy.");
            if (element == null)
                throw new RuntimeException("Cannot add null as the element of a proxy.");
            currentElement = element;
            if (wanted)
                currentElement.forceToRecommended();
            currentElement.parent = this;
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
        public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
            currentElement.update(deltaTime, selected, peripherals);
        }

        @Override
        public void render(IGrDriver igd) {
            currentElement.render(igd);
        }

        @Override
        public void runLayout() {
            boolean cannotSFB = currentElement.getSize().sizeEquals(getSize());
            if (!cannotSFB)
                currentElement.setForcedBounds(this, new Rect(getSize()));
            currentElement.runLayoutLoop();
            setWantedSize(currentElement.getWantedSize());
        }

        @Override
        public void runLayoutLoop() {
            runLayout();
        }

        @Override
        public boolean requestsUnparenting() {
            return currentElement.requestsUnparenting();
        }

        @Override
        public void handleMousewheel(int x, int y, boolean north) {
            currentElement.handleMousewheel(x, y, north);
        }

        @Override
        public IPointerReceiver handleNewPointer(IPointer state) {
            return currentElement.handleNewPointer(state);
        }

        public void release() {
            if (currentElement.parent != this)
                throw new RuntimeException("Cannot release twice.");
            currentElement.parent = null;
        }

        @Override
        public void onWindowClose() {
            currentElement.onWindowClose();
        }
    }
}
