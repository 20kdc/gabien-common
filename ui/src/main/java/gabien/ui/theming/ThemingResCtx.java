/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import java.util.HashMap;

import datum.DatumInvalidVisitor;
import datum.DatumSrcLoc;
import datum.DatumSymbol;
import datum.DatumTreeUtils;
import datum.DatumVisitor;
import gabien.GaBIEn;
import gabien.datum.DatumODec1Visitor;
import gabien.datum.DatumSeqVisitor;
import gabien.render.ITexRegion;
import gabien.uslx.append.Rect;

/**
 * This is theming internal magical stuff.
 * Created 18th February 2023.
 */
class ThemingResCtx implements DatumODec1Visitor.Returner<String> {
    public final HashMap<String, Object> resources = new HashMap<>();

    static final HashMap<String, DatumODec1Visitor.Handler<ThemingResCtx>> handlers = new HashMap<>();
    static {
        handlers.put("img", (k, parent, resCtx) -> {
            return new DatumSeqVisitor() {
                public String fn;
                @Override
                public DatumVisitor handle(int idx) {
                    if (idx == 0)
                        return resCtx.genVisitor((val, ign, srcLoc) -> fn = (String) val, null);
                    return DatumInvalidVisitor.INSTANCE;
                }
                @Override
                public void visitEnd(DatumSrcLoc srcLoc) {
                    if (seqPos != 1)
                        throw new RuntimeException("Can't finish image yet @ " + srcLoc);
                    parent.returnVal(GaBIEn.getImageEx(fn, false, true), srcLoc);
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
                        return resCtx.genVisitor((val, ign, srcLoc) -> src = (ITexRegion) val, null);
                    if (idx == 1)
                        return resCtx.genVisitor((val, ign, srcLoc) -> rct = (Rect) val, null);
                    return DatumInvalidVisitor.INSTANCE;
                }
                @Override
                public void visitEnd(DatumSrcLoc srcLoc) {
                    if (seqPos != 2)
                        throw new RuntimeException("Can't finish region yet @ " + srcLoc);
                    parent.returnVal(src.subRegion(rct.x, rct.y, rct.width, rct.height), srcLoc);
                }
            };
        });
        handlers.put("rect", (k, parent, resCtx) -> {
            return new DatumSeqVisitor() {
                public int x, y, w, h;
                @Override
                public DatumVisitor handle(int idx) {
                    if (idx == 0)
                        return resCtx.genVisitor((val, ign, srcLoc) -> x = DatumTreeUtils.cInt(val), null);
                    if (idx == 1)
                        return resCtx.genVisitor((val, ign, srcLoc) -> y = DatumTreeUtils.cInt(val), null);
                    if (idx == 2)
                        return resCtx.genVisitor((val, ign, srcLoc) -> w = DatumTreeUtils.cInt(val), null);
                    if (idx == 3)
                        return resCtx.genVisitor((val, ign, srcLoc) -> h = DatumTreeUtils.cInt(val), null);
                    return DatumInvalidVisitor.INSTANCE;
                }
                @Override
                public void visitEnd(DatumSrcLoc srcLoc) {
                    if (seqPos != 4)
                        throw new RuntimeException("Can't finish region yet @ " + srcLoc);
                    parent.returnVal(new Rect(x, y, w, h), srcLoc);
                }
            };
        });
        handlers.put("theme", Theme.handler);
        handlers.put("border", Theme.brHandler);
        handlers.put("defaultArrowIcon", (k, parent, resCtx) -> {
            return new DatumSeqVisitor() {
                public int rotation;
                public float r, g, b, a;
                @Override
                public DatumVisitor handle(int idx) {
                    if (idx == 0)
                        return resCtx.genVisitor((val, ign, srcLoc) -> rotation = DatumTreeUtils.cInt(val), null);
                    if (idx == 1)
                        return resCtx.genVisitor((val, ign, srcLoc) -> r = DatumTreeUtils.cFloat(val), null);
                    if (idx == 2)
                        return resCtx.genVisitor((val, ign, srcLoc) -> g = DatumTreeUtils.cFloat(val), null);
                    if (idx == 3)
                        return resCtx.genVisitor((val, ign, srcLoc) -> b = DatumTreeUtils.cFloat(val), null);
                    if (idx == 4)
                        return resCtx.genVisitor((val, ign, srcLoc) -> a = DatumTreeUtils.cFloat(val), null);
                    return DatumInvalidVisitor.INSTANCE;
                }
                @Override
                public void visitEnd(DatumSrcLoc srcLoc) {
                    if (seqPos != 5)
                        throw new RuntimeException("Can't finish defaultArrowIcon yet @ " + srcLoc);
                    parent.returnVal(new DefaultArrowIcon(rotation, r, g, b, a), srcLoc);
                }
            };
        });
    }

    public ThemingResCtx() {
    }

    @Override
    public void accept(Object value, String context, DatumSrcLoc srcLoc) {
        if (context.endsWith("?")) {
            context = context.substring(0, context.length() - 1);
            // Skip already defined resources.
            // This allows for user overrides that alter variables that then alter other stuff.
            if (resources.containsKey(context))
                return;
        }
        resources.put(context, value);
    }

    public <T> DatumVisitor genVisitor(DatumODec1Visitor.Returner<T> returner, T returnerCtx) {
        return new DatumODec1Visitor<ThemingResCtx, T>(handlers, this, (value, context, srcLoc) -> {
            if (value instanceof DatumSymbol) {
                String s = ((DatumSymbol) value).id;
                Object resGet = resources.get(s);
                if (resGet == null)
                    throw new RuntimeException("No such resource: " + s + " @ " + srcLoc);
                returner.accept(resGet, returnerCtx, srcLoc);
            } else {
                returner.accept(value, returnerCtx, srcLoc);
            }
        }, returnerCtx);
    }
}
