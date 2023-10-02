/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

import org.eclipse.jdt.annotation.NonNull;

import gabien.GaBIEn;
import gabien.text.IFixedSizeFont;
import gabien.text.ITypeface;

/**
 * Temporary native font cache to prevent a ton of lookups.
 * Created 16th February 2023.
 */
public final class NativeFontCache {
    private ITypeface defaultTypeface;

    private String lastTypefaceName = null;
    private ITypeface lastTypeface = null;

    private String lastFontName = null;
    private int lastFontSize = -1;
    private IFixedSizeFont lastFont = null;

    private int lastDefaultFontSize = -1;
    private IFixedSizeFont lastDefaultFont = null;

    private final IGaBIEn backend;

    public NativeFontCache(IGaBIEn backend) {
        this.backend = backend;
        GaBIEn.verify(backend);
    }

    private ITypeface getTypeface(@NonNull String name) {
        synchronized (this) {
            if (lastTypeface != null)
                if (lastTypefaceName.equals(name))
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
    public IFixedSizeFont getDefaultNativeFont(int size) {
        synchronized (this) {
            if (lastDefaultFont != null)
                if (lastDefaultFontSize == size)
                    return lastDefaultFont;
        }
        ITypeface gdt;
        synchronized (this) {
            if (defaultTypeface == null)
                defaultTypeface = backend.getDefaultTypeface();
            gdt = defaultTypeface;
        }
        IFixedSizeFont res = gdt.derive(size);
        synchronized (this) {
            lastDefaultFontSize = size;
            lastDefaultFont = res;
        }
        return res;
    }

    /**
     * Proxy to IGaBIEn.getNativeFont
     */
    public IFixedSizeFont getNativeFont(int size, String name) {
        synchronized (this) {
            if (lastFont != null)
                if (lastFontSize == size)
                    if (lastFontName.equals(name))
                        return lastFont;
        }
        ITypeface tf = getTypeface(name);
        if (tf == null)
            return null;
        IFixedSizeFont res = tf.derive(size);
        synchronized (this) {
            lastFontName = name;
            lastFontSize = size;
            lastFont = res;
        }
        return res;
    }
}
