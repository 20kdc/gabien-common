/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import java.util.HashMap;

import gabien.GaBIEn;
import gabien.datum.DatumInvalidVisitor;
import gabien.datum.DatumODec1Visitor;
import gabien.datum.DatumODecVisitor;
import gabien.datum.DatumSeqVisitor;
import gabien.datum.DatumSrcLoc;
import gabien.datum.DatumTreeUtils;
import gabien.datum.DatumVisitor;
import gabien.render.ITexRegion;
import gabien.ui.Rect;

/**
 * This is theming internal magical stuff.
 * Created 18th February 2023.
 */
class ThemingResCtx implements DatumODec1Visitor.Returner<String> {
    public final HashMap<String, Object> resources = new HashMap<>();

    static final HashMap<String, DatumODecVisitor.Handler<ThemingResCtx>> handlers = new HashMap<>();
    static {
        handlers.put("img", (k, parent, resCtx) -> {
            return new DatumSeqVisitor() {
                public String fn;
                @Override
                public DatumVisitor handle(int idx) {
                    if (idx == 0)
                        return resCtx.genVisitor((val, ign) -> fn = (String) val, null);
                    return DatumInvalidVisitor.INSTANCE;
                }
                @Override
                public void visitEnd(DatumSrcLoc srcLoc) {
                    if (seqPos != 1)
                        throw new RuntimeException("Can't finish image yet @ " + srcLoc);
                    parent.visitTree(GaBIEn.getImageEx(fn, false, true), srcLoc);
                }
            };
        });
        handlers.put("reg", (k, parent, resCtx) -> {
            return new DatumSeqVisitor() {
                public ITexRegion src;
                public Rect rct;
                @Override
                public DatumVisitor handle(int idx) {
                    if (idx == 0)
                        return resCtx.genVisitor((val, ign) -> src = (ITexRegion) val, null);
                    if (idx == 1)
                        return resCtx.genVisitor((val, ign) -> rct = (Rect) val, null);
                    return DatumInvalidVisitor.INSTANCE;
                }
                @Override
                public void visitEnd(DatumSrcLoc srcLoc) {
                    if (seqPos != 2)
                        throw new RuntimeException("Can't finish region yet @ " + srcLoc);
                    parent.visitTree(src.subRegion(rct.x, rct.y, rct.width, rct.height), srcLoc);
                }
            };
        });
        handlers.put("rect", (k, parent, resCtx) -> {
            return new DatumSeqVisitor() {
                public int x, y, w, h;
                @Override
                public DatumVisitor handle(int idx) {
                    if (idx == 0)
                        return resCtx.genVisitor((val, ign) -> x = DatumTreeUtils.cInt(val), null);
                    if (idx == 1)
                        return resCtx.genVisitor((val, ign) -> y = DatumTreeUtils.cInt(val), null);
                    if (idx == 2)
                        return resCtx.genVisitor((val, ign) -> w = DatumTreeUtils.cInt(val), null);
                    if (idx == 3)
                        return resCtx.genVisitor((val, ign) -> h = DatumTreeUtils.cInt(val), null);
                    return DatumInvalidVisitor.INSTANCE;
                }
                @Override
                public void visitEnd(DatumSrcLoc srcLoc) {
                    if (seqPos != 4)
                        throw new RuntimeException("Can't finish region yet @ " + srcLoc);
                    parent.visitTree(new Rect(x, y, w, h), srcLoc);
                }
            };
        });
        handlers.put("theme", Theme.handler);
        handlers.put("border", Theme.brHandler);
    }

    public ThemingResCtx() {
    }

    @Override
    public void accept(Object value, String context) {
        // Skip already defined resources; this allows for user overrides that alter variables that then alter other stuff
        if (resources.containsKey(context))
            return;
        resources.put(context, value);
    }

    public <T> DatumVisitor genVisitor(DatumODec1Visitor.Returner<T> returner, T returnerCtx) {
        return new DatumODec1Visitor<ThemingResCtx, T>(handlers, this, returner, returnerCtx) {
            @Override
            public void visitId(String s, DatumSrcLoc srcLoc) {
                Object resGet = resources.get(s);
                if (resGet == null)
                    throw new RuntimeException("No such resource: " + s + " @ " + srcLoc);
                returner.accept(resGet, returnerCtx);
            }
        };
    }
}
