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
import android.graphics.Typeface;
import gabien.text.FontStyle;
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

    public NativeFontKinda(Typeface tf, int s, int style) {
        size = s;
        paint = new Paint();
        paint.setTypeface(Typeface.create(tf, translateStyle(style)));
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        space = (int) paint.measureText(" ");
    }

    private static int translateStyle(int style) {
        int out = 0;
        if ((style & FontStyle.BOLD) != 0)
            out |= Typeface.BOLD;
        if ((style & FontStyle.ITALIC) != 0)
            out |= Typeface.ITALIC;
        return out;
    }

    @Override
    public int getLineHeight() {
        return size;
    }

    @Override
    public int getContentHeight() {
        return size - (size / 8);
    }

    @Override
    public int measureLine(@NonNull String text, boolean withLastAdvance) {
        return (int) (paint.measureText(text) + space);
    }

    @Override
    public int measureLine(@NonNull char[] text, int index, int length, boolean withLastAdvance) {
        // *hmm*... something seems off here.
        return (int) (paint.measureText(text, index, length) + space); // about the " " : it gets it wrong somewhat, by about this amount
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull String text, int r, int g, int b, int a) {
        int ascent = ((size * 3) / 4);
        int descent = size - ascent;
        int margin = 16;
        int mt = measureLine(text, false);
        int offsetX = margin, offsetY = margin + ascent;
        WSIImageDriver wsi = new WSIImageDriver(null, margin + mt + margin, margin + ascent + descent + margin);
        if (wsi.bitmap != null) {
            Canvas cv = new Canvas(wsi.bitmap);
            synchronized (this) {
                paint.setARGB(a, r, g, b);
                cv.drawText(text, offsetX, offsetY, paint);
            }
        }
        return new ImageRenderedTextChunk.WSI(-offsetX, -offsetY, mt, size, ascent, descent, wsi);
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull char[] text, int index, int length, int r, int g, int b, int a) {
        int ascent = ((size * 3) / 4);
        int descent = size - ascent;
        int margin = 16;
        int mt = measureLine(text, index, length, false);
        int offsetX = margin, offsetY = margin + ascent;
        WSIImageDriver wsi = new WSIImageDriver(null, margin + mt + margin, margin + ascent + descent + margin);
        if (wsi.bitmap != null) {
            Canvas cv = new Canvas(wsi.bitmap);
            synchronized (this) {
                paint.setARGB(a, r, g, b);
                cv.drawText(text, index, length, offsetX, offsetY, paint);
            }
        }
        return new ImageRenderedTextChunk.WSI(-offsetX, -offsetY, mt, size, ascent, descent, wsi);
    }
}
