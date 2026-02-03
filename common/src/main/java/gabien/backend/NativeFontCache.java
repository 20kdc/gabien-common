/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.text.IFixedSizeFont;
import gabien.text.ITypeface;

/**
 * Temporary native font cache to prevent a ton of lookups.
 * Created 16th February 2023.
 */
public final class NativeFontCache {
    private @Nullable ITypeface defaultTypeface;

    private @Nullable String lastTypefaceName = null;
    private @Nullable ITypeface lastTypeface = null;

    private @Nullable String lastFontName = null;
    private int lastFontSize = -1;
    private int lastFontStyle = -1;
    private @Nullable IFixedSizeFont lastFont = null;

    private int lastDefaultFontSize = -1;
    private int lastDefaultFontStyle = -1;
    private @Nullable IFixedSizeFont lastDefaultFont = null;

    private final IGaBIEn backend;

    public NativeFontCache(IGaBIEn backend) {
        this.backend = backend;
        GaBIEn.verify(backend);
    }

    private @Nullable ITypeface getTypeface(String name) {
        synchronized (this) {
            if (lastTypeface != null)
                if (lastTypefaceName != null && lastTypefaceName.equals(name))
                    return lastTypeface;
        }
        ITypeface tf = backend.getNativeTypeface(name);
        if (tf == null)
            return null;
        synchronized (this) {
            lastTypeface = tf;
            lastTypefaceName = name;
        }
        return tf;
    }

    /**
     * Proxy to IGaBIEn.getDefaultNativeFont
     */
    public IFixedSizeFont getDefaultNativeFont(int size, int style) {
        synchronized (this) {
            if (lastDefaultFont != null)
                if (lastDefaultFontSize == size)
                    if (lastDefaultFontStyle == style)
                        return lastDefaultFont;
        }
        ITypeface gdt;
        synchronized (this) {
            gdt = defaultTypeface;
            if (gdt == null)
                defaultTypeface = gdt = backend.getDefaultTypeface();
        }
        IFixedSizeFont res = gdt.derive(size, style);
        synchronized (this) {
            lastDefaultFontSize = size;
            lastDefaultFontStyle = style;
            lastDefaultFont = res;
        }
        return res;
    }

    /**
     * Proxy to IGaBIEn.getNativeFont
     */
    public @Nullable IFixedSizeFont getNativeFont(int size, int style, String name) {
        synchronized (this) {
            if (lastFont != null)
                if (lastFontSize == size)
                    if (lastFontStyle == style)
                        if (lastFontName != null && lastFontName.equals(name))
                            return lastFont;
        }
        ITypeface tf = getTypeface(name);
        if (tf == null)
            return null;
        IFixedSizeFont res = tf.derive(size, style);
        synchronized (this) {
            lastFontName = name;
            lastFontSize = size;
            lastFontStyle = style;
            lastFont = res;
        }
        return res;
    }
}
