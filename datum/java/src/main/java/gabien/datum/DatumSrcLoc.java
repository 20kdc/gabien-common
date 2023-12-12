/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Had to make one of these eventually.
 * Created 13th March 2023.
 */
public final class DatumSrcLoc {
    public static final DatumSrcLoc NONE = new DatumSrcLoc("NONE", 0);
    public final String filename;
    public final int lineNumber;
    public DatumSrcLoc(String fn, int ln) {
        filename = fn;
        lineNumber = ln;
    }
    @Override
    public String toString() {
        return filename + ":" + lineNumber;
    }
}
