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
 * Created 16th February 2022.
 */
public abstract class DatumTokenStream {
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
     * Type of last read token.
     */
    public abstract DatumTokenType type();

    /**
     * Parses a value from the token stream into a visitor.
     */
    public final boolean visitValue(DatumVisitor visitor) {
        Stack<DatumVisitor> storedListVisitors = new Stack<>();
        while (true) {
            // First token of the value.
            if (!read()) {
                if (storedListVisitors.isEmpty())
                    return false;
                throw new RuntimeException(position() + ": EOF during list");
            }
            switch (type()) {
            case ID:
                visitor.visitId(contents());
                break;
            case String:
                visitor.visitString(contents());
                break;
            case ListStart:
                storedListVisitors.push(visitor);
                visitor = visitor.visitList();
                break;
            case ListEnd:
                if (storedListVisitors.isEmpty())
                    throw new RuntimeException(position() + ": List end with no list");
                visitor.visitEnd();
                visitor = storedListVisitors.pop();
                break;
            case Numeric:
                // Conversion...
            {
                String c = contents();
                double d = 0d;
                long l = 0L;
                try {
                    d = Double.valueOf(c);
                } catch (NumberFormatException nfe1) {
                    try {
                        l = Long.valueOf(c);
                    } catch (NumberFormatException nfe2) {
                        visitor.visitNumericUnknown(c);
                        break;
                    }
                    visitor.visitInt(l, c);
                    break;
                }
                visitor.visitFloat(d, c);
                break;
            }
            case Quote:
            {
                DatumVisitor dv = visitor.visitList();
                dv.visitId("quote");
                // This works better because it doesn't allow quote followed by list-end
                if (!visitValue(dv))
                    throw new RuntimeException(position() + ": Hit quote, but did not get a value");
                dv.visitEnd();
            }
                break;
            }
            // Read in a value (or started or ended a list), so if the stack is empty we are done here
            if (storedListVisitors.isEmpty())
                return true;
        }
    }

    /**
     * Parses the whole token stream into a visitor.
     */
    public final void visit(DatumVisitor visitor) {
        while (visitValue(visitor));
    }
}
