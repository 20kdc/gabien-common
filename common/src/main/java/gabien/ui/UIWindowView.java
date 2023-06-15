/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IPeripherals;
import gabien.IPointer;
import gabien.ui.theming.Theme;

import java.util.LinkedList;
import java.util.Random;
import gabien.uslx.append.*;

/**
 * NOTE: You have to implement your environment, and stuff like closing a window, on top of this.
 * However, TabShell should be a good base, implementing most things you could want.
 * Created on 12/27/16. Revamped on December 15th, 2017.
 * Ported on February 17th, 2018, as part of what I'm now calling "Project IPCRESS" for no discernible reason.
 * (Oh, shush, if you were doing this you'd go mad too.)
 * Revamped yet again ('Shell') starting November 13th, 2018.
 */
public class UIWindowView extends UIElement {

    // Rather than use the 'upcoming' system, this system is used instead:
    private final LinkedList<IShell> desktop = new LinkedList<IShell>();
    // Entirely visual.
    public IShell selectedWindow;
    private IShell[] desktopCache = new IShell[0];
    private boolean desktopChanged = false;

    private boolean clearKeysLater = false;

    public int windowTextHeight = 12;
    public int sizerVisual = 3;
    public int sizerActual = 8;

    public int getWindowFrameHeight() {
        return UITabBar.getHeight(windowTextHeight);
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        if (clearKeysLater) {
            peripherals.clearKeys();
            clearKeysLater = false;
        }
        updateDesktopCache();
        for (IShell shl : desktopCache)
            shl.update(deltaTime, selected, peripherals);
    }

    @Override
    public void render(IGrDriver igd) {
        updateDesktopCache();
        for (IShell shl : desktopCache)
            shl.render(igd);
    }

    private void updateDesktopCache() {
        if (desktopChanged) {
            desktopCache = desktop.toArray(new IShell[0]);
            desktopChanged = false;
        }
    }

    @Override
    public IPointerReceiver handleNewPointer(IPointer state) {
        updateDesktopCache();
        IShell[] array = desktopCache;
        for (int i = array.length - 1; i >= 0; i--) {
            IPointerReceiver ipr = array[i].provideReceiver(state);
            if (ipr != null)
                return ipr;
        }
        return null;
    }

    @Override
    public void handleMousewheel(final int x, final int y, boolean north) {
        updateDesktopCache();
        IShell[] array = desktopCache;
        for (int i = array.length - 1; i >= 0; i--)
            if (array[i].handleMousewheel(x, y, north))
                return;
    }

    public void addShell(IShell t) {
        t.setAttachedToRoot(getAttachedToRoot());
        desktop.add(t);
        desktopChanged = true;
    }

    public void removeShell(IShell t) {
        removeShell(t, RemoveReason.Manual);
    }

    public void removeShell(IShell t, RemoveReason selfDestruct) {
        desktop.remove(t);
        desktopChanged = true;
        t.setAttachedToRoot(false);
        t.removed(selfDestruct);
    }

    public void raiseShell(IShell t) {
        selectedWindow = t;
        desktop.remove(t);
        desktop.add(t);
        desktopChanged = true;
    }

    public void lowerShell(IShell t) {
        desktop.remove(t);
        desktop.addFirst(t);
        desktopChanged = true;
    }

    public void removeTab(UITabBar.Tab win) {
        removeTab(win, RemoveReason.Manual);
    }

    public void removeTab(UITabBar.Tab win, RemoveReason selfDestruct) {
        updateDesktopCache();
        for (IShell s : desktopCache) {
        	// Not unlikely because TabShell exists.
        	// That said, this used to use .equals, which makes no sense but isn't really wrong
            if (s == win) {
                removeShell(s, selfDestruct);
                return;
            }
        }
    }

    public void cleanup() {
        updateDesktopCache();
        for (IShell s : desktopCache)
            removeShell(s, RemoveReason.Cleanup);
        updateDesktopCache();
    }

    @Override
    public void onWindowClose() {
        cleanup();
    }

    public LinkedList<IShell> getShells() {
        return new LinkedList<IShell>(desktop);
    }

    @Override
    public void setAttachedToRoot(boolean attached) {
        super.setAttachedToRoot(attached);
        updateDesktopCache();
        for (IShell s : desktopCache)
            s.setAttachedToRoot(attached);
    }

    // Represents a surface that controls its own position and has complex hit detection.
    // Must be an interface so that an extension of Tab can implement it.
    public interface IShell {
        // Called in front-to-back order. The first Shell to provide a non-null receiver wins.
        IPointerReceiver provideReceiver(IPointer i);

        boolean handleMousewheel(int x, int y, boolean north);

        void render(IGrDriver igd);

        void update(double deltaTime, boolean parentSelected, IPeripherals peripherals);

        void removed(RemoveReason reason);

        void setAttachedToRoot(boolean attached);
    }

    public enum RemoveReason {
        Cleanup,
        RequestedUnparent,
        Manual
    }

    public static class TabShell extends UITabBar.Tab implements IShell {
        public final UIWindowView parent;
        public boolean removed = false;

        public TabShell(UIWindowView p, UIElement contents, UITabBar.TabIcon[] icons) {
            super(contents, icons);
            parent = p;

            finishInit();
        }

        private void finishInit() {
            Rect r = contents.getParentRelativeBounds();
            Size g = parent.getSize();
            int areaX = g.width - r.width;
            if (areaX < 0)
                areaX = 0;
            int cX = new Random().nextInt(areaX + 1);
            int tabShellNewAreaForgiveness = parent.windowTextHeight * 6;
            if ((g.height - tabShellNewAreaForgiveness) < r.height) {
                // out of room, start crunching
                contents.setForcedBounds(null, new Rect(cX, 0, r.width, g.height - parent.getWindowFrameHeight()));
            } else {
                contents.setForcedBounds(null, new Rect(cX, tabShellNewAreaForgiveness, r.width, r.height));
            }

            windowBoundsCheck();
        }

        @Override
        public IPointerReceiver provideReceiver(IPointer i) {
            final Rect r = contents.getParentRelativeBounds();
            int fh = parent.getWindowFrameHeight();
            Rect mainframe = new Rect(r.x - parent.sizerActual, r.y - (parent.sizerActual + fh), r.width + (parent.sizerActual * 2), r.height + (parent.sizerActual * 2) + fh);
            Rect framebar = new Rect(r.x, r.y - fh, r.width, fh);
            int x = i.getX();
            int y = i.getY();
            if (framebar.contains(x, y)) {
                parent.selectedWindow = this;
                parent.raiseShell(this);
                if (!UITabBar.clickInTab(this, x - framebar.x, y - framebar.y, framebar.width, fh))
                    return new IPointerReceiver.RelativeResizePointerReceiver(r.x, r.y, new IConsumer<Size>() {
                        @Override
                        public void accept(Size size) {
                            Size cs = contents.getSize();
                            if (!removed)
                                contents.setForcedBounds(null, new Rect(size.width, size.height, cs.width, cs.height));
                            windowBoundsCheck();
                        }
                    });
                return new IPointerReceiver.NopPointerReceiver();
            } else if (r.contains(x, y)) {
                parent.selectedWindow = this;
                parent.raiseShell(this);
                i.performOffset(-r.x, -r.y);
                IPointerReceiver ipr = contents.handleNewPointer(i);
                i.performOffset(r.x, r.y);
                if (ipr != null)
                    return new IPointerReceiver.TransformingElementPointerReceiver(contents, ipr);
                return new IPointerReceiver.NopPointerReceiver();
            } else if (mainframe.contains(x, y)) {
                parent.selectedWindow = this;
                parent.raiseShell(this);
                int tx = x - mainframe.x;
                int ty = y - mainframe.y;
                int third = Math.max(parent.sizerActual, Math.min(mainframe.width / 3, mainframe.height / 3));
                int ttx = 0;
                int tty = 0;
                int rw = 0;
                int rh = 0;
                if (tx < third) {
                    ttx = -1;
                    rw = r.x;
                } else if (tx >= mainframe.width - third) {
                    ttx = 1;
                    rw = r.width;
                }
                if (ty < third) {
                    tty = -1;
                    rh = r.y;
                } else if (ty >= mainframe.height - third) {
                    tty = 1;
                    rh = r.height;
                }
                final int tfx = ttx;
                final int tfy = tty;
                return new IPointerReceiver.RelativeResizePointerReceiver(rw, rh, new IConsumer<Size>() {
                    @Override
                    public void accept(Size size) {
                        Rect basis = contents.getParentRelativeBounds();
                        int resX = basis.x;
                        int resY = basis.y;
                        int resW = basis.width;
                        int resH = basis.height;
                        if (tfx == -1) {
                            resX = size.width;
                            resW = (r.x + r.width) - size.width;
                        } else if (tfx == 1) {
                            resW = size.width;
                        }
                        if (tfy == -1) {
                            resY = size.height;
                            resH = (r.y + r.height) - size.height;
                        } else if (tfy == 1) {
                            resH = size.height;
                        }
                        if (!removed)
                            contents.setForcedBounds(null, new Rect(resX, resY, resW, resH));
                    }
                });
            }
            return null;
        }

        @Override
        public boolean handleMousewheel(int x, int y, boolean north) {
            final Rect r = contents.getParentRelativeBounds();
            int fh = parent.getWindowFrameHeight();
            // Note: mainframe is used for most pointers because of the way sizer hit-detection works.
            Rect visframe = new Rect(r.x - parent.sizerVisual, r.y - (parent.sizerVisual + fh), r.width + (parent.sizerVisual * 2), r.height + (parent.sizerVisual * 2) + fh);
            if (visframe.contains(x, y)) {
                parent.selectedWindow = this;
                parent.raiseShell(this);
                if (r.contains(x, y))
                    contents.handleMousewheel(x - r.x, y - r.y, north);
                return true;
            }
            return false;
        }

        @Override
        public void render(IGrDriver igd) {
            int windowFrameHeight = parent.getWindowFrameHeight();

            Rect b = contents.getParentRelativeBounds();

            Theme theme = contents.getTheme();
            UIBorderedElement.drawBorder(theme, igd, Theme.B_WINDOW, parent.sizerVisual, b.x - parent.sizerVisual, b.y - (windowFrameHeight + parent.sizerVisual), b.width + (parent.sizerVisual * 2), b.height + (windowFrameHeight + (parent.sizerVisual * 2)));

            boolean winSelected = parent.selectedWindow == this;
            UITabBar.drawTab(theme, winSelected ? Theme.B_TITLESEL : Theme.B_TITLE, b.x, b.y - windowFrameHeight, b.width, windowFrameHeight, igd, contents.toString(), this);

            for (UILayer layer : UIElement.LAYERS)
                UIPanel.scissoredRender(contents, igd, layer);
        }

        @Override
        public void update(double deltaTime, boolean parentSelected, IPeripherals peripherals) {
            // Only really needed in case of parent resize
            windowBoundsCheck();
            boolean requestedUnparenting = contents.requestsUnparenting();
            if (requestedUnparenting) {
                parent.removeShell(this, RemoveReason.RequestedUnparent);
            } else {
                Rect r = contents.getParentRelativeBounds();

                peripherals.performOffset(-r.x, -r.y);
                contents.update(deltaTime, (parent.selectedWindow == this) && parentSelected, peripherals);
                peripherals.performOffset(r.x, r.y);
            }
        }

        @Override
        public void removed(RemoveReason destroy) {
            removed = true;
            contents.setAttachedToRoot(false);
            if (destroy != RemoveReason.Manual)
                contents.onWindowClose();
        }

        public void windowBoundsCheck() {
            int fh = parent.getWindowFrameHeight();
            Size scr = parent.getSize();
            Rect s = contents.getParentRelativeBounds();
            int ox = s.x;
            int oy = s.y;
            if (ox < 0)
                ox = 0;
            if (ox > (scr.width - s.width))
                ox = scr.width - s.width;
            if (oy < fh)
                oy = fh;
            if (oy > scr.height)
                oy = scr.height;
            int ow = s.width;
            int oh = s.height;
            // Can only happen at this point if forced left by above code?
            if (ox < 0) {
                ow += ox;
                ox = 0;
            }
            ow = Math.max(ow, UITabBar.getTabWidth(this, 0, fh));
            oh = Math.max(oh, 0);
            if ((ox != s.x) || (oy != s.y) || (ow != s.width) || (oh != s.height))
                contents.setForcedBounds(null, new Rect(ox, oy, ow, oh));
        }

        @Override
        public void setAttachedToRoot(boolean attached) {
            contents.setAttachedToRoot(attached);
        }
    }

    public static class ElementShell implements IShell {
        public final UIWindowView parent;
        public final UIElement uie;

        public ElementShell(UIWindowView parent, UIElement element) {
            this.parent = parent;
            uie = element;
        }

        @Override
        public IPointerReceiver provideReceiver(IPointer i) {
            Rect bounds = uie.getParentRelativeBounds();
            if (!bounds.contains(i.getX(), i.getY()))
                return null;
            i.performOffset(-bounds.x, -bounds.y);
            IPointerReceiver ipr = uie.handleNewPointer(i);
            i.performOffset(bounds.x, bounds.y);
            if (ipr == null)
                return null;
            // ElementShell & ScreenShell do NOT raise themselves.
            parent.selectedWindow = this;
            return new IPointerReceiver.TransformingElementPointerReceiver(uie, ipr);
        }

        @Override
        public boolean handleMousewheel(int x, int y, boolean north) {
            Rect r = uie.getParentRelativeBounds();
            if (r.contains(x, y)) {
                parent.selectedWindow = this;
                uie.handleMousewheel(x - r.x, y - r.y, north);
                return true;
            }
            return false;
        }

        @Override
        public void render(IGrDriver igd) {
            for (UILayer layer : UIElement.LAYERS)
                UIPanel.scissoredRender(uie, igd, layer);
        }

        @Override
        public void update(double deltaTime, boolean parentSelected, IPeripherals peripherals) {
            boolean requestedUnparenting = uie.requestsUnparenting();
            if (requestedUnparenting) {
                parent.removeShell(this, RemoveReason.RequestedUnparent);
            } else {
                Rect r = uie.getParentRelativeBounds();

                peripherals.performOffset(-r.x, -r.y);
                uie.update(deltaTime, (parent.selectedWindow == this) && parentSelected, peripherals);
                peripherals.performOffset(r.x, r.y);
            }
        }

        @Override
        public void removed(RemoveReason reason) {
            if (reason != RemoveReason.Manual)
                uie.onWindowClose();
        }

        @Override
        public void setAttachedToRoot(boolean attached) {
            uie.setAttachedToRoot(attached);
        }
    }

    public static class ScreenShell extends ElementShell {
        public ScreenShell(UIWindowView parent, UIElement element) {
            super(parent, element);
            uie.setForcedBounds(null, new Rect(parent.getSize()));
        }

        private void updateSize() {
            Size gs = parent.getSize();
            if (!gs.sizeEquals(uie.getSize()))
                uie.setForcedBounds(null, new Rect(gs));
        }

        @Override
        public void render(IGrDriver igd) {
            updateSize();
            super.render(igd);
        }

        @Override
        public void update(double deltaTime, boolean parentSelected, IPeripherals peripherals) {
            updateSize();
            super.update(deltaTime, parentSelected, peripherals);
        }
    }
}
