/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import static gabien.datum.DatumTreeUtils.asList;
import static gabien.datum.DatumTreeUtils.isSym;

import java.util.HashMap;
import java.util.List;

import gabien.datum.DatumDecodingVisitor;
import gabien.datum.DatumKVDHVisitor;
import gabien.datum.DatumSrcLoc;
import gabien.datum.DatumODecVisitor.Handler;
import gabien.ui.UIBorderedElement;

/**
 * First object to try out ODec... oh no...
 * Created 17th February 2023.
 */
public class Theme {
    public int id = 0;
    public final int[] borderFlags = new int[UIBorderedElement.BORDER_TYPES];
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
    static final HashMap<String, DatumKVDHVisitor.Handler<Theme, ThemingResCtx>> handlersThemeKV = new HashMap<>();
    static final Handler<ThemingResCtx> handler = (k, parent, resCtx) -> {
        Theme theme = new Theme();
        return new DatumKVDHVisitor<Theme, ThemingResCtx>(Theme.handlersThemeKV, theme, resCtx) {
            @Override
            public void visitEnd(DatumSrcLoc srcLoc) {
                parent.visitTree(theme, srcLoc);
            }
        };
    };

    static {
        for (int i = 0; i < borderTypeNames.length; i++) {
            final int borderType = i;
            handlersThemeKV.put(borderTypeNames[i], (k, theme, resCtx) -> {
                return new DatumDecodingVisitor() {
                    @Override
                    public void visitEnd(DatumSrcLoc srcLoc) {
                    }

                    @Override
                    public void visitTree(Object obj, DatumSrcLoc srcLoc) {
                        if (obj instanceof List) {
                            int res = 0;
                            for (Object flg : asList(obj)) {
                                if (isSym(flg, "moveDown")) {
                                    res |= ThemingCentral.BF_MOVEDOWN;
                                } else if (isSym(flg, "clear")) {
                                    res |= ThemingCentral.BF_CLEAR;
                                } else if (isSym(flg, "tiled")) {
                                    res |= ThemingCentral.BF_TILED;
                                } else if (isSym(flg, "lightBkg")) {
                                    res |= ThemingCentral.BF_LIGHTBKG;
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
    }
}
