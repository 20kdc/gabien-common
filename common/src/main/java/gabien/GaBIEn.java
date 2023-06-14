/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.backend.IGaBIEnClipboard;
import gabien.backend.IGaBIEnFileBrowser;
import gabien.backend.IGaBIEnMultiWindow;
import gabien.backend.NullGrDriver;
import gabien.natives.BadGPU;
import gabien.render.IWSIImage;
import gabien.text.IFixedSizeFont;
import gabien.ui.theming.ThemingCentral;
import gabien.uslx.append.*;
import gabien.uslx.vfs.FSBackend;
import gabien.uslx.vfs.FSBackend.DirectoryState;
import gabien.uslx.vfs.FSBackend.XState;
import gabien.vopeks.Vopeks;
import gabien.vopeks.VopeksGrDriver;
import gabien.vopeks.VopeksImage;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class GaBIEn {
    public static Vopeks vopeks;
    public static TimeLogger timeLogger;
    protected static IGaBIEn internal;
    protected static IGaBIEnMultiWindow internalWindowing;
    protected static IGaBIEnFileBrowser internalFileBrowser;
    public static IGaBIEnClipboard clipboard;
    public static FSBackend mutableDataFS;
    private static IImage errorImage;
    private static ReentrantLock callbackQueueLock = new ReentrantLock();
    private static LinkedList<Runnable> callbackQueue = new LinkedList<Runnable>();
    private static LinkedList<Runnable> callbacksToAddAfterCallbacksQueue = new LinkedList<Runnable>();
    private static NativeFontCache nativeFontCache = new NativeFontCache();
    private static ImageCache imageCache = new ImageCache();

    // Additional resource load locations.
    public static String[] appPrefixes = new String[0];
    // Can be used by internal UI.
    public static String wordSave = "Save", wordLoad = "Load";
    public static String wordInvalidFileName = "Invalid or missing file name.";
    public static int sysCoreFontSize = 8;

    private static double lastDt;
    private static long startup = System.currentTimeMillis();

    public static double getTime() {
        return (System.currentTimeMillis() - startup) / 1000.0;
    }

    public static double timeDelta(boolean reset) {
        double dt = getTime() - lastDt;
        if (reset)
            lastDt = getTime();
        return dt;
    }

    // Regarding this change:
    // getRFile and getWFile are the "arbitrary file access" providers,
    //  and should only ever access the "external storage".
    // This prevents the namespace pollution I was worried about before.
    // However, sometimes we do want to access resources, but also want the user to be able to override these.
    // Hence getResource and appPrefix.
    // Essentially, a gabien application that wishes to allow openRFile and openWFile.
    public static InputStream getResource(String resource) {
        for (String s : appPrefixes) {
            InputStream inp = getInFile(s + resource);
            if (inp != null)
                return inp;
        }
        return internal.getResource(resource);
    }

    /**
     * Wrapper over getResource for text.
     */
    public static InputStreamReader getTextResource(String resource) {
        InputStream inp = getResource(resource);
        if (inp == null)
            return null;
        return new InputStreamReader(inp, StandardCharsets.UTF_8);
    }

    public static InputStream getInFile(String resource) {
        try {
            return mutableDataFS.openRead(resource);
        } catch (Exception ioe) {
            return null;
        }
    }

    public static OutputStream getOutFile(String string) {
        try {
            return mutableDataFS.openWrite(string);
        } catch (Exception ioe) {
            return null;
        }
    }

    public static boolean singleWindowApp() {
        return internalWindowing.isActuallySingleWindow();
    }

    public static IRawAudioDriver getRawAudio() {
        return internal.getRawAudio();
    }
    public static void hintShutdownRawAudio() {
        internal.hintShutdownRawAudio();
    }

    public static void ensureQuit() {
        try {
            vopeks.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (timeLogger != null)
                timeLogger.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        internal.ensureQuit();
    }

    public static WindowSpecs defaultWindowSpecs(String name, int w, int h) {
        return internalWindowing.defaultWindowSpecs(name, w, h);
    }

    public static IGrInDriver makeGrIn(String name, int w, int h) {
        WindowSpecs ws = defaultWindowSpecs(name, w, h);
        return makeGrIn(name, w, h, ws);
    }

    public static IGrInDriver makeGrIn(String name, int w, int h, WindowSpecs specs) {
        return internalWindowing.makeGrIn(name, w, h, specs);
    }

    /**
     * Creates an offscreen RGBA buffer.
     */
    public static IGrDriver makeOffscreenBuffer(int width, int height) {
        return makeOffscreenBuffer(width, height, null);
    }

    /**
     * Creates an offscreen RGBA buffer.
     * This variant has a debug ID.
     */
    public static IGrDriver makeOffscreenBuffer(int width, int height, @Nullable String id) {
        if (width <= 0)
            return new NullGrDriver();
        if (height <= 0)
            return new NullGrDriver();
        return new VopeksGrDriver(GaBIEn.vopeks, id, width, height, null);
    }

    // This has to at least support JPGs, PNGs and BMPs.
    // On error, it should return an "error" image. This "error" image is unique, and can be gotten via getErrorImage.

    public static IImage getImage(String a) {
        return getImageEx(a, true, true);
    }

    public static IImage getImageCK(String a, int r, int g, int b) {
        return getImageCKEx(a, true, true, r, g, b);
    }

    public static IImage getImageEx(String a, boolean fs, boolean res) {
        IImage err = getErrorImage();
        if (fs) {
            IImage r = imageCache.getImage(a, false);
            if (r != err)
                return r;
        }
        if (res) {
            for (String s : appPrefixes) {
                IImage r = imageCache.getImage(s + a, false);
                if (r != err)
                    return r;
            }
            IImage r = imageCache.getImage(a, true);
            if (r == err)
                System.err.println("GaBIEn: Couldn't get: " + a + " (" + fs + ", " + res + ")");
            return r;
        }
        System.err.println("GaBIEn: Couldn't get: " + a + " (" + fs + ", " + res + ")");
        return err;
    }

    public static IImage getImageCKEx(String a, boolean fs, boolean res, int r, int g, int b) {
        IImage err = getErrorImage();
        if (fs) {
            IImage ri = imageCache.getImageCK(a, false, r, g, b);
            if (ri != err)
                return ri;
        }
        if (res) {
            for (String s : appPrefixes) {
                IImage ri = imageCache.getImageCK(s + a, false, r, g, b);
                if (ri != err)
                    return ri;
            }
            IImage ri = imageCache.getImageCK(a, true, r, g, b);
            if (ri == err)
                System.err.println("GaBIEn: Couldn't get: " + a + " (" + fs + ", " + res + ")");
            return ri;
        }
        System.err.println("GaBIEn: Couldn't get: " + a + " (" + fs + ", " + res + ")");
        return err;
    }

    public static IImage getErrorImage() {
        if (errorImage == null) {
            errorImage = createImage("getErrorImage", new int[] {
                    0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000,
                    0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000,
                    0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000,
                    0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000,
                    0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF,
                    0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF,
                    0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF,
                    0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFF00FF
            }, 8, 8);
        }
        return errorImage;
    }

    public static IImage createImage(@NonNull int[] colours, int width, int height) {
        return createImage(null, colours, width, height);
    }

    public static IImage createImage(@Nullable String debugId, @NonNull int[] colours, int width, int height) {
        if (width <= 0)
            return new NullGrDriver();
        if (height <= 0)
            return new NullGrDriver();
        return new VopeksImage(GaBIEn.vopeks, debugId, width, height, colours);
    }

    public static IWSIImage.RW createWSIImage(@NonNull int[] colours, int width, int height) {
        return internal.createWSIImage(colours, width, height);
    }

    public static void hintFlushAllTheCaches() {
        imageCache.hintFlushAllTheCaches();
    }

    /**
     * Returns the list of native font names.
     */
    public static String[] getFontOverrides() {
        return internal.getFontOverrides();
    }

    /**
     * Gets a native font by name. If the name is null, returns the default font.
     * If not available, returns null, unless fallback is set.
     */
    public static @Nullable IFixedSizeFont getNativeFont(int size, @Nullable String name, boolean fallback) {
        if (name == null)
            return nativeFontCache.getDefaultNativeFont(size);
        IFixedSizeFont tmp = nativeFontCache.getNativeFont(size, name);
        if (tmp == null && fallback)
            return nativeFontCache.getDefaultNativeFont(size);
        return tmp;
    }

    /**
     * Gets a native font by name. If the name is null, returns the default font.
     * If not available, returns the default font.
     */
    public static @NonNull IFixedSizeFont getNativeFontFallback(int size, @Nullable String name) {
        if (name == null)
            return nativeFontCache.getDefaultNativeFont(size);
        IFixedSizeFont nf = nativeFontCache.getNativeFont(size, name);
        if (nf == null)
            return nativeFontCache.getDefaultNativeFont(size);
        return nf;
    }

    public static boolean fileOrDirExists(String s) {
        return mutableDataFS.getState(s) != null;
    }
    public static boolean dirExists(String s) {
        return mutableDataFS.getState(s) instanceof DirectoryState;
    }

    // Resources DO NOT QUALIFY.
    // It is possible that this will be called with or without a trailing "/".
    // Elements are listed just with names and no further detail.
    public static String[] listEntries(String s) {
        XState xs = mutableDataFS.getState(s);
        if (xs instanceof DirectoryState)
            return ((DirectoryState) xs).entries;
        return null;
    }

    public static String absolutePathOf(String s) {
        return mutableDataFS.absolutePathOf(s);
    }

    public static String nameOf(String s) {
        return mutableDataFS.nameOf(s);
    }

    /**
     * See FSBackend.parentOf
     * TLDR: Returns null if no parent exists, is supposed to switch to absolute paths when necessary.
     */
    public static @Nullable String parentOf(String s) {
        return mutableDataFS.parentOf(s);
    }

    public static void makeDirectories(String s) {
        mutableDataFS.mkdirs(s);
    }

    /**
     * Attempts to start a text editor. Returns false on failure.
     */
    public static boolean tryStartTextEditor(String fpath) {
        return internal.tryStartTextEditor(fpath);
    }

    public static void rmFile(String s) {
        try {
            mutableDataFS.delete(s);
        } catch (Exception e) {
            // errors are silenced
        }
    }

    public static void setBrowserDirectory(String s) {
        internalFileBrowser.setBrowserDirectory(s);
    }

    // exts should just be left blank for now.
    // iConsumer is called as part of runCallbacks.
    // Regarding the path, the only guarantee is that it'll be null or a valid file path.
    // It does not necessarily have to match the standard gabien path separator.
    public static void startFileBrowser(String s, boolean saving, String exts, IConsumer<String> iConsumer) {
        internalFileBrowser.startFileBrowser(s, saving, exts, iConsumer, "");
    }
    public static void startFileBrowser(String s, boolean saving, String exts, IConsumer<String> iConsumer, String initialName) {
        internalFileBrowser.startFileBrowser(s, saving, exts, iConsumer, initialName);
    }

    // invokeLater-alike for the gabien main thread.
    // This can be used by the application,
    //  but mostly exists as a way to get application callbacks called on the thread they are expected to be called on.

    public static void pushCallback(Runnable r) {
        callbackQueueLock.lock();
        callbackQueue.add(r);
        callbackQueueLock.unlock();
    }

    public static void pushLaterCallback(Runnable runnable) {
        callbackQueueLock.lock();
        callbacksToAddAfterCallbacksQueue.add(runnable);
        callbackQueueLock.unlock();
    }

    public static void runCallbacks() {
        callbackQueueLock.lock();
        while (callbackQueue.size() > 0) {
            callbackQueueLock.unlock();
            callbackQueue.removeFirst().run();
            callbackQueueLock.lock();
        }
        callbackQueue.addAll(callbacksToAddAfterCallbacksQueue);
        callbacksToAddAfterCallbacksQueue.clear();
        callbackQueueLock.unlock();
    }

    // DT compensation is optional.
    public static double endFrame(double dTTarg) {
        runCallbacks();
        double dT = GaBIEn.timeDelta(false);
        while (dT < dTTarg) {
            try {
                long ofs = (long) ((dTTarg - dT) * 1000);
                if (ofs > 0) {
                    Thread.sleep(ofs);
                } else {
                    Thread.yield();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dT = GaBIEn.timeDelta(false);
        }
        return GaBIEn.timeDelta(true);
    }

    /**
     * Useful for embedded command-line applications that don't want to muck up the classpath.
     */
    public static void initializeEmbedded() {
        try {
            Class.forName("gabien.Main").getDeclaredMethod("initializeEmbedded").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes gabien internal stuff. Expected to be called from gabien.Main.initializeEmbedded and other places.
     */
    static void setupNativesAndAssets(boolean debug, boolean setupTimeLogger) {
        if (!gabien.natives.Loader.defaultLoader(GaBIEn::getResource, internal::nativeDestinationSetup))
            System.err.println("GaBIEn: Natives did not initialize. And before it gets better, it's getting worse...");
        System.err.println("GaBIEn: Natives: " + gabien.natives.Loader.getNativesVersion());
        if (setupTimeLogger)
            timeLogger = new TimeLogger(getOutFile("gTimeLogger.bin"));
        int newInstanceFlags = BadGPU.NewInstanceFlags.CanPrintf;
        if (debug)
            newInstanceFlags |= BadGPU.NewInstanceFlags.BackendCheck | BadGPU.NewInstanceFlags.BackendCheckAggressive;
        vopeks = new Vopeks(newInstanceFlags, timeLogger);
        FontManager.setupFonts();
        ThemingCentral.setupAssets();
    }
}
