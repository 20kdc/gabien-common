/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import java.util.HashMap;

import gabien.datum.DatumODec1Visitor;
import gabien.datum.DatumODecVisitor;

/**
 * This is theming internal magical stuff.
 * Created 18th February 2023.
 */
class ThemingResCtx implements DatumODec1Visitor.Returner<String> {
    public final HashMap<String, Object> resources = new HashMap<>();

    static final HashMap<String, DatumODecVisitor.Handler<ThemingResCtx>> handlers = new HashMap<>();
    static {
        handlers.put("theme", Theme.handler);
    }

    @Override
    public void accept(Object value, String context) {
        resources.put(context, value);
    }
}
