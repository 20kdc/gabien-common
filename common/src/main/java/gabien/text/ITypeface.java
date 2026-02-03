/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

/**
 * Typeface derivable into fixed size fonts.
 * Created 24th June, 2023.
 */
public interface ITypeface {
    /**
     * Derives a fixed size font given a font size.
     * This may provide a smaller/larger font than you asked for, but only in unusual situations (i.e. bitmap fonts).
     * A smaller font will be preferred to a larger one.
     */
    IFixedSizeFont derive(int size, int style);
}
