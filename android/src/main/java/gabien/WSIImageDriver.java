/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import android.graphics.*;
import gabien.backendhelp.INativeImageHolder;

import java.io.ByteArrayOutputStream;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Copied from OsbDriver 7th June 2023.
 */
public class WSIImageDriver implements INativeImageHolder, IWSIImage.RW {
    protected Bitmap bitmap;
    protected int w, h;

    public WSIImageDriver(int[] ints, int w, int h) {
        this(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
        bitmap.setPixels(ints, 0, w, 0, 0, w, h);
    }

    private WSIImageDriver(Bitmap bt) {
        bt.setDensity(96);
        bitmap = bt;
        w = bt.getWidth();
        h = bt.getHeight();
    }

    @Override
    public Object getNative() {
        return bitmap;
    }

    @Override
    public int getWidth() {
        return bitmap.getWidth();
    }

    @Override
    public int getHeight() {
        return bitmap.getHeight();
    }

    @Override
    public @NonNull int[] getPixels() {
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        return pixels;
    }

    @Override
    public void setPixels(@NonNull int[] colours) {
        bitmap.setPixels(colours, 0, w, 0, 0, w, h);
    }

    @Override
    @NonNull
    public byte[] createPNG() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
}
