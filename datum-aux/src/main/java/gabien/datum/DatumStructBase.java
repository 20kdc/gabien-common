/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.datum;

import datum.DatumVisitor;

/**
 * Base interface for things that can be 'filled in' using a Datum visitor.
 * Created 1st February, 2026
 */
public interface DatumStructBase<GT> {
    /**
     * Creates a visitor to fill in this class with a surrounding ExpectList.
     */
    default DatumVisitor newListWrappedVisitor(GT globalContext) {
        return new DatumExpectListVisitor(() -> newVisitor(globalContext));
    }

    /**
     * Creates a new visitor to fill in this class.
     * This visitor is assumed to be inside a list, either at the head or immediately after, and is consuming the list to its end.
     */
    DatumVisitor newVisitor(GT context);
}
