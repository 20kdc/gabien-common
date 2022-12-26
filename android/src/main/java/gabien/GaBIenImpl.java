/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import gabien.backendhelp.EmulatedFileBrowser;
import gabien.backendhelp.WindowMux;
import gabien.uslx.vfs.impl.*;

import java.io.*;
import java.util.HashMap;

/**
 * The implementation of the runtime.
 */
public final class GaBIenImpl implements IGaBIEn {
    //In case you get confused.
    private long startup = System.currentTimeMillis();
    public HashMap<String, IImage> ht = new HashMap<String, IImage>();
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
    public void hintFlushAllTheCaches() {
        ht.clear();
    }

    @Override
    public int measureText(int i, String text) {
        // *hmm*... something seems off here.
        // figure out what units this uses, do the usual awful hax
        Paint p = new Paint();
        p.setTextSize(i);
        return (int) p.measureText(text + " "); // about the " " : it gets it wrong somewhat, by about this amount
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

    private IImage getImageInternal(String a, boolean res, String id, boolean ck, int i) {
        if (ht.containsKey(id))
            return ht.get(id);
        IImage r = GaBIEn.getErrorImage();
        try {
            Bitmap b = BitmapFactory.decodeStream(res ? getResource(a) : GaBIEn.getInFile(a));
            int w = b.getWidth();
            int h = b.getHeight();
            int[] data = new int[w * h];
            b.getPixels(data, 0, w, 0, 0, w, h);
            if (ck)
                for (int j = 0; j < data.length; j++)
                    if ((data[j] & 0xFFFFFF) == i) {
                        data[j] = 0;
                    } else {
                        data[j] |= 0xFF000000;
                    }
            r = new OsbDriver(w, h, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ht.put(id, r);
        return r;
    }


    @Override
    public IImage getImage(String a, boolean res) {
        return getImageInternal(a, res, (res ? 'R' : 'F') + "~" + a, false, -1);
    }

    @Override
    public IImage getImageCK(String a, boolean res, int r, int g, int b) {
        r &= 0xFF;
        g &= 0xFF;
        b &= 0xFF;
        return getImageInternal(a, res, (res ? 'R' : 'F') + "X" + r + " " + g + " " + b + "~" + a, true, (r << 16) | (g << 8) | b);
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

}
