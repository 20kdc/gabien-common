/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import gabien.GaBIEn;
import gabien.datum.DatumDecodingVisitor;
import gabien.datum.DatumEncodingProxyVisitor;
import gabien.datum.DatumKVDVisitor;
import gabien.datum.DatumODecVisitor;
import gabien.datum.DatumReaderTokenSource;

import static gabien.datum.DatumTreeUtils.*;

/**
 * This is holding all the stuff that's being pulled out of UIBorderedElement.
 * Please don't access this.
 * Created 17th February 2023.
 */
public class ThemingCentral {
    // use 'pressed' offset effect (WHERE SUPPORTED)
    public static final int BF_MOVEDOWN = 1;
    // Contents are black, use a clear for speed. (Ignored if tiling!)
    public static final int BF_CLEAR = 2;
    // hi-res section is tiled, mid-res becomes 3-pixel border w/ added weirdness
    public static final int BF_TILED = 4;
    // text, etc. should be black
    public static final int BF_LIGHTBKG = 8;
    public static final Theme[] themes = new Theme[4];
    public static final String[] borderTypeNames = new String[] {
        "btn",
        "btnP",
        "label",
        "textBox",
        // 4
        "textBoxF",
        "window",
        "sbTray",
        "sbNub",
        // 8
        "tabA",
        "tabB",
        "tabSel",
        "i11",
        // 12
        "i12",
        "r48Overlay"
    };
    public static void setupAssets() {
        try {
            InputStreamReader themesISR = GaBIEn.getTextResource("themes.scm");
            HashMap<String, DatumODecVisitor.Handler<Object>> handlers = new HashMap<>();
            HashMap<String, DatumKVDVisitor.Handler<Theme>> handlersThemeKV = new HashMap<>();
            handlers.put("id", (k, parent, c) -> {
                Theme theme = new Theme();
                return new DatumEncodingProxyVisitor(null) {
                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        parent.visitTree(theme);
                    }
                    @Override
                    public void visitInt(long value, String raw) {
                        if (target != null) {
                            super.visitInt(value, raw);
                            return;
                        }
                        theme.id = (int) value;
                        target = new DatumKVDVisitor<>(handlersThemeKV, theme);
                    }
                };
            });
            for (int i = 0; i < borderTypeNames.length; i++) {
                final int borderType = i;
                handlersThemeKV.put(borderTypeNames[i], (k, theme) -> {
                    return new DatumDecodingVisitor() {

                        @Override
                        public void visitEnd() {
                        }

                        @Override
                        public void visitTree(Object obj) {
                            if (obj instanceof List) {
                                int res = 0;
                                for (Object flg : asList(obj)) {
                                    if (isSym(flg, "moveDown")) {
                                        res |= BF_MOVEDOWN;
                                    } else if (isSym(flg, "clear")) {
                                        res |= BF_CLEAR;
                                    } else if (isSym(flg, "tiled")) {
                                        res |= BF_TILED;
                                    } else if (isSym(flg, "lightBkg")) {
                                        res |= BF_LIGHTBKG;
                                    } else {
                                        throw new RuntimeException("Unrecognized flag " + flg);
                                    }
                                }
                                theme.borderFlags[borderType] = res;
                            } else {
                                throw new RuntimeException("Unrecognized element in themes.scm: " + obj);
                            }
                        }
                    };
                });
            }
            DatumODecVisitor<Object> visitor = new DatumODecVisitor<Object>(handlers, null) {
                @Override
                public void visitTree(Object obj) {
                    if (obj instanceof Theme) {
                        Theme t = (Theme) obj;
                        themes[t.id] = t;
                    } else {
                        throw new RuntimeException("Unrecognized element in themes.scm: " + obj);
                    }
                }
                @Override
                public void visitEnd() {
                    // nothing to do here
                }
            };
            new DatumReaderTokenSource(themesISR).visit(visitor);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
