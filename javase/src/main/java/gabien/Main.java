/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import gabien.backend.EmulatedFileBrowser;
import gabien.backend.WindowMux;
import gabien.natives.BadGPU;
import gabien.natives.BadGPUEnum.MetaInfoType;
import gabien.uslx.vfs.impl.JavaIOFSBackend;
import gabien.wsi.WindowSpecs;

abstract class Main {
    private static boolean ignoreBlindingSun = false;

    public static void main(String[] args) {
        try {
            mainInner(args);
        } catch (Throwable t) {
            t.printStackTrace();
            StringWriter sw = new StringWriter();
            sw.append("The program was unable to start, or crashed while running.\nJVM: ");
            sw.append(System.getProperty("java.vendor"));
            sw.append(" ");
            sw.append(System.getProperty("java.vm.name"));
            sw.append(" ");
            sw.append(System.getProperty("java.vm.version"));
            sw.append(" ");
            sw.append(System.getProperty("os.arch"));
            sw.append(" ");
            sw.append(System.getProperty("os.name"));
            sw.append(" ");
            sw.append(System.getProperty("os.version"));
            sw.append("\nGaBIEn Natives version: ");
            try {
                sw.append(gabien.natives.Loader.getNativesVersion());
            } catch (Throwable failed) {
                sw.append("[NOT INITIALIZED] ");
                try {
                    sw.append(gabien.natives.Loader.defaultLoaderJavaSE() ? "[OK] " : "[FAIL] ");
                    sw.append(gabien.natives.Loader.getNativesVersion());
                } catch (Throwable ex2) {
                    sw.append("[CATASTROPHIC FAILURE]");
                }
            }
            sw.append("\nBadGPU says renderer is: ");
            try {
                BadGPU.Instance bi = BadGPU.newInstance(0);
                String what = bi.getMetaInfo(MetaInfoType.Renderer);
                bi.dispose();
                sw.append(what);
                sw.append(" [OK]\n");
            } catch (Throwable ex2) {
                sw.append("[FAIL]\n");
            }
            sw.append("\n");
            t.printStackTrace(new PrintWriter(sw));
            // something has gone horribly wrong
            Frame f = new Frame("Graphics And Basic Input Engine");
            f.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    GaBIEn.ensureQuit();
                }
            });
            TextArea ta = new TextArea();
            ta.setText(sw.toString());
            f.add(ta);
            f.setSize(800, 600);
            f.setVisible(true);
        }
    }

    /**
     * Use reflection to find and run the application.
     */
    private static void mainInner(String[] args) throws Exception{
        boolean tryForceOpenGL = false;
        boolean ignoreDPI = false;
        boolean useInternalBrowser = false;
        boolean isDebug = false;
        boolean isTimeLogging = false;
        boolean isCrashingVopeks = false;
        if (args.length > 0) {
            for (String s : args) {
                if (s.equalsIgnoreCase("forceOpenGL"))
                    tryForceOpenGL = true;
                if (s.equalsIgnoreCase("debug"))
                    isDebug = true;
                if (s.equalsIgnoreCase("stopImmediately"))
                    throw new RuntimeException("stopImmediately passed on command line");
                if (s.equalsIgnoreCase("crashVopeks"))
                    isCrashingVopeks = true;
                if (s.equalsIgnoreCase("timeLogger"))
                    isTimeLogging = true;
                if (s.equalsIgnoreCase("iAmARobot"))
                    GaBIEnImpl.mobileEmulation = true;
                if (s.equalsIgnoreCase("simAndroid23Init"))
                    GaBIEnImpl.storagePermissionFlag = false;
                if (s.equalsIgnoreCase("forceIgnoreDPI"))
                    ignoreDPI = true;
                if (s.equalsIgnoreCase("blindingSun"))
                    ignoreBlindingSun = true;
                if (s.equalsIgnoreCase("useInternalBrowser"))
                    useInternalBrowser = true;
            }
        }
        if (tryForceOpenGL) {
            System.setProperty("sun.java2d.opengl", "true");
            System.setProperty("sun.java2d.xrender", "true");
        }
        if (!ignoreDPI) {
        	try {
            	System.setProperty("sun.java2d.uiScale", "1");
            	int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            	GrInDriver.uiGuessScaleTenths = (int) Math.max(10, Math.ceil(dpi / 9.6d));
        	} catch (Exception e) {
        		// number format exception, probably, but whatever happens DON'T FAIL
        		e.printStackTrace();
        	}
        }
        initializeEmbedded(isDebug, isTimeLogging, isCrashingVopeks);
        // System.err.println("GJSEStartProfile after initializeEmbedded: " + GaBIEn.getTime());
        if (GaBIEnImpl.mobileEmulation) {
        	WindowSpecs ws = new WindowSpecs(GaBIEn.internal);
        	ws.resizable = false;
        	GaBIEn.internalWindowing = new WindowMux(GaBIEn.internal, GaBIEn.internalWindowing.makeGrIn("Mobile", 960, 540, ws));
            useInternalBrowser = true;
        }
        if (useInternalBrowser)
            GaBIEn.internalFileBrowser = new EmulatedFileBrowser(GaBIEn.internal);
        Class.forName("gabienapp.Application").getDeclaredMethod("gabienmain").invoke(null);
    }

    /**
     * See GaBIEn.initializeEmbedded.
     */
    public static void initializeEmbedded() {
        initializeEmbedded(false, false, false);
    }

    private static void initializeEmbedded(boolean isDebug, boolean isTimeLogging, boolean isCrashingVopeks) {
        if (!ignoreBlindingSun) {
            // Seriously, Sun, were you trying to cause epilepsy episodes?!?!
            System.setProperty("sun.awt.noerasebackground", "true");
            System.setProperty("sun.awt.erasebackgroundonresize", "true");
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    Font f = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
                    BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
                    Graphics g = scratch.createGraphics();
                    g.setFont(f);
                    g.drawString("Flutter", 0, 0);
                    System.err.println("FONT: Font has preloaded");
                } catch (Exception e) {
                    // May crash on certain JREs.
                    System.err.println("FONT: Font didn't preload, " + e);
                }
                GaBIEn.fontsReady = true;
            }
        }.start();
        GaBIEnImpl impl = new GaBIEnImpl();
        GaBIEn.internal = impl;
        GaBIEn.clipboard = new ClipboardImpl();
        GaBIEn.mutableDataFS = JavaIOFSBackend.from(new File("."));
        GaBIEn.internalWindowing = impl;
        GaBIEn.internalFileBrowser = (GaBIEnImpl) GaBIEn.internal;
        // pretty much all the startup time goes here
        GaBIEn.setupNativesAndAssets(isDebug, isTimeLogging, isCrashingVopeks);
        GaBIEnUI.setupAssets();
    }
}
