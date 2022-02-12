/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import gabien.backendhelp.Blender;
import gabien.backendhelp.INativeImageHolder;

import java.io.ByteArrayOutputStream;

public class OsbDriver implements INativeImageHolder, IGrDriver {
    protected Bitmap bitmap;
    private Canvas canvas;
    protected int w, h;
    protected int[] localST;
    protected final Paint globalPaint;

    public OsbDriver(int w, int h, boolean alpha) {
        this(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
        if (!alpha)
            clearAll(0, 0, 0);
    }

    public OsbDriver(int w, int h, int[] ints) {
        this(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
        bitmap.setPixels(ints, 0, w, 0, 0, w, h);
    }

    private OsbDriver(Bitmap bt) {
        bt.setDensity(96);
        bitmap = bt;
        canvas = new Canvas(bt);
        canvas.save();
        globalPaint = new Paint();
        w = bt.getWidth();
        h = bt.getHeight();
        localST = new int[6];
        localST[4] = w;
        localST[5] = h;
    }

    protected void resize(int w, int h) {
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        localST = new int[6];
        localST[4] = w;
        localST[5] = h;
        canvas.save();
        this.w = w;
        this.h = h;
    }

    @Override
    public Runnable[] getLockingSequenceN() {
        return new Runnable[] {
                new Runnable() {
                    @Override
                    public void run() {
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                    }
                }
        };
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
    public int[] getPixels() {
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        return pixels;
    }

    @Override
    public byte[] createPNG() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {
        canvas.drawBitmap((Bitmap) ((INativeImageHolder) i).getNative(), new Rect(srcx, srcy, srcx + srcw, srcy + srch), new Rect(x, y, x + srcw, y + srch), globalPaint);
    }

    @Override
    public void blitTiledImage(int x, int y, int w, int h, IImage cachedTile) {
        // Deprecated because of blahblahblahdensityblah.
        // Call the density? I AM THE DENSITY!... that makes no sense, I assume.
        BitmapDrawable bd = new BitmapDrawable((Bitmap) (((INativeImageHolder) cachedTile).getNative()));
        bd.setBounds(x, y, x + w, y + h);
        bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        bd.draw(canvas);
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        canvas.drawBitmap((Bitmap) ((INativeImageHolder) i).getNative(), new Rect(srcx, srcy, srcx + srcw, srcy + srch), new Rect(x, y, x + acw, y + ach), globalPaint);
    }

    @Override
    public void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i) {
        canvas.save();
        canvas.translate(x + (acw / 2), y + (ach / 2));
        canvas.rotate(angle);
        canvas.translate(-(acw / 2), -(ach / 2));
        canvas.drawBitmap((Bitmap) ((INativeImageHolder) i).getNative(), new Rect(srcx, srcy, srcx + srcw, srcy + srch), new Rect(0, 0, acw, ach), globalPaint);
        canvas.restore();
    }

    @Override
    public void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean blendSub) {
        Blender.blendRotatedScaledImage(this, srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, blendSub);
    }

    @Override
    public void drawText(int x, int y, int r, int g, int b, int i, String text) {
        globalPaint.setARGB(255, r, g, b);
        globalPaint.setAntiAlias(true);
        globalPaint.setTextSize(i);
        canvas.drawText(text, x, y + ((i * 3) / 4), globalPaint);
        globalPaint.setAntiAlias(false);
        globalPaint.setARGB(255, 255, 255, 255);
    }

    @Override
    public void clearAll(int r, int g, int b) {
        canvas.drawRGB(r, g, b);
    }

    @Override
    public void clearRect(int r, int g, int b, int x, int y, int width, int height) {
        globalPaint.setARGB(255, r, g, b);
        canvas.drawRect(new Rect(x, y, x + width, y + height), globalPaint);
        globalPaint.setARGB(255, 255, 255, 255);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public int[] getLocalST() {
        return localST;
    }

    @Override
    public void updateST() {
        canvas.restore();
        canvas.save();
        canvas.clipRect(localST[2], localST[3], localST[4], localST[5]);
        canvas.translate(localST[0], localST[1]);
    }
}
