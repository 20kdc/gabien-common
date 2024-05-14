/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien;

import org.eclipse.jdt.annotation.NonNull;

import android.graphics.Typeface;
import gabien.text.IFixedSizeFont;
import gabien.text.ITypeface;

/**
 * Created 24th June, 2023.
 */
public class TypefaceKinda implements ITypeface {
    // typefaces in the supported styles
    public final Typeface typeface;
    public TypefaceKinda(Typeface base) {
        typeface = base;
    }

    @Override
    @NonNull
    public IFixedSizeFont derive(int size, int style) {
        return new NativeFontKinda(typeface, size, style);
    }
}
