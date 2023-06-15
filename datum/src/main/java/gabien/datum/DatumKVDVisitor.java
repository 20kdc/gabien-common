/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * This is expected to be used to visit the contents of a list for key/value pairs.
 * Created 17th February 2023.
 */
public abstract class DatumKVDVisitor extends DatumEncodingProxyVisitor {
    protected boolean readingKey = true;

    public DatumKVDVisitor() {
        super(DatumInvalidVisitor.INSTANCE);
    }

    @Override
    public void onVisitedValue() {
        // clear that we've visited a value, to interpret next as a key
        target = DatumInvalidVisitor.INSTANCE;
        readingKey = true;
    }

    @Override
    public void visitId(String s, DatumSrcLoc srcLoc) {
        if (!readingKey) {
            super.visitId(s, srcLoc);
            return;
        }
        target = handle(s);
        readingKey = false;
    }

    public abstract DatumVisitor handle(String key);

    @Override
    public void visitEnd(DatumSrcLoc loc) {
        if (!readingKey)
            throw new RuntimeException("Can't visit end in the middle of reading a value @ " + loc);
    }
}
