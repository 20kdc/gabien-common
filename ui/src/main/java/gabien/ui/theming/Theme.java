/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import static datum.DatumTreeUtils.*;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import datum.DatumSrcLoc;
import datum.DatumVisitor;
import gabien.datum.DatumKVDVisitor;
import gabien.datum.DatumODec1Visitor;
import gabien.datum.DatumSeqVisitor;
import gabien.datum.DatumODec1Visitor.Handler;
import gabien.render.IDrawable;
import gabien.render.ITexRegion;
import gabien.ui.FontManager;
import gabien.ui.LAFChain;

/**
 * Immutable (*in theory) theme.
 * Created 17th February 2023.
 */
public final class Theme {
    // Theme attributes
    public static final Attr<IBorder> B_BTN = new BAttr("btn", 0);
    public static final Attr<IBorder> B_BTNP = new BAttr("btnP", 1);
    public static final Attr<IBorder> B_LABEL = new BAttr("label", 2);
    public static final Attr<IBorder> B_TEXTBOX = new BAttr("textBox", 3);
    // 4
    public static final Attr<IBorder> B_TEXTBOXF = new BAttr("textBoxF", 4);
    public static final Attr<IBorder> B_WINDOW = new BAttr("window", 5);
    public static final Attr<IBorder> B_SBTRAY = new BAttr("sbTray", 6);
    public static final Attr<IBorder> B_SBNUB = new BAttr("sbNub", 7);
    // 8
    public static final Attr<IBorder> B_TABA = new BAttr("tabA", 8);
    public static final Attr<IBorder> B_TABB = new BAttr("tabB", 9);
    public static final Attr<IBorder> B_TABSEL = new BAttr("tabSel", 10);
    public static final Attr<IBorder> B_TITLE = new BAttr("title", 11);
    // 12
    public static final Attr<IBorder> B_TITLESEL = new BAttr("titleSel", 12);
    public static final Attr<IBorder> B_MENUBORDER = new BAttr("menuBorder", 13);
    // 14 - note the unfortunate stashing of an ultimate fallback FontManager here.
    // this may end up unexpectedly caching things past engine resets, but luckily those only happen in tests...
    public static final Attr<FontManager> FM_GLOBAL = new BuiltinAttr<>("fontManager", 14, FontManager.class, new FontManager(null, false));
    // 15
    public static final Attr<IDrawable> IC_ARROW_UP = new IAttr("arrowUp", 15, DefaultArrowIcon.DARK_U);
    public static final Attr<IDrawable> IC_ARROW_RIGHT = new IAttr("arrowRight", 16, DefaultArrowIcon.DARK_R);
    public static final Attr<IDrawable> IC_ARROW_DOWN = new IAttr("arrowDown", 17, DefaultArrowIcon.DARK_D);
    public static final Attr<IDrawable> IC_ARROW_LEFT = new IAttr("arrowLeft", 18, DefaultArrowIcon.DARK_L);
    public static final Attr<IBorder> B_NOTABPANEL = new BAttr("noTabPanel", 19);

    private final static Attr<?>[] allBI = {
            B_BTN, B_BTNP, B_LABEL, B_TEXTBOX, B_TEXTBOXF, B_WINDOW, B_SBTRAY, B_SBNUB,
            B_TABA, B_TABB, B_TABSEL, B_TITLE, B_TITLESEL, B_MENUBORDER, FM_GLOBAL, IC_ARROW_UP,
            IC_ARROW_RIGHT, IC_ARROW_DOWN, IC_ARROW_LEFT, B_NOTABPANEL
    };

    // Actual guts
    public static final Theme ROOT = new Theme();
    private final HashMap<String, Object> values = new HashMap<>();
    private final Object[] builtins = new Object[allBI.length];

    private Theme() {
    }

    private Theme(Theme other) {
        System.arraycopy(other.builtins, 0, builtins, 0, builtins.length);
        values.putAll(other.values);
    }

    public @Nullable Object getObject(String id) {
        return values.get(id);
    }

    /**
     * Internal mutable set.
     * Other functions use this to implement the immutable return-altered-version functions.
     */
    private void set(String key, Object obj) {
        for (Theme.Attr<?> s : allBI) {
            if (s.id.equals(key)) {
                @SuppressWarnings("unchecked")
                final Theme.Attr<Object> target = (Theme.Attr<Object>) s;
                target.set(this, obj);
                return;
            }
        }
        // Custom attribute
        values.put(key, obj);
    }

    /**
     * Replaces the given entry.
     */
    public Theme with(String id, @Nullable Object replacement) {
        Theme modified = new Theme(this);
        modified.set(id, replacement);
        return modified;
    }

    public static class Attr<T> {
        public final Class<T> clazz;
        public final @NonNull T def;
        public final String id;

        public Attr(String i, Class<T> clz, T def) {
            id = i;
            this.clazz = clz;
            this.def = def;
        }

        @SuppressWarnings("unchecked")
        public T get(Theme theme) {
            Object tmp = theme.getObject(id);
            if (tmp == null)
                return def;
            if (!clazz.isAssignableFrom(tmp.getClass()))
                return def;
            return (T) tmp;
        }

        public T get(LAFChain src) {
            return get(src.getTheme());
        }

        public final Theme with(Theme base, T res) {
            return base.with(id, res);
        }

        void set(Theme theme, T res) {
            theme.values.put(id, res);
        }
    }
    private static class BuiltinAttr<T> extends Attr<T> {
        final int iid;

        public BuiltinAttr(String id, int iid, Class<T> clz, T def) {
            super(id, clz, def);
            this.iid = iid;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T get(Theme theme) {
            Object tmp = theme.builtins[iid];
            if (tmp == null) {
                // System.out.println(id + " fell back to default, null @ " + theme + " ; root theme is " + Theme.ROOT + " and scf is " + GaBIEn.sysThemeRoot.getTheme());
                return def;
            }
            if (!clazz.isAssignableFrom(tmp.getClass())) {
                // System.out.println(id + " fell back to default, not assignable");
                return def;
            }
            return (T) tmp;
        }

        @Override
        void set(Theme theme, T res) {
            // Note that sets are mirrored to both, so that a get can happen via either method.
            super.set(theme, res);
            theme.builtins[iid] = res;
        }
    }
    private static class BAttr extends BuiltinAttr<IBorder> {
        public BAttr(String id, int iid) {
            super(id, iid, IBorder.class, FallbackBorder.INSTANCE);
        }
    }
    private static class IAttr extends BuiltinAttr<IDrawable> {
        public IAttr(String id, int iid, IDrawable d) {
            super(id, iid, IDrawable.class, d);
        }
    }

    // Visitor stuff follows...

    static final Handler<ThemingResCtx> handler = (k, parent, resCtx) -> {
        return new DatumKVDVisitor() {
            Theme theme = new Theme(ROOT);
            @Override
            public DatumVisitor handle(final String key, DatumSrcLoc loc) {
                if (key.equals("parent"))
                    return resCtx.genVisitor((obj, ctx, srcLoc) -> {
                        theme = new Theme((Theme) obj);
                    }, null);
                return resCtx.genVisitor((obj, ctx, srcLoc) -> {
                    theme.set(key, obj);
                }, null);
            }

            @Override
            public void visitEnd(DatumSrcLoc srcLoc) {
                // and return
                parent.returnVal(theme, srcLoc);
            }
        };
    };
    static final Handler<ThemingResCtx> brHandler = (k, parent, resCtx) -> {
        return makeGenericBorderVisitor(parent, resCtx);
    };
    private static DatumVisitor makeGenericBorderVisitor(DatumODec1Visitor<ThemingResCtx, ?> parent, ThemingResCtx resCtx) {
        return new DatumSeqVisitor() {
            ITexRegion basis;
            int flags = 0;
            boolean tiled = false;

            @Override
            public DatumVisitor handle(int idx) {
                if (idx == 0)
                    return resCtx.genVisitor((obj, ctx, srcLoc) -> basis = (ITexRegion) obj, null);
                return decVisitor((res, srcLoc) -> {
                    if (isSym(res, "moveDown")) {
                        flags |= ThemingCentral.BF_MOVEDOWN;
                        return;
                    } else if (isSym(res, "lightBkg")) {
                        flags |= ThemingCentral.BF_LIGHTBKG;
                        return;
                    } else if (isSym(res, "tiled")) {
                        tiled = true;
                        return;
                    }
                    throw new RuntimeException("Unrecognized border flag " + res + " @ " + srcLoc);
                });
            }

            @Override
            public void visitEnd(DatumSrcLoc srcLoc) {
                if (basis == null)
                    throw new RuntimeException("Border missing base image @ " + srcLoc);
                IBorder b = tiled ? new TiledBorder(flags, basis) : new StretchBorder(flags, basis);
                parent.returnVal(b, srcLoc);
            }
        };
    }
}
