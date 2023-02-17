/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Wrapper around String for symbols in the tree form.
 * Ideally, this would be unwrapped immediately, but a few measures have been taken to ensure things are okay if you don't.
 * Created 15th February 2023.
 */
public final class DatumSymbol implements Comparable<DatumSymbol> {
    public final String id;
    public DatumSymbol(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object var1) {
        if (var1 instanceof DatumSymbol)
            return id.equals(((DatumSymbol) var1).id);
        return false;
    }

    @Override
    public int compareTo(DatumSymbol var1) {
        return id.compareTo(var1.id);
    }
}
