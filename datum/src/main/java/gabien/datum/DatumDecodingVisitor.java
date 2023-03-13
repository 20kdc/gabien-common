/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.util.LinkedList;

/**
 * Turns a visitor on its head so that it outputs objects.
 * Created 15th February 2023.
 */
public abstract class DatumDecodingVisitor extends DatumVisitor {
    public DatumDecodingVisitor() {
        
    }

    @Override
    public abstract void visitTree(Object obj, DatumSrcLoc srcLoc);

    @Override
    public void visitString(String s, DatumSrcLoc srcLoc) {
        visitTree(s, srcLoc);
    }

    @Override
    public void visitId(String s, DatumSrcLoc srcLoc) {
        visitTree(new DatumSymbol(s), srcLoc);
    }

    @Override
    public void visitNumericUnknown(String s, DatumSrcLoc srcLoc) {
        throw new RuntimeException("Numeric can't be parsed: " + s);
    }

    @Override
    public void visitSpecialUnknown(String s, DatumSrcLoc srcLoc) {
        throw new RuntimeException("Special ID can't be parsed: " + s);
    }

    @Override
    public void visitBoolean(boolean value, DatumSrcLoc srcLoc) {
        visitTree(value, srcLoc);
    }

    @Override
    public void visitNull(DatumSrcLoc srcLoc) {
        visitTree(null, srcLoc);
    }

    @Override
    public void visitInt(long value, String raw, DatumSrcLoc srcLoc) {
        visitTree(value, srcLoc);
    }

    @Override
    public void visitFloat(double value, String raw, DatumSrcLoc srcLoc) {
        visitTree(value, srcLoc);
    }

    @Override
    public DatumVisitor visitList(DatumSrcLoc srcLoc) {
        final LinkedList<Object> buildingList = new LinkedList<>();
        final DatumDecodingVisitor me = this;
        return new DatumDecodingVisitor() {
            @Override
            public void visitTree(Object obj, DatumSrcLoc srcLoc) {
                buildingList.add(obj);
            }

            @Override
            public void visitEnd(DatumSrcLoc srcLoc) {
                me.visitTree(buildingList, srcLoc);
            }
        };
    }
}
