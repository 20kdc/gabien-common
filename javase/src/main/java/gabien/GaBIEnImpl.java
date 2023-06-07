/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.text.IFixedSizeFont;
import gabien.uslx.append.*;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
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

    // For testing only!
    public static boolean fontsAlwaysMeasure16;

    protected static ReentrantLock activeDriverLock = new ReentrantLock();
    protected static HashSet<GrInDriver> activeDrivers = new HashSet<GrInDriver>();
    protected static GraphicsDevice lastClosureDevice = null;

    private RawSoundDriver sound = null;

    private String fbDirectory = ".";

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

    public InputStream getResource(String resource) {
        return ClassLoader.getSystemClassLoader().getResourceAsStream("assets/" + resource);
    }

    public IGrInDriver makeGrIn(String name, int w, int h, WindowSpecs ws) {
        return new gabien.GrInDriver(name, ws, w, h);
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
    public IWSIImage getImage(String a, boolean res) {
        try {
            BufferedImage bi = ImageIO.read(res ? getResource(a) : GaBIEn.getInFile(a));
            if (bi.getType() == BufferedImage.TYPE_INT_ARGB)
                return new AWTWSIImage(bi);
            int[] tmp = new int[bi.getWidth() * bi.getHeight()];
            bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), tmp, 0, bi.getWidth());
            return new AWTWSIImage(tmp, bi.getWidth(), bi.getHeight());
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public IWSIImage.RW createWSIImage(@NonNull int[] colours, int width, int height) {
        return new AWTWSIImage(colours, width, height);
    }

    public static String getDefaultFont() {
        return Font.SANS_SERIF;
    }

    @Override
    @NonNull
    public IFixedSizeFont getDefaultNativeFont(int size) {
        return AWTNativeFont.getFont(size, null);
    }

    @Override
    @Nullable
    public IFixedSizeFont getNativeFont(int size, @NonNull String name) {
        return AWTNativeFont.getFont(size, name);
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
    public void setBrowserDirectory(String b) {
        if (b.isEmpty())
            b = ".";
        fbDirectory = b;
    }

    @Override
    public void startFileBrowser(String text, boolean saving, String exts, final IConsumer<String> result, String initialName) {
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
