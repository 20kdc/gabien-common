/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien;

import android.graphics.Paint;
import gabien.text.NativeFont;

/**
 * Implementation of NativeFont given the unique circumstances of the Android port.
 * Created 17th February 2023.
 */
public class NativeFontKinda extends NativeFont {
    public final int size;

    public NativeFontKinda(int s) {
        size = s;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int measureLine(String text) {
        // *hmm*... something seems off here.
        Paint p = new Paint();
        p.setTextSize(size);
        return (int) p.measureText(text + " "); // about the " " : it gets it wrong somewhat, by about this amount
    }
}
