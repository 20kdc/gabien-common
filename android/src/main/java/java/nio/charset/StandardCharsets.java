/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package java.nio.charset;

/**
 * Created 13th June 2023 as yet another polyfill
 * Why wasn't this in the base Android API from API level 1?!?!?
 */
public final class StandardCharsets {
    public static final Charset ISO_8859_1 = prep("ISO-8859-1");
    public static final Charset US_ASCII = prep("US-ASCII");
    public static final Charset UTF_16 = prep("UTF-16");
    public static final Charset UTF_16BE = prep("UTF-16BE");
    public static final Charset UTF_16LE = prep("UTF-16LE");
    public static final Charset UTF_8 = prep("UTF-8");
    private StandardCharsets() {
    }
    private static final Charset prep(String id) {
        try {
            return Charset.forName(id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

