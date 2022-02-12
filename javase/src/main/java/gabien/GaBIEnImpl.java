/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.ui.IConsumer;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The important parts of the GaBIEn implementation.
 * Created sometime in the past.
 */
public class GaBIEnImpl implements IGaBIEn, IGaBIEnMultiWindow, IGaBIEnFileBrowser {
    public static boolean mobileEmulation;

    // Can be read by an application to get the global UI scale.
    // On mobile devices, this should be done based on the pixel resolution, NOT DPI.
    // This is due to practicality issues.
    // On desktop devices, this should work based on the external UI scale.
    protected static int uiGuessScaleTenths = 10;

    private HashMap<String, IImage> loadedImages = new HashMap<String, IImage>();

    protected static ReentrantLock activeDriverLock = new ReentrantLock();
    protected static HashSet<GrInDriver> activeDrivers = new HashSet<GrInDriver>();
    protected static GraphicsDevice lastClosureDevice = null;

    private final boolean useMultithread;

    private long startup = System.currentTimeMillis();

    private double lastDt = getTime();

    private RawSoundDriver sound = null;

    private String fbDirectory = ".";

    public GaBIEnImpl(boolean useMT) {
        useMultithread = useMT;
    }

    // Tries to work out a sensible device for fullscreen.
    protected static GraphicsDevice getFSDevice() {
        activeDriverLock.lock();
        for (GrInDriver igd : activeDrivers) {
            GraphicsDevice gd = igd.frame.getGraphicsConfiguration().getDevice();
            if (gd != null) {
                if (gd.isFullScreenSupported()) {
                    activeDriverLock.unlock();
                    return gd;
                }
            }
        }
        if (lastClosureDevice != null) {
            if (lastClosureDevice.isFullScreenSupported()) {
                activeDriverLock.unlock();
                return lastClosureDevice;
            }
        }
        activeDriverLock.unlock();
        GraphicsDevice[] devs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (devs.length == 0)
            return null;
        for (int i = 0; i < devs.length; i++)
            if (devs[i].isFullScreenSupported())
                return devs[i];
        return null;
    }

    public double getTime() {
        return (System.currentTimeMillis() - startup) / 1000.0;
    }

    public double timeDelta(boolean reset) {
        double dt = getTime() - lastDt;
        if (reset)
            lastDt = getTime();
        return dt;
    }

    public InputStream getResource(String resource) {
        return ClassLoader.getSystemClassLoader().getResourceAsStream("assets/" + resource);
    }

    public InputStream getFile(String FDialog) {
        try {
            return new FileInputStream(FDialog);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public OutputStream getOutFile(String FDialog) {
        try {
            return new FileOutputStream(FDialog);
        } catch (Exception e) {
            return null;
        }
    }


    protected IWindowGrBackend makeOffscreenBufferInt(int w, int h, boolean alpha) {
        // Note all the multithreading occurs in OsbDriverMT.
        if (w <= 0)
            return new NullOsbDriver();
        if (h <= 0)
            return new NullOsbDriver();
        if (useMultithread)
            return new OsbDriverMT(w, h, alpha);
        return new OsbDriverCore(w, h, alpha);
    }

    public IGrInDriver makeGrIn(String name, int w, int h, WindowSpecs ws) {
        return new gabien.GrInDriver(name, ws, makeOffscreenBufferInt(w, h, false));
    }

    @Override
    public IGrDriver makeOffscreenBuffer(int w, int h, boolean alpha) {
        // Finalization wrapper as a just-in-case.
        return new ProxyOsbDriver(makeOffscreenBufferInt(w, h, alpha));
    }

    @Override
    public boolean isActuallySingleWindow() {
        return false;
    }

    public void ensureQuit() {
        System.exit(0);
    }

    public IRawAudioDriver getRawAudio() {
        if (sound == null) {
            try {
                sound = new RawSoundDriver();
            } catch (LineUnavailableException ex) {
                Logger.getLogger(GaBIEnImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return sound;
    }

    @Override
    public void hintShutdownRawAudio() {
        if (sound != null)
            sound.shutdown();
        sound = null;
    }

    @Override
    public WindowSpecs defaultWindowSpecs(String name, int w, int h) {
        WindowSpecs ws = new WindowSpecs();
        ws.scale = ((w > 400) || (h > 300)) ? 1 : 2;
        ws.resizable = false;
        return ws;
    }

    @Override
    public IImage getImage(String a, boolean res) {
        String ki = a + "_N_N_N" + (res ? 'R' : 'F');
        if (loadedImages.containsKey(ki))
            return loadedImages.get(ki);
        try {
            AWTImage img = new AWTImage();
            img.buf = ImageIO.read(res ? getResource(a) : getFile(a));
            if (img.buf == null)
                throw new NullPointerException();
            loadedImages.put(ki, img);
            return img;
        } catch (Exception ex) {
            System.err.println("Couldn't get:" + ki);
            IImage img = GaBIEn.getErrorImage();
            loadedImages.put(ki, img);
            return img;
        }
    }
    @Override
    public IImage getImageCK(String a, boolean res, int tr, int tg, int tb) {
        String ki = a + "_" + tr + "_" + tg + "_" + tb + (res ? 'R' : 'F');
        if (loadedImages.containsKey(ki))
            return loadedImages.get(ki);
        try {
            AWTImage img = new AWTImage();
            BufferedImage tmp;
            tmp = ImageIO.read(res ? getResource(a) : getFile(a));
            img.buf = new BufferedImage(tmp.getWidth(), tmp.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int px = 0; px < tmp.getWidth(); px++) {
                for (int py = 0; py < tmp.getHeight(); py++) {
                    int c = tmp.getRGB(px, py);
                    if ((c & 0xFFFFFF) != (tb | (tg << 8) | (tr << 16))) {
                        img.buf.setRGB(px, py, c | 0xFF000000);
                    } else {
                        img.buf.setRGB(px, py, 0);
                    }
                }
            }
            loadedImages.put(ki, img);
            return img;
        } catch (Exception ex) {
            System.err.println("Couldn't get:" + ki);
            IImage img = GaBIEn.getErrorImage();
            loadedImages.put(ki, img);
            return img;
        }
    }

    @Override
    public IImage createImage(int[] colours, int width, int height) {
        if (width <= 0)
            return new NullOsbDriver();
        if (height <= 0)
            return new NullOsbDriver();
        AWTImage ia = new AWTImage();
        ia.buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ia.buf.setRGB(0, 0, width, height, colours, 0, width);
        return ia;
    }

    @Override
    public void hintFlushAllTheCaches() {
        loadedImages.clear();
    }

    @Override
    public int measureText(int i, String text) {
        Font f = OsbDriverCore.getFont(i);
        if (f == null)
            return text.length() * (i / 2);
        Rectangle r = f.getStringBounds(text, new FontRenderContext(AffineTransform.getTranslateInstance(0, 0), true, false)).getBounds();
        return r.width;
    }

    public static String getDefaultFont() {
        return Font.SANS_SERIF;
    }


    @Override
    public String[] getFontOverrides() {
        String[] p = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        boolean foundFirst = false;
        for (int i = 0; i < p.length; i++) {
            if (p[i].equals(getDefaultFont())) {
                foundFirst = true;
                if (i != 0) {
                    String x = p[0];
                    p[0] = getDefaultFont();
                    p[i] = x;
                }
                break;
            }
        }
        if (!foundFirst) {
            String[] p2 = new String[p.length + 1];
            p2[0] = getDefaultFont();
            System.arraycopy(p, 0, p2, 1, p.length);
            return p2;
        }
        return p;
    }

    @Override
    public String[] listEntries(String s) {
        return new File(s).list();
    }

    @Override
    public void makeDirectories(String s) {
        new File(s).mkdirs();
    }

    @Override
    public boolean fileOrDirExists(String s) {
        return new File(s).exists();
    }

    @Override
    public boolean dirExists(String s) {
        return new File(s).isDirectory();
    }

    @Override
    public boolean tryStartTextEditor(String fpath) {
        try {
            // edit fails here, for some reason?
            Desktop.getDesktop().open(new File(fpath));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void rmFile(String s) {
        new File(s).delete();
    }

    @Override
    public void setBrowserDirectory(String b) {
        if (b.isEmpty())
            b = ".";
        fbDirectory = b;
    }

    @Override
    public void startFileBrowser(String text, boolean saving, String exts, final IConsumer<String> result) {
        Frame f = null;
        GraphicsDevice gd = getFSDevice();
        if (gd != null) {
            activeDriverLock.lock();
            for (GrInDriver gid : activeDrivers)
                if (gd.getFullScreenWindow() == gid.frame)
                    f = gid.frame;
            activeDriverLock.unlock();
        }
        final FileDialog fd = new FileDialog(f, text);
        fd.setDirectory(fbDirectory);
        fd.setFile(exts);
        fd.setMode(saving ? FileDialog.SAVE : FileDialog.LOAD);
        // The next operation locks AWT up in an event loop.
        // So hand it over to AWT.
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                fd.setVisible(true);
                // Can be null. This is fine.
                final String f = fd.getFile();
                final String fdr = fd.getDirectory();
                final String fs = f == null ? null : (fd.getDirectory() + f);
                GaBIEn.pushCallback(new Runnable() {
                    @Override
                    public void run() {
                        if (fdr != null)
                            fbDirectory = fdr;
                        result.accept(fs);
                    }
                });
            }
        });
    }
}
