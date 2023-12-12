/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

/**
 * Expects a list (for forwarding to DatumKVDVisitor) and nothing else.
 * Created 10th March 2023.
 */
public class DatumExpectListVisitor extends DatumInvalidVisitor {
    public final Handler handler;

    public DatumExpectListVisitor(Handler handler) {
        this.handler = handler;
    }

    @Override
    public DatumVisitor visitList(DatumSrcLoc srcLoc) {
        return handler.handle();
    }

    public interface Handler {
        /**
         * Creates a handler
         */
        DatumVisitor handle();
    }
}
