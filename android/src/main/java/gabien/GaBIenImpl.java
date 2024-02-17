/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import gabien.audio.IRawAudioDriver;
import gabien.backend.EmulatedFileBrowser;
import gabien.backend.IGaBIEn;
import gabien.backend.WindowMux;
import gabien.render.WSIImage;
import gabien.text.ITypeface;
import gabien.uslx.vfs.impl.*;

import java.io.*;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The implementation of the runtime.
 */
public final class GaBIenImpl implements IGaBIEn {
    //In case you get confused.
    private RawAudioDriver rad;

    public static final String FONTNAME_ANDROID_DEFAULT = "Android Default";
    public static final String FONTNAME_ANDROID_BOLD = "Android Bold";
    public static final String FONTNAME_ANDROID_MONO = "Android Mono";
    public static final String FONTNAME_ANDROID_SANS = "Android Sans";
    public static final String FONTNAME_ANDROID_SERIF = "Android Serif";

    public static final ITypeface tfkDefault = new TypefaceKinda(Typeface.DEFAULT);
    public static final ITypeface tfkBold = new TypefaceKinda(Typeface.DEFAULT_BOLD);
    public static final ITypeface tfkMono = new TypefaceKinda(Typeface.MONOSPACE);
    public static final ITypeface tfkSans = new TypefaceKinda(Typeface.SANS_SERIF);
    public static final ITypeface tfkSerif = new TypefaceKinda(Typeface.SERIF);

    public static void main() throws Exception {
        GaBIEn.fontsReady = true;
        final GaBIenImpl impl = new GaBIenImpl();
    	GaBIEn.internal = impl;
    	GaBIEn.clipboard = new ClipboardImpl();
    	GaBIEn.mutableDataFS = JavaIOFSBackend.from(new File("/sdcard"));
    	// Use these "key files" to control startup. Only their existence matters, contents don't.
        GaBIEn.setupNativesAndAssets(AndroidPortGlobals.debugFlag, new File("/sdcard/gabien_android_enable_timelogger").exists());
        GaBIEnUI.setupAssets();
        AndroidPortGlobals.theMainWindow = new GrInDriver(800, 600);
    	GaBIEn.internalWindowing = new WindowMux(impl, AndroidPortGlobals.theMainWindow);
    	GaBIEn.internalFileBrowser = new EmulatedFileBrowser(impl);
    	Class.forName("gabienapp.Application").getDeclaredMethod("gabienmain").invoke(null);
    }

    /**
     * This is the central controller of where the "current directory" is.
     */
    public static File mutablePathToFile(String fileName) {
        File f = new File(fileName);
        if (f.isAbsolute())
            return f;
        return new File("/sdcard", fileName);
    }

    @Override
    public WSIImage.RW createWSIImage(@NonNull int[] colours, int width, int height) {
        return new WSIImageDriver(colours, width, height);
    }

    @Override
    @NonNull
    public String getDefaultNativeFontName() {
        return FONTNAME_ANDROID_DEFAULT;
    }

    @Override
    @NonNull
    public ITypeface getDefaultTypeface() {
        return tfkDefault;
    }

    @Override
    @Nullable
    public ITypeface getNativeTypeface(@NonNull String name) {
        if (name.equals(FONTNAME_ANDROID_DEFAULT))
            return tfkDefault;
        if (name.equals(FONTNAME_ANDROID_BOLD))
            return tfkBold;
        if (name.equals(FONTNAME_ANDROID_MONO))
            return tfkMono;
        if (name.equals(FONTNAME_ANDROID_SANS))
            return tfkSans;
        if (name.equals(FONTNAME_ANDROID_SERIF))
            return tfkSerif;
        return null;
    }

    @Override
    public String[] getFontOverrides() {
        return new String[] {
            FONTNAME_ANDROID_DEFAULT,
            FONTNAME_ANDROID_BOLD,
            FONTNAME_ANDROID_MONO,
            FONTNAME_ANDROID_SANS,
            FONTNAME_ANDROID_SERIF,
        };
    }

    @Override
    public boolean tryStartTextEditor(String fpath) {
        final File target = mutablePathToFile(fpath);
        AndroidPortGlobals.mainActivityLock.lock();
        final Context appCtx = AndroidPortGlobals.applicationContext;
        AndroidPortGlobals.mainActivity.runOnUiThread(() -> {
            try {
                Uri uri = Uri.fromFile(target);
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(uri, "text/plain");
                appCtx.startActivity(intent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        AndroidPortGlobals.mainActivityLock.unlock();
        // this doesn't usually *really* work, because of "security" features
        // there's no good workaround for this, only various levels of bad
        return false;
    }

    @Override
    public boolean tryStartBrowser(String url) {
        AndroidPortGlobals.mainActivityLock.lock();
        final Context appCtx = AndroidPortGlobals.applicationContext;
        AndroidPortGlobals.mainActivity.runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appCtx.startActivity(intent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        AndroidPortGlobals.mainActivityLock.unlock();
        return true;
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
    public WSIImage decodeWSIImage(@NonNull InputStream a) {
        try {
            Bitmap b = BitmapFactory.decodeStream(a);
            int w = b.getWidth();
            int h = b.getHeight();
            int[] data = new int[w * h];
            b.getPixels(data, 0, w, 0, 0, w, h);
            return new WSIImageDriver(data, w, h);
        } catch (Exception e) {
            return null;
        }
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
