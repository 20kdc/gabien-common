/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien;

import java.awt.Font;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.text.IFixedSizeFont;
import gabien.text.ITypeface;

/**
 * Created 24th June, 2023.
 */
public final class AWTTypeface implements ITypeface {
    public final Font fontPlain;

    public AWTTypeface(Font fontPlain) {
        this.fontPlain = fontPlain;
    }

    public static @NonNull ITypeface getDefaultTypeface() {
        try {
            return new AWTTypeface(new Font(GaBIEnImpl.getDefaultFont(), Font.PLAIN, 1));
        } catch (Exception ex) {
        }
        // Shouldn't happen, so return a fake font as if we know what we're doing.
        System.err.println("AWTNativeFont failed to get fallback font, so an engine default was used.");
        return GaBIEn.engineFonts;
    }

    public static @Nullable ITypeface getTypeface(String s) {
        try {
            return new AWTTypeface(new Font(s, Font.PLAIN, 1));
        } catch (Exception ex) {
        }
        return null;
    }

    @Override
    @NonNull
    public IFixedSizeFont derive(int size) {
        // This MUST be float because deriveFont is weird.
        float szf = size - (size / 8);
        return new AWTNativeFont(fontPlain.deriveFont(szf), size);
    }
}
