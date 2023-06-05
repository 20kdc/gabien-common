/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import gabien.backendhelp.EmulatedFileBrowser;
import gabien.backendhelp.WindowMux;
import gabien.text.NativeFont;
import gabien.uslx.vfs.impl.*;

import java.io.*;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The implementation of the runtime.
 */
public final class GaBIenImpl implements IGaBIEn {
    //In case you get confused.
    private long startup = System.currentTimeMillis();
    private double lastdt = getTime();
    private RawAudioDriver rad;

    public static void main() throws Exception {
        FontManager.fontsReady = true;
        final GaBIenImpl impl = new GaBIenImpl();
    	GaBIEn.internal = impl;
    	GaBIEn.clipboard = new ClipboardImpl();
    	GaBIEn.mutableDataFS = new JavaIOFSBackend() {
    	    @Override
    	    public File asFile(String fileName) {
    	        File f = new File(fileName);
    	        if (f.isAbsolute())
    	            return f;
    	        return new File("/sdcard", fileName);
    	    }
    	};
        GaBIEn.setupNativesAndAssets();
    	GaBIEn.internalWindowing = new WindowMux(AndroidPortGlobals.theMainWindow);
    	GaBIEn.internalFileBrowser = new EmulatedFileBrowser();
    	Class.forName("gabienapp.Application").getDeclaredMethod("gabienmain").invoke(null);
    }

    public double getTime() {
        return (System.currentTimeMillis() - startup) / 1000.0d;
    }

    public double timeDelta(boolean reset) {
        double dt = getTime() - lastdt;
        if (reset)
            lastdt = getTime();
        return dt;
    }

    @Override
    public IImage createImage(int[] colours, int width, int height) {
        return new OsbDriver(width, height, colours);
    }

    @Override
    @NonNull
    public NativeFont getDefaultNativeFont(int size) {
        return new NativeFontKinda(size);
    }

    @Override
    @Nullable
    public NativeFont getNativeFont(int size, @NonNull String name) {
        return null;
    }

    @Override
    public String[] getFontOverrides() {
        return new String[] {
                "Nautilus"
        };
    }

    @Override
    public boolean tryStartTextEditor(String fpath) {
        // Maybe autodetect OI Notepad for this.
        return false;
    }

    public InputStream getResource(String resource) {
        AssetManager am = AndroidPortGlobals.applicationContext.getAssets();
        try {
            return am.open(resource);
        } catch (Exception e) {
            return null;
        }
//        return ClassLoader.getSystemClassLoader().getResourceAsStream(resource);
    }

    @Override
    public IGrDriver makeOffscreenBuffer(int w, int h, boolean alpha) {
        return new OsbDriver(w, h, alpha);
    }

    @Override
    public IImage getImage(String a, boolean res) {
        try {
            Bitmap b = BitmapFactory.decodeStream(res ? getResource(a) : GaBIEn.getInFile(a));
            int w = b.getWidth();
            int h = b.getHeight();
            int[] data = new int[w * h];
            b.getPixels(data, 0, w, 0, 0, w, h);
            return new OsbDriver(w, h, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean singleWindowApp() {
    	return true;
    }

    @Override
    public IRawAudioDriver getRawAudio() {
        if (rad == null)
            rad = new RawAudioDriver();
        return rad;
    }

    @Override
    public void hintShutdownRawAudio() {
        if (rad != null) {
            rad.keepAlive = false;
            rad = null;
        }
    }

    public void ensureQuit() {
        System.exit(0);
    }

    @Override
    public File nativeDestinationSetup(String name) {
        File dataDir = new File(AndroidPortGlobals.applicationContext.getApplicationInfo().dataDir);
        // don't delete on exit; tablets might not like writing over this data
        // gabien.natives.Loader should be kind about this
        return new File(dataDir, "nativeDestinationSetup." + name);
    }
}
