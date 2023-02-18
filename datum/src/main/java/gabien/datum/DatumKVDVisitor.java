/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.util.HashMap;

/**
 * This is expected to be used to visit the contents of a list for key/value pairs.
 * Created 17th February 2023.
 */
public class DatumKVDVisitor<T> extends DatumEncodingProxyVisitor {
    public final HashMap<String, Handler<T>> handlers;
    public final T context;
    protected boolean readingKey = true;

    public DatumKVDVisitor(HashMap<String, Handler<T>> handlers, T context) {
        super(DatumInvalidVisitor.INSTANCE);
        this.handlers = handlers;
        this.context = context;
    }

    @Override
    public void onVisitedValue() {
        // clear that we've visited a value, to interpret next as a key
        target = DatumInvalidVisitor.INSTANCE;
        readingKey = true;
    }

    @Override
    public void visitId(String s) {
        if (!readingKey) {
            super.visitId(s);
            return;
        }
        Handler<T> h = handlers.get(s);
        if (h == null)
            throw new RuntimeException("No handler for: " + s);
        target = h.handle(s, context);
        readingKey = false;
    }

    public interface Handler<T> {
        /**
         * Given the key and the context, returns a visitor to handle the next value.
         * This visitor will be conveniently and automatically terminated.
         */
        DatumVisitor handle(String key, T context);
    }
}
