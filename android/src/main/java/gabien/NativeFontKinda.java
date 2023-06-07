/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien;

import org.eclipse.jdt.annotation.NonNull;

import android.graphics.Canvas;
import android.graphics.Paint;
import gabien.text.IFixedSizeFont;
import gabien.text.ImageRenderedTextChunk;

/**
 * Implementation of NativeFont given the unique circumstances of the Android port.
 * Created 17th February 2023.
 */
public class NativeFontKinda implements IFixedSizeFont {
    public final int size;
    public final int space;
    public final Paint paint;

    public NativeFontKinda(int s) {
        size = s;
        paint = new Paint();
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        space = (int) paint.measureText(" ");
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int measureLine(@NonNull String text) {
        return (int) (paint.measureText(text) + space);
    }

    @Override
    public int measureLine(@NonNull char[] text, int index, int length) {
        // *hmm*... something seems off here.
        return (int) (paint.measureText(text, index, length) + space); // about the " " : it gets it wrong somewhat, by about this amount
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull String text, boolean textBlack) {
        int r = textBlack ? 0 : 255;
        int mt = measureLine(text);
        int margin = 16;
        WSIImageDriver wsi = new WSIImageDriver(null, margin + mt + margin, margin + size + margin);
        Canvas cv = new Canvas(wsi.bitmap);
        paint.setARGB(255, r, r, r);
        cv.drawText(text, margin, margin + ((size * 3) / 4), paint);
        return new ImageRenderedTextChunk.CPU(margin, margin, mt, size, wsi);
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull char[] text, int index, int length, boolean textBlack) {
        int r = textBlack ? 0 : 255;
        int mt = measureLine(text, index, length);
        int margin = 16;
        WSIImageDriver wsi = new WSIImageDriver(null, margin + mt + margin, margin + size + margin);
        Canvas cv = new Canvas(wsi.bitmap);
        paint.setARGB(255, r, r, r);
        cv.drawText(text, index, length, margin, margin + ((size * 3) / 4), paint);
        return new ImageRenderedTextChunk.CPU(margin, margin, mt, size, wsi);
    }
}
