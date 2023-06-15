/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IPeripherals;

import java.util.LinkedList;

import org.eclipse.jdt.annotation.Nullable;

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
public abstract class UIElement extends LAFChain {
    /**
     * Just so that this doesn't have to be repeated a thousand times...
     */
    public static final UILayer[] LAYERS = new UILayer[] {UILayer.Clear, UILayer.Base, UILayer.Content};

    private Rect elementBounds = Rect.ZERO;

    private UIElement parent;
    private Size wantedSize = Size.ZERO;
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
    // Used for textboxes
    private boolean attachedToRootFlag;

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

    /**
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

    /**
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

    public void renderLayer(IGrDriver igd, UILayer layer) {
        if (layer == UILayer.Content)
            render(igd);
    }

    public final void renderAllLayers(IGrDriver igd) {
        for (UILayer layer : LAYERS)
            renderLayer(igd, layer);
    }

    /**
     * DO NOT CALL THIS ANYMORE. Call one of:
     * 1. renderLayer
     * 2. renderAllLayers
     * DEPENDING ON USECASE.
     * You're still okay to override this if you want to draw on the Content layer.
     */
    protected void render(IGrDriver igd) {
        // Nothing here!
    }

    /**
     * How you should call runLayout.
     * Failure to call it this way can result in *stuff* not getting updated properly.
     * In particular, this is now NON-FINAL.
     */
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

    /**
     * This method *SHOULD* work like this:
     * 1. Get all wanted sizes
     * 2. Adjust controls inside
     * Step 2 will trigger "pleaseContinueLayingOut" to be set if necessary.
     * UNDER NO CIRCUMSTANCES should this be non-super-called anywhere except from UIElement,
     *  or classes with an understanding of what they're calling, i.e. their own runLayout method.
     */
    public void runLayout() {

    }

    // Only processed for window-level elements.
    public boolean requestsUnparenting() {
        return false;
    }

    // Also only processed for window-level elements.
    public void onWindowClose() {

    }

    /**
     * Internal parent attach/detach logic, ensures root attach/detach is correct
     */
    private void internalSetParent(UIElement newParent) {
        parent = newParent;
        if (newParent != null)
            setAttachedToRoot(newParent.attachedToRootFlag);
        else
            setAttachedToRoot(false);
        themeUpdate();
    }

    /**
     * Updates the attached root IGrInDriver of the element.
     * Important because of things like textboxes which need to know when they're being mucked with.
     * UNDER NO CIRCUMSTANCES should this be non-super-called anywhere except from UIElement.
     */
    public void setAttachedToRoot(boolean attached) {
        attachedToRootFlag = attached;
    }

    public final boolean getAttachedToRoot() {
        return attachedToRootFlag;
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

    public final UIElement getParent() {
        return parent;
    }

    @Override
    @Nullable
    LAFChain getLAFParentInternal() {
        LAFChain c1 = super.getLAFParentInternal();
        if (c1 != null)
            return c1;
        return parent;
    }

    /**
     * UIPanel is the basis of all layouts.
     */
    public static abstract class UIPanel extends UIElement {
        // Selection matters for textbox issues so make it easy to debug.
        private final static boolean debugSelection = false;

        private UIElement selectedElement;
        // DO NOT CHANGE WITHOUT SETTING ALLELEMENTSCHANGED. This will cause all sorts of fun and interesting bugs.
        // Much like how stepping in a particle accelerator will cause all sorts of fun and interesting effects on your body.
        private LinkedList<UIElement> allElements = new LinkedList<UIElement>();
        // As this can under some circumstances change, 'lock it in' with for (I i : a) syntax or a local variable before usage,
        //  and use recacheElements before that because it's a cache and might be out of date otherwise.
        private UIElement[] cachedAllElements;
        private boolean allElementsChanged = true;
        private boolean released = false;
        private final boolean panelScissors;

        public UIPanel() {
            this(false);
        }

        public UIPanel(int w, int h) {
            this(false, w, h);
        }

        public UIPanel(boolean scissors) {
            panelScissors = scissors;
        }

        public UIPanel(boolean scissors, int w, int h) {
            super(w, h);
            panelScissors = scissors;
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
            uie.internalSetParent(this);
            uie.visibleFlag = true;
            allElements.add(uie);
            allElementsChanged = true;
        }

        protected final void layoutRemoveElement(UIElement uie) {
            if (uie.parent != this)
                throw new RuntimeException("UIE " + uie + " already lost parent somehow in " + this);
            uie.internalSetParent(null);
            if (selectedElement == uie) {
                if (debugSelection)
                    System.err.println("Deselected " + uie.toString() + " due to removal.");
                selectedElement = null;
            }
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
            if (debugSelection)
                System.err.println("Selected " + uie.toString() + " by force.");
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
        public void renderLayer(IGrDriver igd, UILayer layer) {
            if (released)
                throw new RuntimeException("Trying to use released panel.");
            // javac appears to be having conflicting memories.
            // elementBounds: invalid (something about not being static)
            // this.elementBounds: invalid (access error)
            // myself.elementBounds: ok
            recacheElements();
            if (panelScissors) {
                for (UIElement uie : cachedAllElements)
                    if (uie.visibleFlag)
                        unscissoredRender(uie, igd, layer);
            } else {
                for (UIElement uie : cachedAllElements)
                    if (uie.visibleFlag)
                        scissoredRender(uie, igd, layer);
            }
        }

        @Override
        protected final void render(IGrDriver igd) {
            // Disabled to stop shenanigans
        }

        @Override
        public void setAttachedToRoot(boolean attached) {
            super.setAttachedToRoot(attached);
            recacheElements();
            for (UIElement uie : cachedAllElements)
                uie.setAttachedToRoot(attached);
        }

        public static void unscissoredRender(UIElement uie, IGrDriver igd, UILayer layer) {
            int x = uie.elementBounds.x;
            int y = uie.elementBounds.y;

            float[] trs = igd.getTRS();
            float osTX = trs[0];
            float osTY = trs[1];

            trs[0] += x;
            trs[1] += y;
            uie.renderLayer(igd, layer);

            trs[0] = osTX;
            trs[1] = osTY;
        }

        public static void scissoredRender(UIElement uie, IGrDriver igd, UILayer layer) {
            int x = uie.elementBounds.x;
            int y = uie.elementBounds.y;
            // Scissoring. The maths here is painful, and breaking it leads to funky visbugs.
            // YOU HAVE BEEN WARNED.
            int left = x;
            int top = y;
            int right = left + uie.elementBounds.width;
            int bottom = top + uie.elementBounds.height;

            float[] trs = igd.getTRS();
            int[] scissor = igd.getScissor();
            float osTX = trs[0];
            float osTY = trs[1];
            float osSX = trs[2];
            float osSY = trs[3];
            int osLeft = scissor[0];
            int osTop = scissor[1];
            int osRight = scissor[2];
            int osBottom = scissor[3];

            float scaledX = x * osSX;
            float scaledY = y * osSY;

            left = (int) Math.max(osTX + scaledX, Math.max(osLeft, 0));
            top = (int) Math.max(osTY + scaledY, Math.max(osTop, 0));
            right = (int) Math.min(osTX + (right * osSX), osRight);
            bottom = (int) Math.min(osTY + (bottom * osSY), osBottom);

            trs[0] += scaledX;
            trs[1] += scaledY;
            scissor[0] = left;
            scissor[1] = top;
            scissor[2] = Math.max(left, right);
            scissor[3] = Math.max(top, bottom);
            uie.renderLayer(igd, layer);

            trs[0] = osTX;
            trs[1] = osTY;
            scissor[0] = osLeft;
            scissor[1] = osTop;
            scissor[2] = osRight;
            scissor[3] = osBottom;
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
                if (!uie.visibleFlag)
                    continue;
                if (uie.elementBounds.contains(state.getX(), state.getY())) {
                    int x = uie.elementBounds.x;
                    int y = uie.elementBounds.y;
                    state.performOffset(-x, -y);
                    IPointerReceiver ipr = uie.handleNewPointer(state);
                    state.performOffset(x, y);
                    if (ipr != null) {
                        selectedElement = uie;
                        if (debugSelection)
                            System.err.println("Selected " + uie.toString() + " by pointer.");
                        return new IPointerReceiver.TransformingElementPointerReceiver(selectedElement, ipr);
                    }
                }
            }
            if (debugSelection)
                System.err.println("Deselected by pointer.");
            // Returns null so that UIGrid & such can use that to mean 'not handled by existing elements'.
            return null;
        }

        // Used by some UI stuff that needs to reuse elements.
        public void release() {
            for (UIElement uie : allElements)
                uie.internalSetParent(null);
            allElements.clear();
            allElementsChanged = true;
            released = true;
        }

        @Override
        public void themeUpdateChildren() {
            recacheElements();
            for (UIElement uie : cachedAllElements)
                uie.themeUpdate();
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
            currentElement.internalSetParent(this);
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
        public void renderLayer(IGrDriver igd, UILayer layer) {
            currentElement.renderLayer(igd, layer);
        }

        @Override
        protected final void render(IGrDriver igd) {
            // Disabled to stop shenanigans
        }

        @Override
        public void setAttachedToRoot(boolean attached) {
            super.setAttachedToRoot(attached);
            currentElement.setAttachedToRoot(attached);
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
            currentElement.internalSetParent(null);
        }

        @Override
        public void onWindowClose() {
            currentElement.onWindowClose();
        }

        @Override
        public void themeUpdateChildren() {
            currentElement.themeUpdate();
        }
    }
}
