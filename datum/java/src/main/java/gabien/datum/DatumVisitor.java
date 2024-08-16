/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Mechanism for receiving Datums.
 * Created 15th February 2023.
 */
public abstract class DatumVisitor {
    // Atoms

    /**
     * Called to visit a string.
     */
    public abstract void visitString(String s, DatumSrcLoc loc);

    /**
     * Called to visit an identifier.
     */
    public abstract void visitId(String s, DatumSrcLoc loc);

    /**
     * Called to visit an undecodable numeric value.
     */
    public abstract void visitNumericUnknown(String s, DatumSrcLoc loc);

    /**
     * Called to visit an undecodable special identifier.
     * The string does not include the opening '#'.
     */
    public abstract void visitSpecialUnknown(String s, DatumSrcLoc loc);

    /**
     * Called to visit a boolean.
     */
    public abstract void visitBoolean(boolean value, DatumSrcLoc loc);

    /**
     * Called to visit null.
     */
    public abstract void visitNull(DatumSrcLoc loc);

    /**
     * Called to visit an integer.
     */
    public abstract void visitInt(long value, String raw, DatumSrcLoc loc);

    /**
     * Called to visit an integer.
     */
    public final void visitInt(long value, DatumSrcLoc loc) {
        visitInt(value, Long.toString(value), loc);
    }

    /**
     * Called to visit a float.
     * The raw value is equivalent to the parameter to visitNumericUnknown.
     */
    public abstract void visitFloat(double value, String raw, DatumSrcLoc loc);

    /**
     * Called to visit a float.
     */
    public final void visitFloat(double value, DatumSrcLoc loc) {
        if (!Double.isFinite(value)) {
            if (Double.isInfinite(value)) {
                if (value > 0)
                    visitFloat(value, "+inf.0", loc);
                else
                    visitFloat(value, "-inf.0", loc);
            } else {
                visitFloat(value, "+nan.0", loc);
            }
        } else {
            visitFloat(value, Double.toString(value), loc);
        }
    }

    // List start/end

    /**
     * Called when entering a list.
     * The DatumVisitor returned will visit the entire list, then visitEnd will be called on it.
     */
    public abstract DatumVisitor visitList(DatumSrcLoc loc);

    /**
     * Called on the list DatumVisitor when leaving a list.
     * (Notably, this won't get called at root level.)
     */
    public abstract void visitEnd(DatumSrcLoc loc);

    // Utilties

    /**
     * Either DatumDecodingVisitor implements this, or DatumEncodingVisitor implements everything else.
     * Which you choose depends on which API you want.
     */
    public abstract void visitTree(Object obj, DatumSrcLoc loc);
}
