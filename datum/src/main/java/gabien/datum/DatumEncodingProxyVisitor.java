/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Performs object to visitor encoding, but then forwards to another DatumVisitor.
 * This is mainly useful for testing, but also acts as an assertion that the data can be encoded.
 * Created on February 16th, 2023.
 */
public class DatumEncodingProxyVisitor extends DatumEncodingVisitor {
    /**
     * The target DatumVisitor.
     */
    public final DatumVisitor target;

    public DatumEncodingProxyVisitor(DatumVisitor target) {
        this.target = target;
    }

    @Override
    public void visitString(String s) {
        target.visitString(s);
    }

    @Override
    public void visitId(String s) {
        target.visitId(s);
    }

    @Override
    public void visitNumericUnknown(String s) {
        target.visitNumericUnknown(s);
    }

    @Override
    public void visitSpecialUnknown(String s) {
        target.visitSpecialUnknown(s);
    }

    @Override
    public void visitBoolean(boolean value) {
        target.visitBoolean(value);
    }

    @Override
    public void visitInt(long value, String raw) {
        target.visitInt(value, raw);
    }

    @Override
    public void visitFloat(double value, String raw) {
        target.visitFloat(value, raw);
    }

    @Override
    public DatumVisitor visitList() {
        return new DatumEncodingProxyVisitor(target.visitList());
    }

    @Override
    public void visitEnd() {
        target.visitEnd();
    }
    
}
