/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.uslx.append.*;
import gabien.uslx.vfs.FSBackend;
import gabien.uslx.vfs.FSBackend.DirectoryState;
import gabien.uslx.vfs.FSBackend.XState;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.Nullable;

public class GaBIEn {
    protected static IGaBIEn internal;
    protected static IGaBIEnMultiWindow internalWindowing;
    protected static IGaBIEnFileBrowser internalFileBrowser;
    public static IGaBIEnClipboard clipboard;
    public static FSBackend mutableDataFS;
    private static IImage errorImage;
    private static ReentrantLock callbackQueueLock = new ReentrantLock();
    private static LinkedList<Runnable> callbackQueue = new LinkedList<Runnable>();
    private static LinkedList<Runnable> callbacksToAddAfterCallbacksQueue = new LinkedList<Runnable>();

    // Additional resource load locations.
    public static String[] appPrefixes = new String[0];
    // Can be used by internal UI.
    public static String wordSave = "Save", wordLoad = "Load";
    public static String wordInvalidFileName = "Invalid or missing file name.";
    public static int sysCoreFontSize = 8;

    public static double getTime() {
        return internal.getTime();
    }

    public static double timeDelta(boolean reset) {
        return internal.timeDelta(reset);
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

    // Note: The buffer does not have an alpha channel.
    public static IGrDriver makeOffscreenBuffer(int w, int h, boolean alpha) {
        return internal.makeOffscreenBuffer(w, h, alpha);
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
        if (fs) {
            IImage r = internal.getImage(a, false);
            if (r != getErrorImage())
                return r;
        }
        if (res) {
            for (String s : appPrefixes) {
                IImage r = internal.getImage(s + a, false);
                if (r != getErrorImage())
                    return r;
            }
            return internal.getImage(a, true);
        }
        return getErrorImage();
    }

    public static IImage getImageCKEx(String a, boolean fs, boolean res, int r, int g, int b) {
        if (fs) {
            IImage ri = internal.getImageCK(a, false, r, g, b);
            if (ri != getErrorImage())
                return ri;
        }
        if (res) {
            for (String s : appPrefixes) {
                IImage ri = internal.getImageCK(s + a, false, r, g, b);
                if (ri != getErrorImage())
                    return ri;
            }
            return internal.getImageCK(a, true, r, g, b);
        }
        return getErrorImage();
    }

    public static IImage getErrorImage() {
        if (errorImage == null) {
            errorImage = createImage(new int[] {
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
    public static IImage createImage(int[] colours, int width, int height) {
        return internal.createImage(colours, width, height);
    }

    public static void hintFlushAllTheCaches() {
        internal.hintFlushAllTheCaches();
    }

    public static int measureText(int i, String text) {
        return internal.measureText(i, text);
    }

    public static String[] getFontOverrides() {
        return internal.getFontOverrides();
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
}
