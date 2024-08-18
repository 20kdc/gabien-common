/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Utilities to identify characters.
 * Created 15th February 2023, turned into an enum the next day.
 */
public enum DatumCharClass {
    Content(true, null),
    Whitespace(false, null),
    Newline(false, null),
    LineComment(false, null),
    String(false, null),
    ListStart(false, DatumTokenType.ListStart),
    ListEnd(false, DatumTokenType.ListEnd),
    SpecialID(true, null),
    NumericStart(true, null);

    /**
     * If true, this is valid in potential identifiers.
     */
    public final boolean isValidPID;

    /**
     * If non-null, this character class is supposed to represent an alone token.
     */
    public final DatumTokenType aloneToken;

    DatumCharClass(boolean pid, DatumTokenType alone) {
        isValidPID = pid;
        aloneToken = alone;
    }

    public static DatumCharClass identify(char c) {
        // Order matters here, newline needs to override whitespace, everything overrides Content
        if (c == '\n')
            return Newline;
        if (c <= 32 || c == 127)
            return Whitespace;
        if (c == ';')
            return LineComment;
        if (c == '"')
            return String;
        if (c == '(')
            return ListStart;
        if (c == ')')
            return ListEnd;
        if (c == '#')
            return SpecialID;
        if ((c >= '0' && c <= '9') || c == '-')
            return NumericStart;
        return Content;
    }
}
