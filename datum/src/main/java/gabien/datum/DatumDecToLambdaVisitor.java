/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Simply passes to a lambda.
 * Mainly useful for syntactic simplicity.
 * Created 13th March 2023.
 */
public class DatumDecToLambdaVisitor extends DatumDecodingVisitor {
    public final Handler handler;

    public DatumDecToLambdaVisitor(Handler h) {
        this.handler = h;
    }

    @Override
    public void visitTree(Object obj, DatumSrcLoc srcLoc) {
        handler.handle(obj, srcLoc);
    }

    @Override
    public void visitEnd(DatumSrcLoc loc) {
    }

    public interface Handler {
        void handle(Object value, DatumSrcLoc srcLoc);
    }
}
