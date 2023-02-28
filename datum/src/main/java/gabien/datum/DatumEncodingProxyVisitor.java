/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Performs object to visitor encoding, but then forwards to another DatumVisitor.
 * Main uses are: Testing, and visitor state machines (using onVisitedValue).
 * Created on February 16th, 2023.
 */
public class DatumEncodingProxyVisitor extends DatumEncodingVisitor {
    /**
     * The target DatumVisitor.
     */
    public DatumVisitor target;

    public DatumEncodingProxyVisitor(DatumVisitor target) {
        this.target = target;
    }

    /**
     * Override this method to be notified when a value was visited.
     * If visiting a list, a proxy is created to ensure this is called when the list ends.
     */
    public void onVisitedValue() {
        
    }

    @Override
    public void visitString(String s) {
        target.visitString(s);
        onVisitedValue();
    }

    @Override
    public void visitId(String s) {
        target.visitId(s);
        onVisitedValue();
    }

    @Override
    public void visitNumericUnknown(String s) {
        target.visitNumericUnknown(s);
        onVisitedValue();
    }

    @Override
    public void visitSpecialUnknown(String s) {
        target.visitSpecialUnknown(s);
        onVisitedValue();
    }

    @Override
    public void visitBoolean(boolean value) {
        target.visitBoolean(value);
        onVisitedValue();
    }

    @Override
    public void visitNull() {
        target.visitNull();
        onVisitedValue();
    }

    @Override
    public void visitInt(long value, String raw) {
        target.visitInt(value, raw);
        onVisitedValue();
    }

    @Override
    public void visitFloat(double value, String raw) {
        target.visitFloat(value, raw);
        onVisitedValue();
    }

    @Override
    public DatumVisitor visitList() {
        final DatumEncodingProxyVisitor me = this;
        // Make a visitor that hooks the target list visitor so that we know when the list ends.
        return new DatumEncodingProxyVisitor(target.visitList()) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                me.onVisitedValue();
            }
        };
    }

    @Override
    public void visitEnd() {
        target.visitEnd();
    }
}
