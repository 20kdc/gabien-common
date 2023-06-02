/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives.examples;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import gabien.natives.BadGPU;
import gabien.natives.BadGPUEnum.MetaInfoType;
import gabien.natives.BadGPUEnum.TextureLoadFormat;
import gabien.uslx.append.ThreadOwned;

/**
 * An abomination trying to test better ways of handling graphical output.
 * Created 30th May, 2023.
 */
public class Main {
    private volatile int currentCanvasWidth, currentCanvasHeight;
    private volatile BadGPU.Texture screen1;
    private volatile int[] dataSrcA, dataSrcB, dataSrc;
    boolean flipper1 = false;
    private volatile int currentBufferWidth, currentBufferHeight;
    public final BadGPU.Instance instance;
    private final ThreadOwned.Locked instanceLock;
    private final Thread gameThread;
    public IState currentState = new StateMenu();
    public volatile boolean shutdown;
    private final Semaphore frameRequestSemaphore = new Semaphore(1);
    private final Semaphore frameCompleteSemaphore = new Semaphore(1);
    private Timer t;
    private volatile BufferedImage transferBuffer;
    private volatile WritableRaster transferBufferWR;

    public static final int KEY_W = 0;
    public static final int KEY_A = 1;
    public static final int KEY_S = 2;
    public static final int KEY_D = 3;
    public static final int KEY_Z = 4;
    public static final int KEY_X = 5;
    public static final int KEY_SPACE = 6;
    public boolean[] keysEvent = new boolean[7];
    public boolean[] keys = new boolean[7];

    public Main() {
        instance = BadGPU.newInstance(BadGPU.NewInstanceFlags.BackendCheck | BadGPU.NewInstanceFlags.CanPrintf);
        instanceLock = new ThreadOwned.Locked(instance.syncObject);
        try (ThreadOwned.Locked tmp = instanceLock) {
            System.out.println("init: vendor: " + instance.getMetaInfo(MetaInfoType.Vendor));
            System.out.println("init: renderer: " + instance.getMetaInfo(MetaInfoType.Renderer));
            System.out.println("init: version: " + instance.getMetaInfo(MetaInfoType.Version));
        }
        // Do this early 
        frameCompleteSemaphore.acquireUninterruptibly();
        gameThread = new Thread("BadGPU/Game Thread") {
            @Override
            public void run() {
                try (ThreadOwned.Locked tmp = instanceLock.open()) {
                    while (!shutdown) {
                        frameRequestSemaphore.acquireUninterruptibly();
                        if (shutdown)
                            break;
                        // figure out resize
                        int clampedCW = currentCanvasWidth;
                        int clampedCH = currentCanvasHeight;
                        if (clampedCW < 1)
                            clampedCW = 1;
                        if (clampedCW > 32767)
                            clampedCW = 32767;
                        if (clampedCH < 1)
                            clampedCH = 1;
                        if (clampedCH > 32767)
                            clampedCH = 32767;
                        if (screen1 == null || clampedCW != currentBufferWidth || clampedCH != currentBufferHeight) {
                            System.out.println(" -- RECREATING TEXTURE --");
                            screen1 = instance.newTexture(BadGPU.TextureFlags.HasAlpha, clampedCW, clampedCH);
                            dataSrcA = new int[clampedCW * clampedCH];
                            dataSrcB = new int[clampedCW * clampedCH];
                            currentBufferWidth = clampedCW;
                            currentBufferHeight = clampedCH;
                        }
                        // transfer the last frame
                        long tA, tB;
                        tA = System.currentTimeMillis();
                        instance.flush();
                        dataSrc = flipper1 ? dataSrcA : dataSrcB;
                        screen1.readPixels(0, 0, currentBufferWidth, currentBufferHeight, TextureLoadFormat.ARGBI32, dataSrc, 0);
                        tB = System.currentTimeMillis();
                        frameCompleteSemaphore.release();
                        //System.out.println("from TT");
                        System.out.println(tB - tA);
                        // continue...
                        currentState.frame(Main.this, screen1, currentBufferWidth, currentBufferHeight);
                        flipper1 = !flipper1;
                        instance.flush();
                    }
                }
            }
        };
        gameThread.start();
    }

    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        System.setProperty("sun.awt.erasebackgroundonresize", "true");
        // "native" double buffering takes us off of the VSync path
        System.setProperty("awt.nativeDoubleBuffering", "false");
        // but unless this is set, we don't get double buffering at all!
        System.setProperty("swing.bufferPerWindow", "true");
        // doing this should help perf right
        // NO NO NO NO NO
        // System.setProperty("sun.java2d.opengl", "True");
        gabien.natives.Loader.defaultLoader();
        final Main m = new Main();
        // need to use a JFrame to get VSync
        final JFrame w = new JFrame("gabien-natives examples");
        w.addKeyListener(new KeyAdapter() {
            private int translateKey(int kc) {
                if (kc == KeyEvent.VK_W)
                    return KEY_W;
                if (kc == KeyEvent.VK_A)
                    return KEY_A;
                if (kc == KeyEvent.VK_S)
                    return KEY_S;
                if (kc == KeyEvent.VK_D)
                    return KEY_D;
                if (kc == KeyEvent.VK_Z)
                    return KEY_Z;
                if (kc == KeyEvent.VK_X)
                    return KEY_X;
                if (kc == KeyEvent.VK_SPACE)
                    return KEY_SPACE;
                return -1;
            }
            @Override
            public void keyPressed(KeyEvent e) {
                int tk = translateKey(e.getKeyCode());
                if (tk == -1)
                    return;
                m.keys[tk] = true;
                m.keysEvent[tk] = true;
            }
            @Override
            public void keyReleased(KeyEvent e) {
                int tk = translateKey(e.getKeyCode());
                if (tk == -1)
                    return;
                m.keys[tk] = false;
            }
        });
        // This is the not *really* documented way you set client size in AWT.
        w.setPreferredSize(new Dimension(800, 600));
        // w.getRootPane().setDoubleBuffered(true);
        System.out.println("DBB: " + w.getRootPane().isDoubleBuffered());
        @SuppressWarnings("serial")
        final JComponent canvas = new JComponent() {
            @Override
            public void paint(Graphics var1) {
                if (m.transferBuffer != null) {
                    // now we do the thing
                    long tA = System.currentTimeMillis();
                    var1.drawImage(m.transferBuffer, 0, 0, null);
                    long tB = System.currentTimeMillis();
                    System.out.println("DI:" + (tB - tA));
                }
            }
            @Override
            public boolean isOpaque() {
                return true;
            }
        };
        w.getRootPane().setDoubleBuffered(true);
        canvas.setDoubleBuffered(true);
        RepaintManager.currentManager(w).setDoubleBufferingEnabled(true);
        // What the hell, Sun?
        try {
            Class.forName("com.sun.java.swing.SwingUtilities3").getMethod("setVsyncRequested", Container.class, boolean.class).invoke(null, w, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        w.setContentPane(canvas);
        w.pack();
        w.setVisible(true);
        w.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                w.dispose();
                System.exit(1);
            }
        });
        m.t = new Timer();
        m.t.scheduleAtFixedRate(new TimerTask() {
            long lastFrameTime = 0;
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    // ensure a frame has completed before continuing...
                    long thisFrameTime = System.currentTimeMillis();
                    System.out.println("FT: " + (thisFrameTime - lastFrameTime));
                    lastFrameTime = thisFrameTime;
                    m.frameCompleteSemaphore.acquireUninterruptibly();
                    // this means the game thread is now waiting for us to schedule a new frame
                    int cw = canvas.getWidth();
                    int ch = canvas.getHeight();
                    m.currentCanvasWidth = cw;
                    m.currentCanvasHeight = ch;
                    int sw = m.currentBufferWidth;
                    int sh = m.currentBufferHeight;
                    // the game thread picks a buffer to read into, reads into it, then sets this
                    // then *after* we release the FRS, it changes this and reads into the *other* buffer
                    int[] grabbedDS = m.dataSrc;
                    m.frameRequestSemaphore.release();
                    if (grabbedDS != null) {
                        if (m.transferBuffer == null || m.transferBuffer.getWidth() != sw || m.transferBuffer.getHeight() != sh) {
                            m.transferBuffer = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                            m.transferBufferWR = m.transferBuffer.getRaster();
                        }
                        long tA = System.currentTimeMillis();
                        // that I have to do this to avoid a slowpath is so stupid
                        m.transferBufferWR.setDataElements(0, 0, sw, sh, grabbedDS);
                        long tB = System.currentTimeMillis();
                        System.out.println("SDE:" + (tB - tA));
                    }
                    canvas.paintImmediately(0, 0, cw, ch);
                });
            }
        }, 0, 15);
    }
    private static final float[] triImmDat1 = new float[24];
    public static void triImm(BadGPU.Texture scr, int w, int h,
            float xA, float yA,
            float rA, float gA, float bA,
            float xB, float yB,
            float rB, float gB, float bB,
            float xC, float yC,
            float rC, float gC, float bC
        ) {
        float[] data = triImmDat1;
        data[0] = xA;
        data[1] = yA;
        data[2] = 0;
        data[3] = 1;
        data[4] = xB;
        data[5] = yB;
        data[6] = 0;
        data[7] = 1;
        data[8] = xC;
        data[9] = yC;
        data[10] = 0;
        data[11] = 1;
        data[12] = rA;
        data[13] = gA;
        data[14] = bA;
        data[15] = 1;
        data[16] = rB;
        data[17] = gB;
        data[18] = bB;
        data[19] = 1;
        data[20] = rC;
        data[21] = gC;
        data[22] = bC;
        data[23] = 1;
        BadGPU.drawGeomNoDS(scr, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0,
                0,
                data, 0, data, 12, null, 0,
                BadGPU.PrimitiveType.Triangles, 0,
                0, 3, null, 0,
                null, 0, null, 0,
                0, 0, w, h,
                null, null, 0,
                0,
                BadGPU.BlendWeight.Zero, BadGPU.BlendWeight.Zero, BadGPU.BlendEquation.Add,
                BadGPU.BlendWeight.Zero, BadGPU.BlendWeight.Zero, BadGPU.BlendEquation.Add);
    }
}
