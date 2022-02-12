/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.backendhelp.ProxyGrDriver;
import gabien.ui.UIBorderedElement;

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
class GrInDriver extends ProxyGrDriver<IWindowGrBackend> implements IGrInDriver {
    public Frame frame; // Really a JFrame for better close handling.
    public Panel panel; // Actually a Panel because there's no point for this to be a JPanel.
    public TextboxMaintainer tm;
    public IPeripherals peripherals;
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

    Random fuzzer = new Random();

    public GrInDriver(String name, WindowSpecs ws, IWindowGrBackend t) {
        super(t);
        sc = ws.scale;
        frame = new JFrame(name) {
            @Override
            public void paint(Graphics graphics) {
                // nope!
                paintComponents(graphics);
            }
        };

        // Setup frontBuffer...

        int rw = t.getWidth();
        int rh = t.getHeight();

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
        if (UIBorderedElement.getBlackTextFlagWindowRoot())
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

        KeyListener commonKeyListener = new KeyListener() {

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

        tm = new TextboxMaintainer(panel, commonKeyListener);

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
        GaBIEnImpl.activeDriverLock.lock();
        GaBIEnImpl.activeDrivers.add(this);
        GaBIEnImpl.activeDriverLock.unlock();
    }

    @Override
    public boolean flush() {
        if (peripherals instanceof MobilePeripherals)
            ((MobilePeripherals) peripherals).mobilePeripheralsFinishFrame();

        // To explain what goes on here now in MT-mode:
        // 1. wait for current queue to complete
        // 2. finish render
        // 3. release queue for next render
        // Note that I did at one point plan to make it so this ran on a 1-frame delay...
        Runnable[] l = getLockingSequenceN();
        if (l != null)
            l[0].run();

        tm.newFrame();


        // Update frontBuffer for slowpaint, then perform fastpaint
        BufferedImage backBuffer = (BufferedImage) target.getNative();
        BufferedImage frontBuf = frontBuffer;
        int panelW = panel.getWidth();
        int panelH = panel.getHeight();
        if ((frontBuf.getWidth() != panelW) || (frontBuf.getHeight() != panelH)) {
            // Resize maybe needed?
            if (getWidth() != 0)
                if (getHeight() != 0)
                    frontBuf = new BufferedImage(panelW, panelH, BufferedImage.TYPE_INT_RGB);
        }
        Graphics fbG = frontBuf.getGraphics();
        fbG.setColor(panel.getBackground());
        fbG.fillRect(0, 0, frontBuf.getWidth(), frontBuf.getHeight());
        fbG.drawImage(backBuffer, 0, 0, backBuffer.getWidth() * sc, backBuffer.getHeight() * sc, null);

        // Change buffer if necessary
        frontBuffer = frontBuf;

        if (l != null)
            l[1].run();

        drawFrontBuffer(panel.getGraphics());

        int wantedRW = panelW / sc;
        int wantedRH = panelH / sc;

        boolean resized = false;
        if ((getWidth() != wantedRW) || (getHeight() != wantedRH)) {
            target.resize(wantedRW, wantedRH);
            resized = true;
        }

        return resized;
    }

    private void drawFrontBuffer(Graphics pg) {
        if (tm.maintainedString != null) {
            int txX = tm.target.getX();
            int txY = tm.target.getY();
            int txW = tm.target.getWidth();
            int txH = tm.target.getHeight();
            ClipBoundHelper cbh = new ClipBoundHelper();
            cbh.point(0, 0);
            cbh.point(getWidth() * sc, 0);
            cbh.point(0, getHeight() * sc);
            cbh.point(-(getWidth() * sc), 0);
            // Alternatively, maybe go up to 0, 0 then to txX, txY, and follow the points that way? depends on how much harm would be caused by non-orthogonal lines
            cbh.point(0, txY - (getHeight() * sc));
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
    public void shutdown() {
        GaBIEnImpl.activeDriverLock.lock();
        GaBIEnImpl.lastClosureDevice = frame.getGraphicsConfiguration().getDevice();
        GaBIEnImpl.activeDrivers.remove(this);
        GaBIEnImpl.activeDriverLock.unlock();
        super.shutdown();
        frame.setVisible(false);
    }

    private void fuzzXY() {
        // Emulate difficulties positioning correctly with a touch interface
        mouseX += fuzzer.nextInt(17) - 8;
        mouseY += fuzzer.nextInt(17) - 8;
    }

	@Override
	public int estimateUIScaleTenths() {
		return uiGuessScaleTenths;
	}
}
