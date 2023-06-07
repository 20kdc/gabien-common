/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.backendhelp;

import java.util.LinkedList;

import gabien.IDesktopPeripherals;
import gabien.IGaBIEnMultiWindow;
import gabien.IGrInDriver;
import gabien.IImage;
import gabien.IPeripherals;
import gabien.PriorityElevatorForUseByBackendHelp;
import gabien.WindowSpecs;

/**
 * This 'mux' provides the illusion of multi-window support using:
 * 1. a single real window provided via the single-window API
 * 2. offscreen buffers
 * 3. lots and lots of fun regarding peripheral juggling
 * 
 * Created on 04/03/2020.
 */
public class WindowMux extends PriorityElevatorForUseByBackendHelp implements IGaBIEnMultiWindow {
    public IGrInDriver underlyingWindow;
    
    public LinkedList<Window> windows = new LinkedList<WindowMux.Window>();
    public LinkedList<Window> windowsSystem = new LinkedList<WindowMux.Window>();
    
    public WindowMux(IGrInDriver underlying) {
        underlyingWindow = underlying;
    }
    
    public Window getCurrentWindow() {
        if (!windowsSystem.isEmpty())
            return windowsSystem.getLast();
        if (!windows.isEmpty())
            return windows.getLast();
        return null;
    }
    
    @Override
    public boolean isActuallySingleWindow() {
        return true;
    }
    
    @Override
    public WindowSpecs defaultWindowSpecs(String name, int w, int h) {
        return newWindowSpecs();
    }
    
    @Override
    public IGrInDriver makeGrIn(String name, int w, int h, WindowSpecs windowspecs) {
        Window wnd = new Window(underlyingWindow.getWidth(), underlyingWindow.getHeight());
        Window oldWnd = getCurrentWindow();
        if (isOfSystemPriority(windowspecs)) {
            windowsSystem.add(wnd);
        } else {
            windows.add(wnd);
        }
        performWindowTransition(oldWnd);
        return wnd;
    }
    
    private void performWindowTransition(Window o) {
        Window n = getCurrentWindow();
        if (o == n)
            return;
        underlyingWindow.getPeripherals().clearKeys();
        if (o != null)
            o.windowPeripherals.target = DeadDesktopPeripherals.INSTANCE;
        if (n != null) {
            n.windowPeripherals.target = underlyingWindow.getPeripherals();
        }
        //System.out.println("-- Window transition " + o + " -> " + n + " --");
    }
    
    public class Window implements IGrInDriver {
        public boolean running = true;
        public final ProxyPeripherals<IPeripherals> windowPeripherals;
        
        @SuppressWarnings("unchecked")
        public Window(int w, int h) {
            if (underlyingWindow.getPeripherals() instanceof IDesktopPeripherals) {
                ProxyDesktopPeripherals<IDesktopPeripherals> wp = new ProxyDesktopPeripherals<IDesktopPeripherals>();
                wp.target = DeadDesktopPeripherals.INSTANCE;
                windowPeripherals = (ProxyPeripherals<IPeripherals>) (ProxyDesktopPeripherals<?>) wp;
            } else {
                windowPeripherals = new ProxyPeripherals<IPeripherals>();
                windowPeripherals.target = DeadDesktopPeripherals.INSTANCE;
            }
        }

        @Override
        public boolean stillRunning() {
            return running && underlyingWindow.stillRunning();
        }

        @Override
        public int getWidth() {
            return underlyingWindow.getWidth();
        }

        @Override
        public int getHeight() {
            return underlyingWindow.getHeight();
        }

        @Override
        public void flush(IImage backBuffer) {
            if (getCurrentWindow() == this)
                underlyingWindow.flush(backBuffer);
        }

        @Override
        public IPeripherals getPeripherals() {
            return windowPeripherals;
        }
        
        @Override
        public void shutdown() {
            if (running) {
                running = false;
                Window old = getCurrentWindow();
                windows.remove(this);
                windowsSystem.remove(this);
                performWindowTransition(old);
            }
        }
        
        @Override
        public int estimateUIScaleTenths() {
            return underlyingWindow.estimateUIScaleTenths();
        }
    }
}
