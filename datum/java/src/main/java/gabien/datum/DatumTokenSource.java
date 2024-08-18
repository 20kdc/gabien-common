/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.util.Stack;

/**
 * Stream of read-in Datum tokens.
 * Also contains the logic that turns this into a parsed stream, so this is the parser too.
 * Created February 16th, 2023.
 */
public abstract class DatumTokenSource {
    /**
     * Reads a token.
     * Returns true if a token was read successfully (see contents, type)
     */
    public abstract boolean read();

    /**
     * Contents of last read token, if any.
     * Meaning is dependent on type but is pretty obvious for cases where it isn't null.
     */
    public abstract String contents();

    /**
     * A human-readable representation of the position within the token stream.
     */
    public abstract String position();

    /**
     * A computer-readable representation of the position within the token stream (for less severe errors).
     * Maybe this should be refactored at some later point.
     */
    public abstract DatumSrcLoc srcLoc();

    /**
     * Type of last read token.
     */
    public abstract DatumTokenType type();

    /**
     * Parses a value from the token stream into a visitor.
     */
    public final boolean visitValue(DatumVisitor visitor) {
        Stack<DatumVisitor> storedListVisitors = new Stack<>();
        Stack<String> storedListStarts = new Stack<>();
        String listStart = null;
        while (true) {
            // First token of the value.
            if (!read()) {
                if (storedListVisitors.isEmpty())
                    return false;
                throw new RuntimeException(listStart + ": EOF during list started here");
            }
            switch (type()) {
            case ID:
                visitor.visitId(contents(), srcLoc());
                break;
            case SpecialID:
            {
                String c = contents();
                if (c.equalsIgnoreCase("t")) {
                    visitor.visitBoolean(true, srcLoc());
                } else if (c.equalsIgnoreCase("f")) {
                    visitor.visitBoolean(false, srcLoc());
                } else if (c.equals("{}#")) {
                    visitor.visitId("", srcLoc());
                } else if (c.equalsIgnoreCase("nil")) {
                    visitor.visitNull(srcLoc());
                } else if (c.startsWith("i") || c.startsWith("I")) {
                    visitNumeric(visitor, c.substring(1));
                } else {
                    visitor.visitSpecialUnknown(c, srcLoc());
                }
            }
                break;
            case String:
                visitor.visitString(contents(), srcLoc());
                break;
            case ListStart:
                storedListVisitors.push(visitor);
                storedListStarts.push(listStart);
                visitor = visitor.visitList(srcLoc());
                listStart = position();
                break;
            case ListEnd:
                if (storedListVisitors.isEmpty())
                    throw new RuntimeException(position() + ": List end with no list");
                visitor.visitEnd(srcLoc());
                visitor = storedListVisitors.pop();
                listStart = storedListStarts.pop();
                break;
            case Numeric:
                visitNumeric(visitor, contents());
                break;
            }
            // Read in a value (or started or ended a list), so if the stack is empty we are done here
            if (storedListVisitors.isEmpty())
                return true;
        }
    }

    private final void visitNumeric(DatumVisitor visitor, String c) {
        if (c.equalsIgnoreCase("+inf.0")) {
            visitor.visitFloat(Double.POSITIVE_INFINITY, c, srcLoc());
            return;
        } else if (c.equalsIgnoreCase("-inf.0")) {
            visitor.visitFloat(Double.NEGATIVE_INFINITY, c, srcLoc());
            return;
        } else if (c.equalsIgnoreCase("+nan.0")) {
            visitor.visitFloat(Double.NaN, c, srcLoc());
            return;
        }
        // Conversion...
        long l = 0L;
        try {
            l = Long.valueOf(c);
        } catch (NumberFormatException nfe1) {
            double d = 0d;
            try {
                d = Double.valueOf(c);
            } catch (NumberFormatException nfe2) {
                visitor.visitNumericUnknown(c, srcLoc());
                return;
            }
            visitor.visitFloat(d, c, srcLoc());
            return;
        }
        visitor.visitInt(l, c, srcLoc());
    }

    /**
     * Parses the whole token stream into a visitor.
     */
    public final void visit(DatumVisitor visitor) {
        while (visitValue(visitor));
    }
}
