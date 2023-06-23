/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.backend.IGaBIEn;
import gabien.backend.IGaBIEnFileBrowser;
import gabien.backend.IGaBIEnMultiWindow;
import gabien.backend.ImageCache;
import gabien.backend.NativeFontCache;
import gabien.backend.NullGrDriver;
import gabien.natives.BadGPU;
import gabien.render.IGrDriver;
import gabien.render.IImage;
import gabien.render.WSIImage;
import gabien.text.IFixedSizeFont;
import gabien.text.EngineFonts;
import gabien.ui.LAFChain;
import gabien.ui.theming.ThemingCentral;
import gabien.uslx.append.*;
import gabien.uslx.vfs.FSBackend;
import gabien.uslx.vfs.FSBackend.DirectoryState;
import gabien.uslx.vfs.FSBackend.XState;
import gabien.vopeks.Vopeks;
import gabien.vopeks.VopeksGrDriver;
import gabien.vopeks.VopeksImage;
import gabien.wsi.IGaBIEnClipboard;
import gabien.wsi.IGrInDriver;
import gabien.wsi.WindowSpecs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The entrypoint to the API. Has been around as long as the library was a distinct library.
 * This is at least as early as 10th October, 2014, though it only consisted of 8 classes back then.
 */
public final class GaBIEn {
    /**
     * Vopeks instance. See the vopeks package.
     */
    public static Vopeks vopeks;

    /**
     * TimeLogger for profiling. Can be null, be careful.
     */
    public static TimeLogger timeLogger;

    /**
     * The clipboard.
     */
    public static IGaBIEnClipboard clipboard;

    /**
     * The filesystem.
     */
    public static FSBackend mutableDataFS;

    /**
     * Flag that indicates native fonts can be used without causing a lagspike.
     * This is a workaround for some oddities I've run into.
     */
    public static volatile boolean fontsReady;

    /**
     * Only the backend should access this! Implementation.
     */
    static IGaBIEn internal;

    /**
     * Only the backend should access this! Windowing implementation.
     * This is separated out to allow swapping based on user configuration (mobile emulation).
     */
    static IGaBIEnMultiWindow internalWindowing;

    /**
     * Only the backend should access this! File Browser implementation.
     */
    static IGaBIEnFileBrowser internalFileBrowser;

    /**
     * Engine fonts. They're fonts, in the engine.
     */
    public static EngineFonts engineFonts;

    private static IImage errorImage;
    private static ReentrantLock callbackQueueLock = new ReentrantLock();
    private static LinkedList<Runnable> callbackQueue = new LinkedList<Runnable>();
    private static LinkedList<Runnable> callbacksToAddAfterCallbacksQueue = new LinkedList<Runnable>();
    private static NativeFontCache nativeFontCache;
    private static ImageCache imageCache;

    /**
     * Additional resource load locations.
     * This is initialized to gabienapp.Application.appPrefixes if possible.
     */
    public static String[] appPrefixes = new String[0];

    /**
     * Translatable phrases used by internal UI.
     */
    public static String wordSave = "Save", wordLoad = "Load", wordInvalidFileName = "Invalid or missing file name.";

    /**
     * Font size used by internal UI.
     */
    public static int sysCoreFontSize = 8;

    /**
     * Theme root used by internal UI.
     */
    public static final LAFChain.Node sysThemeRoot = new LAFChain.Node();

    private static double lastDt;
    private static long startup = System.currentTimeMillis();

    private GaBIEn() {
        // Nope!
    }

    /**
     * Gets the time since application start in seconds.
     */
    public static double getTime() {
        return (System.currentTimeMillis() - startup) / 1000.0;
    }

    /**
     * Gets the amount of time since this function was last called with true passed.
     * If the function was never called with true passed, gets the time since application start in seconds.
     */
    public static double timeDelta(boolean reset) {
        double recording = getTime();
        double dt = recording - lastDt;
        if (reset)
            lastDt = recording;
        return dt;
    }

    /**
     * Gets a resource.
     * Resources can come from disk via application resource prefixes, or from the JAR's assets directory.
     * @return Resource InputStream, or null on failure.
     */
    public static @Nullable InputStream getResource(@NonNull String resource) {
        for (String s : appPrefixes) {
            InputStream inp = getInFile(s + resource);
            if (inp != null)
                return inp;
        }
        return internal.getResource(resource);
    }

    /**
     * Wrapper over getResource for text.
     * @return Resource InputStreamReader (UTF-8), or null on failure.
     */
    public static @Nullable InputStreamReader getTextResource(@NonNull String resource) {
        InputStream inp = getResource(resource);
        if (inp == null)
            return null;
        return new InputStreamReader(inp, StandardCharsets.UTF_8);
    }

    /**
     * Wraps mutableDataFS. Opens a file for input. If an error occurs, returns null.
     * @return The InputStream, or null on error.
     */
    public static @Nullable InputStream getInFile(@NonNull String name) {
        try {
            return mutableDataFS.openRead(name);
        } catch (Exception ioe) {
            return null;
        }
    }

    /**
     * Wraps mutableDataFS. Opens a file for output. If an error occurs, returns null.
     * @return The InputStream, or null on error.
     */
    public static @Nullable OutputStream getOutFile(@NonNull String name) {
        try {
            return mutableDataFS.openWrite(name);
        } catch (Exception ioe) {
            return null;
        }
    }

    /**
     * @return Is this a single-window platform? (Android or mobile emulator)
     */
    public static boolean singleWindowApp() {
        return internalWindowing.isActuallySingleWindow();
    }

    /**
     * Gets/creates the raw audio driver.
     * @return Raw audio driver.
     */
    public static @NonNull IRawAudioDriver getRawAudio() {
        return internal.getRawAudio();
    }

    /**
     * A hint to the backend to shutdown raw audio (possibly saving CPU).
     */
    public static void hintShutdownRawAudio() {
        internal.hintShutdownRawAudio();
    }

    /**
     * Ensures that the application quits.
     */
    public static void ensureQuit() {
        try {
            if (timeLogger != null)
                timeLogger.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        internal.ensureQuit();
    }

    /**
     * Returns the default window specifications for the given parameters.
     */
    public static @NonNull WindowSpecs defaultWindowSpecs(@NonNull String name, int w, int h) {
        return internalWindowing.defaultWindowSpecs(name, w, h);
    }

    /**
     * Opens a window with the default window specifications.
     */
    public static @NonNull IGrInDriver makeGrIn(@NonNull String name, int w, int h) {
        WindowSpecs ws = defaultWindowSpecs(name, w, h);
        return makeGrIn(name, w, h, ws);
    }

    /**
     * Opens a window with the given window specifications.
     */
    public static @NonNull IGrInDriver makeGrIn(String name, int w, int h, @NonNull WindowSpecs specs) {
        return internalWindowing.makeGrIn(name, w, h, specs);
    }

    /**
     * Creates an offscreen RGBA buffer.
     */
    public static @NonNull IGrDriver makeOffscreenBuffer(int width, int height) {
        return makeOffscreenBuffer(width, height, null);
    }

    /**
     * Creates an offscreen RGBA buffer.
     * This variant has a debug ID.
     */
    public static @NonNull IGrDriver makeOffscreenBuffer(int width, int height, @Nullable String id) {
        if (width <= 0)
            return new NullGrDriver();
        if (height <= 0)
            return new NullGrDriver();
        return new VopeksGrDriver(GaBIEn.vopeks, id, width, height, null);
    }

    /**
     * Gets an image. This is cached.
     * Tries the filesystem first, then resources.
     * This has to at least support JPGs, PNGs and BMPs.
     * Returns the result of getErrorImage on failure.
     */
    public static @NonNull IImage getImage(String a) {
        return getImageEx(a, true, true);
    }

    /**
     * Gets an image, and colour-keys away a colour. This is cached.
     */
    public static @NonNull IImage getImageCK(@NonNull String a, int r, int g, int b) {
        return getImageCKEx(a, true, true, r, g, b);
    }

    /**
     * Gets an image. You can specify if the image can be gotten from the filesystem directly or the resources.
     */
    public static @NonNull IImage getImageEx(@NonNull String a, boolean fs, boolean res) {
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

    /**
     * getImageEx with colour-key processing.
     */
    public static @NonNull IImage getImageCKEx(@NonNull String a, boolean fs, boolean res, int r, int g, int b) {
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

    /**
     * Returns the one true static error image.
     * This can be compared to image get results.
     */
    public static @NonNull IImage getErrorImage() {
        return errorImage;
    }

    /**
     * Creates an image from colours/width/height.
     */
    public static @NonNull IImage createImage(@NonNull int[] colours, int width, int height) {
        return createImage(null, colours, width, height);
    }

    /**
     * Creates an image from colours/width/height.
     * Has a debug ID to help keep track.
     */
    public static @NonNull IImage createImage(@Nullable String debugId, @NonNull int[] colours, int width, int height) {
        if (width <= 0)
            return new NullGrDriver();
        if (height <= 0)
            return new NullGrDriver();
        return new VopeksImage(GaBIEn.vopeks, debugId, width, height, colours);
    }

    /**
     * Creates a WSI image from colours/width/height.
     * This is only really useful when saving an image.
     */
    public static @NonNull WSIImage.RW createWSIImage(@NonNull int[] colours, int width, int height) {
        return internal.createWSIImage(colours, width, height);
    }

    /**
     * Clears the image cache.
     */
    public static void hintFlushAllTheCaches() {
        imageCache.hintFlushAllTheCaches();
    }

    /**
     * Returns the list of native font names.
     */
    public static @NonNull String[] getFontOverrides() {
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

    /**
     * File or directory exists.
     */
    public static boolean fileOrDirExists(@NonNull String s) {
        return mutableDataFS.getState(s) != null;
    }

    /**
     * Directory exists.
     */
    public static boolean dirExists(@NonNull String s) {
        return mutableDataFS.getState(s) instanceof DirectoryState;
    }

    /**
     * Wrapper around mutableDataFS.
     * Lists entries in a directory.
     * @return List of filenames, or null on error.
     */
    public static @Nullable String[] listEntries(@NonNull String s) {
        XState xs = mutableDataFS.getState(s);
        if (xs instanceof DirectoryState)
            return ((DirectoryState) xs).entries;
        return null;
    }

    /**
     * Converts a path to an absolute path.
     */
    public static @NonNull String absolutePathOf(@NonNull String s) {
        return mutableDataFS.absolutePathOf(s);
    }

    /**
     * Returns the name of a file.
     */
    public static @NonNull String nameOf(@NonNull String s) {
        return mutableDataFS.nameOf(s);
    }

    /**
     * See FSBackend.parentOf
     * TLDR: Returns null if no parent exists, is supposed to switch to absolute paths when necessary.
     */
    public static @Nullable String parentOf(@NonNull String s) {
        return mutableDataFS.parentOf(s);
    }

    /**
     * Makes directories up to the given path.
     */
    public static void makeDirectories(@NonNull String s) {
        mutableDataFS.mkdirs(s);
    }

    /**
     * Attempts to start a text editor. Returns false on failure.
     */
    public static boolean tryStartTextEditor(@NonNull String fpath) {
        return internal.tryStartTextEditor(fpath);
    }

    /**
     * Attempts to delete a file.
     */
    public static void rmFile(@NonNull String s) {
        try {
            mutableDataFS.delete(s);
        } catch (Exception e) {
            // errors are silenced
        }
    }

    /**
     * Sets the file browser directory.
     */
    public static void setBrowserDirectory(@NonNull String s) {
        internalFileBrowser.setBrowserDirectory(s);
    }

    /**
     * (This is a bit of an inconsistent one due to AWT being difficult.)
     * exts should just be left blank for now.
     * iConsumer is called as part of runCallbacks.
     * Regarding the path, the only guarantee is that it'll be null or a valid file path.
     * It does not necessarily have to match the standard gabien path separator.
     */
    public static void startFileBrowser(String s, boolean saving, String exts, IConsumer<String> iConsumer) {
        internalFileBrowser.startFileBrowser(s, saving, exts, iConsumer, "");
    }

    /**
     * Same as previous version, but now with an initial filename.
     */
    public static void startFileBrowser(String s, boolean saving, String exts, IConsumer<String> iConsumer, String initialName) {
        internalFileBrowser.startFileBrowser(s, saving, exts, iConsumer, initialName);
    }

    /**
     * invokeLater-alike for the gabien main thread.
     * This can be used by the application,
     *  but mostly exists as a way to get application callbacks called on the thread they are expected to be called on.
     */
    public static void pushCallback(Runnable r) {
        callbackQueueLock.lock();
        callbackQueue.add(r);
        callbackQueueLock.unlock();
    }

    /**
     * Like pushCallback, but occurs on the next time runCallbacks is run, after the current time.
     */
    public static void pushLaterCallback(Runnable runnable) {
        callbackQueueLock.lock();
        callbacksToAddAfterCallbacksQueue.add(runnable);
        callbackQueueLock.unlock();
    }

    /**
     * Runs callbacks.
     */
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

    /**
     * Attempts to end a frame with a decently consistent framerate while properly sleeping.
     * Also performs runCallbacks.
     */
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
        try {
            String[] str = (String[]) Class.forName("gabienapp.Application").getField("appPrefixes").get(null);
            appPrefixes = str;
            StringBuilder sb = new StringBuilder();
            sb.append("GaBIEn: Successfully set app resource prefixes to:");
            for (String s : appPrefixes) {
                sb.append(" \"");
                sb.append(s);
                sb.append("\"");
            }
            System.err.println(sb.toString());
        } catch (Exception ex) {
            // do nothing
        }
        if (setupTimeLogger && (timeLogger == null))
            timeLogger = new TimeLogger(getOutFile("gTimeLogger.bin"));
        // If VOPEKS has already been initialized, skip initializing it again.
        // It - thankfully - works independently of the rest of the system.
        // (Sadly, this can't be cleanly separated without removing a lot of really nice utility methods.)
        if (vopeks == null) {
            // Initialize VOPEKS
            if (!gabien.natives.Loader.defaultLoader(GaBIEn::getResource, internal::nativeDestinationSetup))
                System.err.println("GaBIEn: Natives did not initialize. And before it gets better, it's getting worse...");
            System.err.println("GaBIEn: Natives: " + gabien.natives.Loader.getNativesVersion());
            int newInstanceFlags = BadGPU.NewInstanceFlags.CanPrintf;
            if (debug)
                newInstanceFlags |= BadGPU.NewInstanceFlags.BackendCheck | BadGPU.NewInstanceFlags.BackendCheckAggressive;
            vopeks = new Vopeks(newInstanceFlags, timeLogger);
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
        // VOPEKS has started, we can now start user-level services.
        // Notably, because these use engine permission keys, they MUST be refreshed.
        nativeFontCache = new NativeFontCache(internal);
        imageCache = new ImageCache(internal);
        // These will hold references to dead assets if not reinitialized.
        engineFonts = new EngineFonts(internal);
        ThemingCentral.setupAssets(internal);
    }

    /**
     * This is used to verify that a call has come from "in-engine" (GaBIEn).
     * Some classes are internal API but need to communicate cross-package.
     * As such, the very secure* encapsulation abilities of Java are used for defensive programming.
     *
     * * Security subject to CVE-of-the-week. See "Phrack: Twenty years of Escaping the Java Sandbox (Ieu Eauvidoum &amp; disk noise)".
     * * Security also subject to the same issues that allow gabien backends and the test rig to operate in the first place.
     *   If you're abusing this, that's on you.
     */
    public static void verify(IGaBIEn engine) {
        if (internal != engine)
            throw new RuntimeException("Attempt to falsify engine permissions.");
    }
}
