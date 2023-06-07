/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien;

import gabien.text.IFixedSizeFont;

/**
 * Temporary native font cache to prevent a ton of lookups.
 * Created 16th February 2023.
 */
final class NativeFontCache {
    private String lastFontName = null;
    private int lastFontSize = -1;
    private IFixedSizeFont lastFont = null;

    private int lastDefaultFontSize = -1;
    private IFixedSizeFont lastDefaultFont = null;

    /**
     * Proxy to IGaBIEn.getDefaultNativeFont
     */
    IFixedSizeFont getDefaultNativeFont(int size) {
        synchronized (this) {
            if (lastDefaultFont != null)
                if (lastDefaultFontSize == size)
                    return lastDefaultFont;
        }
        IFixedSizeFont res = GaBIEn.internal.getDefaultNativeFont(size);
        synchronized (this) {
            lastDefaultFontSize = size;
            lastDefaultFont = res;
        }
        return res;
    }

    /**
     * Proxy to IGaBIEn.getNativeFont
     */
    IFixedSizeFont getNativeFont(int size, String name) {
        synchronized (this) {
            if (lastFont != null)
                if (lastFontSize == size)
                    if (lastFontName.equals(name))
                        return lastFont;
        }
        IFixedSizeFont res = GaBIEn.internal.getNativeFont(size, name);
        if (res == null)
            return null;
        synchronized (this) {
            lastFontName = name;
            lastFontSize = size;
            lastFont = res;
        }
        return res;
    }
}
