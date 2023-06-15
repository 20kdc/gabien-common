/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * This is expected to visit the contents of a list.
 * This pairs nicely with DatumKVDVisitor.
 * Created 15th June 2023.
 */
public abstract class DatumSeqVisitor extends DatumEncodingProxyVisitor {
    protected int seqPos;

    public DatumSeqVisitor() {
        super(DatumInvalidVisitor.INSTANCE);
        target = handle(0);
    }

    @Override
    public void onVisitedValue() {
        // switch to next sequence position
        seqPos++;
        target = handle(seqPos);
    }

    public abstract DatumVisitor handle(int idx);
}
