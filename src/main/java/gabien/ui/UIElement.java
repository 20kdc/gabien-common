/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;

import java.util.HashMap;
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

    private UIPanel parent;
    private Size wantedSize = new Size(0, 0);

    public UIElement() {
        // This is sufficient.
    }

    public UIElement(int width, int height) {
        // Simplifies things.
        Rect sz = new Rect(0, 0, width, height);
        setWantedSize(sz);
        setForcedBounds(null, sz);
    }

    /*
     * Sets boundaries that this element MUST comply with.
     */
    public final void setForcedBounds(UIElement mustBeThis, Rect r) {
        if (mustBeThis != parent)
            throw new RuntimeException("You aren't allowed to do that!");
        elementBounds = r;
        // Oh, *this* is a mindbender
        if (this instanceof UIPanel)
            ((UIPanel) this).needsLayout = true;
    }

    public final Rect getParentRelativeBounds() {
        return elementBounds;
    }

    public final Size getSize() {
        return elementBounds;
    }

    public final void setWantedSize(Size size) {
        boolean relayout = !wantedSize.equals(size);
        wantedSize = size;
        if (relayout)
            if (parent != null)
                parent.needsLayout = true;
    }

    public final Size getWantedSize() {
        return wantedSize;
    }

    public abstract void update(double deltaTime);
    public abstract void render(boolean selected, IPointer mouse, IGrInDriver igd);

    @Override
    public void handlePointerBegin(IPointer state) {

    }

    @Override
    public void handlePointerUpdate(IPointer state) {

    }

    @Override
    public void handlePointerEnd(IPointer state) {

    }

    // Almost never used. Doesn't follow the normal system, shouldn't have to.
    public void handleMousewheel(int x, int y, boolean north) {

    }

    // Only processed for window-level elements.
    public boolean requestsUnparenting() {
        return false;
    }

    // If this occurs, the containing window has been closed.
    public void handleRootDisconnect() {

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
        private boolean needsLayout = false;
        private WeakHashMap<IPointer, UIElement> pointerClickMapping = new WeakHashMap<IPointer, UIElement>();

        public UIPanel() {

        }

        public UIPanel(int w, int h) {
            super(w, h);
        }

        protected void layoutAddElement(UIElement uie) {
            if (uie.parent != null)
                throw new RuntimeException("UIE " + uie + " already has parent " + uie.parent + " in " + this);
            uie.parent = this;
            allElements.add(uie);
            visElements.add(uie);
        }

        protected void layoutRemoveElement(UIElement uie) {
            if (uie.parent == null)
                throw new RuntimeException("UIE " + uie + " already lost parent somehow in " + this);
            uie.parent = null;
            if (selectedElement == uie)
                selectedElement = null;
            allElements.remove(uie);
            visElements.remove(uie);
            uie.handleRootDisconnect();
        }

        protected void layoutSetElementVis(UIElement uie, boolean visible) {
            if (!allElements.contains(uie))
                throw new RuntimeException("Can't set visibility of an element we don't have in " + this);
            if (visible) {
                visElements.add(uie);
            } else {
                visElements.remove(uie);
            }
        }

        protected boolean layoutContainsElement(UIElement uie) {
            return allElements.contains(uie);
        }

        protected boolean layoutElementVisible(UIElement uie) {
            return visElements.contains(uie);
        }

        protected LinkedList<UIElement> layoutGetElements() {
            return new LinkedList<UIElement>(allElements);
        }

        public abstract void runLayout();

        @Override
        public void update(double deltaTime) {
            if (needsLayout) {
                needsLayout = false;
                runLayout();
            }
            for (UIElement uie : visElements)
                uie.update(deltaTime);
        }

        @Override
        public void render(boolean selected, IPointer mouse, IGrInDriver igd) {
            // javac appears to be having conflicting memories.
            // elementBounds: invalid (something about not being static)
            // this.elementBounds: invalid (access error)
            // myself.elementBounds: ok
            UIElement myself = this;
            for (UIElement uie : visElements)
                scissoredRender(false, uie, selected && (uie == selectedElement), mouse, igd, myself.elementBounds.width, myself.elementBounds.height);
        }

        public static void scissoredRender(boolean asWindow, UIElement uie, boolean selected, IPointer mouse, IGrInDriver igd, int w, int h) {
            int x = uie.elementBounds.x;
            int y = uie.elementBounds.y;
            // How many pixels to cut off?
            int wp = Math.max((x + uie.elementBounds.width) - w, 0);
            int hp = Math.max((y + uie.elementBounds.height) - h, 0);

            mouse.performOffset(-x, -y);
            // make sure x/y aren't escaping negative
            int ex = x;
            int ey = y;
            if (ex < 0) {
                wp -= ex;
                ex = 0;
            }
            if (ey < 0) {
                hp -= ex;
                ey = 0;
            }
            wp = Math.min(w, wp);
            hp = Math.min(h, hp);
            igd.adjustScissoring(ex, ey, -wp, -hp);
            if (asWindow)
                UIBorderedElement.drawBorder(igd, 5, 4, uie.elementBounds.width, uie.elementBounds.height);
            uie.render(selected, mouse, igd);
            igd.adjustScissoring(ex, ey, wp, hp);
            mouse.performOffset(x, y);
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
    }
}
