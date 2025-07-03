/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.util.Map;

import datum.DatumInvalidVisitor;
import datum.DatumPositionedException;
import datum.DatumProxyVisitor;
import datum.DatumSrcLoc;
import datum.DatumStreamingVisitor;
import datum.DatumSymbol;
import datum.DatumVisitor;

/**
 * Object decoding visitor that passes along context for possible lambda reuse.
 * Created 17th February 2023.
 */
public class DatumODec1Visitor<HT, RT> extends DatumStreamingVisitor {
    public Map<String, Handler<HT>> handlers;
    public HT context;

    public Returner<RT> returner;
    public RT returnerContext;

    public DatumODec1Visitor(Map<String, Handler<HT>> handlers, HT context, Returner<RT> returner, RT returnerContext) {
        this.handlers = handlers;
        this.context = context;
        this.returner = returner;
        this.returnerContext = returnerContext;
    }

    public void returnVal(Object v, DatumSrcLoc srcLoc) {
        returner.accept(v, returnerContext, srcLoc);
    }

    @Override
    public final void visitString(String s, DatumSrcLoc srcLoc) {
        returner.accept(s, returnerContext, srcLoc);
    }

    @Override
    public final void visitId(String s, DatumSrcLoc srcLoc) {
        returner.accept(new DatumSymbol(s), returnerContext, srcLoc);
    }

    @Override
    public final void visitBoolean(boolean value, DatumSrcLoc srcLoc) {
        returner.accept(value, returnerContext, srcLoc);
    }

    @Override
    public final void visitNull(DatumSrcLoc srcLoc) {
        returner.accept(null, returnerContext, srcLoc);
    }

    @Override
    public final void visitInt(long value, DatumSrcLoc srcLoc) {
        returner.accept(value, returnerContext, srcLoc);
    }

    @Override
    public final void visitFloat(double value, DatumSrcLoc srcLoc) {
        returner.accept(value, returnerContext, srcLoc);
    }

    @Override
    public final DatumVisitor visitList(DatumSrcLoc srcLoc) {
        return new DatumProxyVisitor(DatumInvalidVisitor.INSTANCE) {
            boolean done = false;
            @Override
            public void visitId(String s, DatumSrcLoc srcLoc) {
                if (done) {
                    super.visitId(s, srcLoc);
                } else {
                    Handler<HT> h = handlers.get(s);
                    if (h == null)
                        throw new DatumPositionedException(srcLoc, "No handler for: " + s);
                    target = h.handle(s, DatumODec1Visitor.this, context);
                    done = true;
                }
            }
            @Override
            public void onVisitedValue() {
                // the proxy would always throw an error if a case like this were to be possible illegitimately
            }
        };
    }

    @Override
    public void visitEnd(DatumSrcLoc srcLoc) {
    }

    public interface Handler<HT> {
        /**
         * Given a parent visitor, returns a visitor to handle the remainder of the list.
         * At some point, the parent visitor's visitTree method should be called.
         * Ideally, this would be done after all changes to the object are finished.
         * But depending on application requirements that isn't required.
         * Calling visitTree at all is not a requirement, as the value may also be sent back via the context.
         */
        DatumVisitor handle(String key, DatumODec1Visitor<HT, ?> parent, HT context);
    }

    public interface Returner<T> {
        void accept(Object value, T context, DatumSrcLoc srcLoc);
    }
}
