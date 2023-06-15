/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import java.util.HashMap;

import gabien.datum.DatumDecToLambdaVisitor;
import gabien.datum.DatumKVDHVisitor;
import gabien.datum.DatumKVDVisitor;
import gabien.datum.DatumODecVisitor;
import gabien.datum.DatumSrcLoc;
import gabien.datum.DatumVisitor;
import gabien.datum.DatumODecVisitor.Handler;
import gabien.ui.UIBorderedElement;

/**
 * First object to try out ODec... oh no...
 * Created 17th February 2023.
 */
public class Theme {
    public int id = 0;
    public final IBorder[] border = new IBorder[UIBorderedElement.BORDER_TYPES];
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
                // finish off theme setup...
                for (int i = 0; i < theme.border.length; i++)
                    if (theme.border[i] == null)
                        throw new RuntimeException("Border missing: " + borderTypeNames[i]);
                // and return
                parent.visitTree(theme, srcLoc);
            }
        };
    };
    static final Handler<ThemingResCtx> brHandler = (k, parent, resCtx) -> {
        RegularBorder b = new RegularBorder();
        return makeGenericBorderVisitor(b, parent);
    };
    static final Handler<ThemingResCtx> btHandler = (k, parent, resCtx) -> {
        TiledBorder b = new TiledBorder();
        return makeGenericBorderVisitor(b, parent);
    };
    private static DatumVisitor makeGenericBorderVisitor(IBorder b, DatumODecVisitor<ThemingResCtx> parent) {
        return new DatumKVDVisitor() {
            int res = 0;
            @Override
            public void visitEnd(DatumSrcLoc srcLoc) {
                super.visitEnd(srcLoc);
                b.setFlags(res);
                parent.visitTree(b, srcLoc);
            }
            
            @Override
            public DatumVisitor handle(String key) {
                if (key.equals("moveDown"))
                    return flagVisitor(ThemingCentral.BF_MOVEDOWN);
                if (key.equals("clear"))
                    return flagVisitor(ThemingCentral.BF_CLEAR);
                if (key.equals("lightBkg"))
                    return flagVisitor(ThemingCentral.BF_LIGHTBKG);
                throw new RuntimeException("Unrecognized border key " + key);
            }

            private DatumVisitor flagVisitor(int flag) {
                return new DatumDecToLambdaVisitor((value, srcLoc) -> {
                    if (value == Boolean.TRUE) {
                        res |= flag;
                    } else if (value == Boolean.FALSE) {
                        // nothing
                    } else {
                        throw new RuntimeException("Flag cannot be " + value + " at " + srcLoc);
                    }
                });
            }
        };
    }

    static {
        for (int i = 0; i < borderTypeNames.length; i++) {
            final int borderType = i;
            handlersThemeKV.put(borderTypeNames[i], (k, theme, resCtx) -> {
                return resCtx.genVisitor((rb, ctx) -> {
                    theme.border[borderType] = (IBorder) rb;
                }, null);
            });
        }
    }
}
