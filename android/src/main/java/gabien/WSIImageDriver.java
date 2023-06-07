/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import android.graphics.*;

import java.io.ByteArrayOutputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Copied from OsbDriver 7th June 2023, before that class was then expunged anyway.
 * The first time it was seen in commit logs was February 12th, 2022, but I know better.
 * The underlying code's been around in some form or another for a long, long time; since the original I.M.E. on Android tests.
 * Dear goodness, has it really been ten years?
 */
public class WSIImageDriver implements IWSIImage.RW {
    protected final @Nullable Bitmap bitmap;
    protected final int w, h;

    public WSIImageDriver(@Nullable int[] ints, int w, int h) {
        if (w > 0 && h > 0) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            if (ints != null)
                bitmap.setPixels(ints, 0, w, 0, 0, w, h);
            this.w = w;
            this.h = h;
        } else {
            bitmap = null;
            this.w = 0;
            this.h = 0;
        }
    }

    @Override
    public int getWidth() {
        if (bitmap == null)
            return 0;
        return bitmap.getWidth();
    }

    @Override
    public int getHeight() {
        if (bitmap == null)
            return 0;
        return bitmap.getHeight();
    }

    @Override
    public void getPixels(@NonNull int[] pixels) {
        if (bitmap != null)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
    }

    @Override
    public void setPixels(@NonNull int[] colours) {
        if (bitmap != null)
            bitmap.setPixels(colours, 0, w, 0, 0, w, h);
    }

    @Override
    @NonNull
    public byte[] createPNG() {
        if (bitmap == null)
            return new WSIImageDriver(null, 1, 1).createPNG();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
}
