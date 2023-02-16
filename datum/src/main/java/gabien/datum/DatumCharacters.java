/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Utilities to identify characters.
 * Created 15th February 2022.
 */
public class DatumCharacters {
    public static boolean isNumericStart(char c) {
        if (c >= '0' && c <= '9')
            return true;
        if (c == '-')
            return true;
        return false;
    }

    public static boolean isContent(char c) {
        if (c <= 32 || c == 127 || c == ';' || c == '"' || c == '\'' || c == '(' || c == ')')
            return false;
        return true;
    }

    public static boolean isWhitespace(char c) {
        return c <= 32 || c == 127;
    }

    public static boolean isSpecial(char c) {
        return c == ';' || c == '"';
    }

    public static boolean isAlone(char c) {
        return c == '\'' || c == '(' || c == ')';
    }
}
