/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.util.Map;

/**
 * Object decoding visitor that passes along context for possible lambda reuse.
 * Created 17th February 2023.
 */
public class DatumODec1Visitor<HT, RT> extends DatumODecVisitor<HT> {
    public Returner<RT> returner;
    public RT returnerContext;

    public DatumODec1Visitor(Map<String, Handler<HT>> handlers, HT context, Returner<RT> returner, RT returnerContext) {
        super(handlers, context);
        this.returner = returner;
        this.returnerContext = returnerContext;
    }

    @Override
    public void visitTree(Object obj, DatumSrcLoc srcLoc) {
        returner.accept(obj, returnerContext);
    }

    @Override
    public void visitEnd(DatumSrcLoc srcLoc) {
    }

    public interface Returner<T> {
        void accept(Object value, T context);
    }
}
