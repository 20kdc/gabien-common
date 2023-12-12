/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.util.Map;

/**
 * All lists passed to this are expected to begin with a symbol.
 * The symbol defines how the list is handled.
 * Useful to quickly get data formats off the ground.
 * Created 17th February 2023.
 */
public abstract class DatumODecVisitor<T> extends DatumDecodingVisitor {
    public Map<String, Handler<T>> handlers;
    public T context;

    public DatumODecVisitor(Map<String, Handler<T>> handlers, T context) {
        this.handlers = handlers;
        this.context = context;
    }

    @Override
    public DatumVisitor visitList(DatumSrcLoc srcLoc) {
        return new DatumEncodingProxyVisitor(DatumInvalidVisitor.INSTANCE) {
            boolean done = false;
            @Override
            public void visitId(String s, DatumSrcLoc srcLoc) {
                if (done) {
                    super.visitId(s, srcLoc);
                } else {
                    Handler<T> h = handlers.get(s);
                    if (h == null)
                        throw new RuntimeException("No handler for: " + s);
                    target = h.handle(s, DatumODecVisitor.this, context);
                    done = true;
                }
            }
            @Override
            public void onVisitedValue() {
                // the proxy would always throw an error if a case like this were to be possible illegitimately
            }
        };
    }

    public interface Handler<T> {
        /**
         * Given a parent visitor, returns a visitor to handle the remainder of the list.
         * At some point, the parent visitor's visitTree method should be called.
         * Ideally, this would be done after all changes to the object are finished.
         * But depending on application requirements that isn't required.
         * Calling visitTree at all is not a requirement, as the value may also be sent back via the context.
         */
        DatumVisitor handle(String key, DatumODecVisitor<T> parent, T context);
    }
}
