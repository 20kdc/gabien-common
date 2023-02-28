/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Throws invalid errors on any kind of visit by default.
 * Created February 18th, 2023.
 */
public class DatumInvalidVisitor extends DatumEncodingVisitor {
    public static final DatumInvalidVisitor INSTANCE = new DatumInvalidVisitor();

    public DatumInvalidVisitor() {
    }

    @Override
    public void visitString(String s) {
        throw new RuntimeException("Did not expect string " + s + " here");
    }

    @Override
    public void visitId(String s) {
        throw new RuntimeException("Did not expect ID " + s + " here");
    }

    @Override
    public void visitNumericUnknown(String s) {
        throw new RuntimeException("Did not expect numeric " + s + " here");
    }

    @Override
    public void visitSpecialUnknown(String s) {
        throw new RuntimeException("Did not expect special ID " + s + " here");
    }

    @Override
    public void visitBoolean(boolean value) {
        throw new RuntimeException("Did not expect boolean " + value + " here");
    }

    @Override
    public void visitNull() {
        throw new RuntimeException("Did not expect null here");
    }

    @Override
    public void visitInt(long value, String raw) {
        throw new RuntimeException("Did not expect int " + raw + " here");
    }

    @Override
    public void visitFloat(double value, String raw) {
        throw new RuntimeException("Did not expect float " + raw + " here");
    }

    @Override
    public DatumVisitor visitList() {
        throw new RuntimeException("Did not expect list here");
    }

    @Override
    public void visitEnd() {
    }
}
