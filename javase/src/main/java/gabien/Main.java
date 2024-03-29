/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import gabien.backend.EmulatedFileBrowser;
import gabien.backend.WindowMux;
import gabien.uslx.vfs.impl.JavaIOFSBackend;
import gabien.wsi.WindowSpecs;

abstract class Main {
    private static boolean ignoreBlindingSun = false;
    /**
     * Use reflection to find and run the application.
     */
    public static void main(String[] args) {
        boolean tryForceOpenGL = false;
        boolean ignoreDPI = false;
        boolean useInternalBrowser = false;
        boolean isDebug = false;
        boolean isTimeLogging = false;
        if (args.length > 0) {
            for (String s : args) {
                if (s.equalsIgnoreCase("forceOpenGL"))
                    tryForceOpenGL = true;
                if (s.equalsIgnoreCase("debug"))
                    isDebug = true;
                if (s.equalsIgnoreCase("timeLogger"))
                    isTimeLogging = true;
                if (s.equalsIgnoreCase("iAmARobot"))
                    GaBIEnImpl.mobileEmulation = true;
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
        initializeEmbedded(isDebug, isTimeLogging);
        // System.err.println("GJSEStartProfile after initializeEmbedded: " + GaBIEn.getTime());
        if (GaBIEnImpl.mobileEmulation) {
        	WindowSpecs ws = new WindowSpecs(GaBIEn.internal);
        	ws.resizable = false;
        	GaBIEn.internalWindowing = new WindowMux(GaBIEn.internal, GaBIEn.internalWindowing.makeGrIn("Mobile", 960, 540, ws));
            useInternalBrowser = true;
        }
        if (useInternalBrowser)
            GaBIEn.internalFileBrowser = new EmulatedFileBrowser(GaBIEn.internal);
        try {
            Class.forName("gabienapp.Application").getDeclaredMethod("gabienmain").invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
            GaBIEn.ensureQuit();
        }
    }

    /**
     * See GaBIEn.initializeEmbedded.
     */
    public static void initializeEmbedded(boolean isDebug, boolean isTimeLogging) {
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
        GaBIEn.setupNativesAndAssets(isDebug, isTimeLogging);
        GaBIEnUI.setupAssets();
    }
}
