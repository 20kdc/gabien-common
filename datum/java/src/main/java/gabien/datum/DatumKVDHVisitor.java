/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.util.Map;

/**
 * This is expected to be used to visit the contents of a list for key/value pairs.
 * Created 18th February 2023.
 */
public class DatumKVDHVisitor<T, GT> extends DatumKVDVisitor {
    private final Map<String, Handler<T, GT>> handlers;
    private final T context;
    private final GT globalContext;

    public DatumKVDHVisitor(Map<String, Handler<T, GT>> handlers, T context, GT globalContext) {
        this.handlers = handlers;
        this.context = context;
        this.globalContext = globalContext;
    }

    @Override
    public DatumVisitor handle(String key) {
        Handler<T, GT> h = handlers.get(key);
        if (h == null)
            throw new RuntimeException("Key not handled: " + key);
        return h.handle(key, context, globalContext);
    }

    public interface Handler<T, GT> {
        /**
         * Returns a visitor for the given key, context, and global context.
         */
        DatumVisitor handle(String key, T context, GT globalContext);
    }
}
