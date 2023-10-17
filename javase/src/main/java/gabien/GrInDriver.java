/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.backend.WSIDownloadPair;
import gabien.render.IImage;
import gabien.render.WSIImage;
import gabien.uslx.append.Function;
import gabien.wsi.IGrInDriver;
import gabien.wsi.IPeripherals;
import gabien.wsi.ITextEditingSession;
import gabien.wsi.WindowSpecs;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wow, this code dates back a long time.
 * (See: very early versions of IkachanMapEdit)
 * (Though now it's been split up for OsbDriver - Jun 4 2017)
 */
class GrInDriver implements IGrInDriver {
    public Frame frame; // Really a JFrame for better close handling.
    public Panel panel; // Actually a Panel because there's no point for this to be a JPanel.
    public TextboxMaintainer currentEditingSession;
    public IGJSEPeripheralsInternal peripherals;
    public boolean[] keys = new boolean[IGrInDriver.KEYS];
    public boolean[] keysjd = new boolean[IGrInDriver.KEYS];
    public int sc;

    public static int uiGuessScaleTenths = 10;

    public ReentrantLock mouseLock = new ReentrantLock();
    public int mouseX = 0, mouseY = 0;
    public HashSet<Integer> mouseDown = new HashSet<Integer>();
    public HashSet<Integer> mouseJustDown = new HashSet<Integer>();
    public HashSet<Integer> mouseJustUp = new HashSet<Integer>();
    public int mousewheelMovements = 0;

    public BufferedImage frontBuffer;
    public int wantedBackBufferW, wantedBackBufferH;

    private final DLIAPair dlIA;
    private final DLBIPair dlBI;

    public final KeyListener commonKeyListener;

    Random fuzzer = new Random();

    @SuppressWarnings("serial")
    public GrInDriver(String name, WindowSpecs ws, int rw, int rh) {
        if (rw < 1)
            rw = 1;
        if (rh < 1)
            rh = 1;
        sc = ws.scale;
        frame = new JFrame(name) {
            @Override
            public void paint(Graphics graphics) {
                // nope!
                paintComponents(graphics);
            }
        };

        // Setup frontBuffer...

        wantedBackBufferW = rw;
        wantedBackBufferH = rh;

        frontBuffer = new BufferedImage(rw * sc, rh * sc, BufferedImage.TYPE_INT_RGB);

        panel = new Panel() {
            @Override
            public void paint(Graphics graphics) {
                // Nope, don't use the usual panel paint (which draws background).
                drawFrontBuffer(graphics);
                paintComponents(graphics);
            }
        };

        frame.setResizable(ws.resizable && (!ws.fullscreen));

        // Set the background because of resizing. Must occur before peers created.
        Color background = Color.black;
        // Actually in a light theme? Check w/ gabien.ui
        if (ws.backgroundLight)
            background = Color.white;
        // This sets OS-level stuff indirectly. It won't be enough, though.
        frame.setBackground(background);
        panel.setBackground(background);

        // Size elements & go...

        panel.setPreferredSize(new Dimension(rw * sc, rh * sc));
        frame.setSize(rw * sc, rh * sc);

        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent arg0) {

            }

            @Override
            public void focusLost(FocusEvent arg0) {
                for (int p = 0; p < keys.length; p++)
                    keys[p] = false;
            }
        });

        panel.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent me) {
            }

            @Override
            public void mousePressed(MouseEvent me) {
                mouseLock.lock();
                mouseX = me.getX() / sc;
                mouseY = me.getY() / sc;
                int mouseB = me.getButton();

                mouseDown.add(mouseB);
                mouseJustDown.add(mouseB);
                mouseLock.unlock();
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                mouseLock.lock();
                mouseX = me.getX() / sc;
                mouseY = me.getY() / sc;
                int mouseB = me.getButton();
                mouseDown.remove(mouseB);
                mouseJustUp.add(mouseB);
                mouseLock.unlock();
                // justdown is a click checker.
                // as in, even if the mouse is
                // released, register the click
            }

            @Override
            public void mouseEntered(MouseEvent me) {
            }

            @Override
            public void mouseExited(MouseEvent me) {
            }
        });
        panel.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent me) {
                mouseLock.lock();
                mouseX = me.getX() / sc;
                mouseY = me.getY() / sc;
                mouseLock.unlock();
            }

            @Override
            public void mouseMoved(MouseEvent me) {
                mouseLock.lock();
                mouseX = me.getX() / sc;
                mouseY = me.getY() / sc;
                if (mouseDown.size() > 0) {
                    mouseJustUp.addAll(mouseDown);
                    mouseDown.clear();
                }
                mouseLock.unlock();
            }
        });
        panel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
                mouseLock.lock();
                mousewheelMovements += mouseWheelEvent.getWheelRotation();
                mouseLock.unlock();
            }
        });

        commonKeyListener = new KeyListener() {

            @Override
            public void keyTyped(KeyEvent ke) {
            }

            public int[] keymap = {KeyEvent.VK_ESCAPE, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0, KeyEvent.VK_MINUS, KeyEvent.VK_EQUALS, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_TAB, KeyEvent.VK_Q, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_R, KeyEvent.VK_T, KeyEvent.VK_Y, KeyEvent.VK_U, KeyEvent.VK_I, KeyEvent.VK_O, KeyEvent.VK_P, KeyEvent.VK_OPEN_BRACKET, KeyEvent.VK_CLOSE_BRACKET, KeyEvent.VK_ENTER, KeyEvent.VK_CONTROL, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_F, KeyEvent.VK_G, KeyEvent.VK_H, KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_L, KeyEvent.VK_SEMICOLON, KeyEvent.VK_QUOTE,
                    // VK_HASH/VK_TILDE
                    0,
                    KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_SLASH, KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_B, KeyEvent.VK_N, KeyEvent.VK_M, KeyEvent.VK_COMMA, KeyEvent.VK_PERIOD, KeyEvent.VK_SLASH,
                    // VK_KP_MULTIPLY
                    0,
                    KeyEvent.VK_ALT, KeyEvent.VK_SPACE, KeyEvent.VK_CAPS_LOCK, KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4, KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8, KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_NUM_LOCK, KeyEvent.VK_SCROLL_LOCK, KeyEvent.VK_NUMPAD7, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9, 0, KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, 0, KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, KeyEvent.VK_NUMPAD0, KeyEvent.VK_PERIOD, KeyEvent.VK_F11, KeyEvent.VK_F12, 0, 0, KeyEvent.VK_ALT_GRAPH, 0, KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_INSERT,};

            public int filterKey(KeyEvent ke) {
                for (int p = 0; p < keymap.length; p++) {
                    if (ke.getKeyCode() == keymap[p])
                        return p;
                }
                return -1;
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                int KeyNum = filterKey(ke);
                if (KeyNum != -1) {
                    if (!keys[KeyNum])
                        keysjd[KeyNum] = true;
                    keys[KeyNum] = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {
                int KeyNum = filterKey(ke);
                if (KeyNum != -1)
                    keys[KeyNum] = false;
            }
        };

        frame.addKeyListener(commonKeyListener);
        panel.addKeyListener(commonKeyListener);

        // This serves as the new disambiguator between emulated-mobile & desktop.
        if (GaBIEnImpl.mobileEmulation) {
            peripherals = new MobilePeripherals(this);
        } else {
            peripherals = new DesktopPeripherals(this);
        }

        if (ws.fullscreen) {
            GraphicsDevice gd = GaBIEnImpl.getFSDevice();
            if (gd != null)
                gd.setFullScreenWindow(frame);
        }

        dlIA = new DLIAPair(name + ".dlIA");
        dlBI = new DLBIPair(name + ".dlBI");

        GaBIEnImpl.activeDriverLock.lock();
        GaBIEnImpl.activeDrivers.add(this);
        GaBIEnImpl.activeDriverLock.unlock();
    }

    @Override
    public int getWidth() {
        return wantedBackBufferW;
    }

    @Override
    public int getHeight() {
        return wantedBackBufferH;
    }

    @Override
    public void flush(IImage backBuffer) {
        synchronized (this) {
            if (peripherals instanceof MobilePeripherals)
                ((MobilePeripherals) peripherals).mobilePeripheralsFinishFrame(backBuffer);
            wantedBackBufferW = panel.getWidth() / sc;
            wantedBackBufferH = panel.getHeight() / sc;
        }

        backBuffer.batchFlush();
        // Flushes early and helps the profiler.
        GaBIEn.vopeks.putFinishTask();
        GaBIEn.vopeks.putBatchStatisticsTask();

        // To avoid deadlock, we don't want to be locked while doing waitingFrames
        final int[] ia = dlIA.acquire(backBuffer.getWidth(), backBuffer.getHeight());

        // Transfer.
        backBuffer.getPixelsAsync(ia, () -> {
            final AWTWSIImage bi = dlBI.acquire(backBuffer.getWidth(), backBuffer.getHeight());
            bi.setPixels(ia);
            dlIA.release(ia);
            drawBackBufferToFrontBuffer(bi);
            dlBI.release(bi);
        });    
    }

    private void drawBackBufferToFrontBuffer(WSIImage wsi) {
        synchronized (this) {
            final int panelW = panel.getWidth();
            final int panelH = panel.getHeight();
            // Update frontBuffer for slowpaint, then perform fastpaint
            BufferedImage backBufferBI = ((AWTWSIImage) wsi).buf;
            // Resize maybe needed?
            if ((frontBuffer.getWidth() != panelW) || (frontBuffer.getHeight() != panelH))
                if ((panelW != 0) && (panelH != 0))
                    frontBuffer = new BufferedImage(panelW, panelH, BufferedImage.TYPE_INT_RGB);
            Graphics fbG = frontBuffer.getGraphics();
            if (backBufferBI != null)
                fbG.drawImage(backBufferBI, 0, 0, backBufferBI.getWidth() * sc, backBufferBI.getHeight() * sc, null);
        }
        // little break here so that a waiting flush() can run housekeeping
        synchronized (this) {
            drawFrontBuffer(panel.getGraphics());
        }
    }

    private synchronized void drawFrontBuffer(Graphics pg) {
        if (currentEditingSession != null) {
            JComponent target = currentEditingSession.placeComponent;
            int txX = target.getX();
            int txY = target.getY();
            int txW = target.getWidth();
            int txH = target.getHeight();
            ClipBoundHelper cbh = new ClipBoundHelper();
            cbh.point(0, 0);
            int fbW = frontBuffer.getWidth();
            int fbH = frontBuffer.getHeight();
            cbh.point(fbW, 0);
            cbh.point(0, fbH);
            cbh.point(-fbW, 0);
            // Alternatively, maybe go up to 0, 0 then to txX, txY, and follow the points that way? depends on how much harm would be caused by non-orthogonal lines
            cbh.point(0, txY - fbH);
            cbh.point(txX, 0);
            cbh.point(0, txH);
            cbh.point(txW, 0);
            cbh.point(0, -txH);
            cbh.point(-(txX + txW), 0);
            pg.setClip(cbh.p);
            pg.drawImage(frontBuffer, 0, 0, null);
            pg.setClip(null);
        } else {
            pg.setClip(null);
            pg.drawImage(frontBuffer, 0, 0, null);
        }
    }

    @Override
    public IPeripherals getPeripherals() {
        return peripherals;
    }

    @Override
    public boolean stillRunning() {
        boolean res = frame.isVisible();
        if (!res)
            shutdown();
        return res;
    }

    @Override
    public synchronized void shutdown() {
        GaBIEnImpl.activeDriverLock.lock();
        GaBIEnImpl.lastClosureDevice = frame.getGraphicsConfiguration().getDevice();
        GaBIEnImpl.activeDrivers.remove(this);
        GaBIEnImpl.activeDriverLock.unlock();
        frame.setVisible(false);
    }

    // This was a "feature" for mobile testing meant to emulate the unreliability of precision inputs on touch devices.
    // Code is kept here in case it is for some reason required to resurrect it.
    @SuppressWarnings("unused")
    private void fuzzXY() {
        // Emulate difficulties positioning correctly with a touch interface.
        // This would have presumably been called just after mouseX/mouseY were set.
        mouseX += fuzzer.nextInt(17) - 8;
        mouseY += fuzzer.nextInt(17) - 8;
    }

	@Override
	public int estimateUIScaleTenths() {
		return uiGuessScaleTenths;
	}

    public ITextEditingSession openEditingSession(IGJSEPeripheralsInternal peripheralsInternal, boolean multiLine, int textHeight, Function<String, String> fun) {
        if (currentEditingSession != null)
            currentEditingSession.endSession();
        return currentEditingSession = new TextboxMaintainer(peripheralsInternal, panel, commonKeyListener, multiLine, textHeight, fun);
    }

    private class DLIAPair extends WSIDownloadPair<int[]> {
        public DLIAPair(String n) {
            // Any more than this and the lag becomes very obvious.
            // Remember, as long as vopeks_main is saturated, we're at our limit.
            super(n, 3);
        }

        @Override
        public boolean bufferMatchesSize(int[] buffer, int width, int height) {
            return buffer.length == (width * height);
        }
        @Override
        public int[] genBuffer(int width, int height) {
            return new int[width * height];
        }
    }

    private class DLBIPair extends WSIDownloadPair<AWTWSIImage> {
        public DLBIPair(String n) {
            super(n, 1);
        }

        @Override
        public boolean bufferMatchesSize(AWTWSIImage buffer, int width, int height) {
            return buffer.width == width && buffer.height == height;
        }
        @Override
        public AWTWSIImage genBuffer(int width, int height) {
            return new AWTWSIImage(null, width, height);
        }
    }
}
