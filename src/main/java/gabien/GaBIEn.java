/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien;

import java.io.InputStream;
import java.io.OutputStream;

public class GaBIEn {
    protected static IGaBIEn internal;
    private static SimpleMixer soundImpl;

    public static double getTime() {
        return internal.getTime();
    }

    public static double timeDelta(boolean reset) {
        return internal.timeDelta(reset);
    }

    public static InputStream getResource(String resource) {
        return internal.getResource(resource);
    }

    public static InputStream getFile(String resource) {
        InputStream ifs = internal.getFile(resource);
        if (ifs == null)
            return internal.getResource(resource);
        return ifs;
    }

    public static boolean singleWindowApp() {
        return internal.singleWindowApp();
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
        return internal.defaultWindowSpecs(name, w, h);
    }

    public static IGrInDriver makeGrIn(String name, int w, int h) {
        WindowSpecs ws = defaultWindowSpecs(name, w, h);
        return makeGrIn(name, w, h, ws);
    }

    public static IGrInDriver makeGrIn(String name, int w, int h, WindowSpecs specs) {
        return internal.makeGrIn(name, w, h, specs);
    }

    // Note: The buffer does not have an alpha channel.
    public static IOsbDriver makeOffscreenBuffer(int w, int h, boolean alpha) {
        return internal.makeOffscreenBuffer(w, h, alpha);
    }

    public static OutputStream getOutFile(String string) {
        return internal.getOutFile(string);
    }

    // This has to at least support PNGs and BMPs.
    public static IGrInDriver.IImage getImage(String a) {
        return internal.getImage(a);
    }
    public static IGrInDriver.IImage getImageCK(String a, int r, int g, int b) {
        return internal.getImageCK(a, r, g, b);
    }

    public static IGrInDriver.IImage createImage(int[] colours, int width, int height) {
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
}
