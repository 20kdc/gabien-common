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
import gabien.datum.DatumODecVisitor;
import gabien.datum.DatumSeqVisitor;
import gabien.datum.DatumSrcLoc;
import gabien.datum.DatumVisitor;
import gabien.datum.DatumODecVisitor.Handler;
import gabien.render.ITexRegion;
import gabien.ui.UIBorderedElement;

import static gabien.datum.DatumTreeUtils.isSym;

/**
 * First object to try out ODec... oh no...
 * Created 17th February 2023.
 */
public class Theme {
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
        return makeGenericBorderVisitor(parent, resCtx);
    };
    private static DatumVisitor makeGenericBorderVisitor(DatumODecVisitor<ThemingResCtx> parent, ThemingResCtx resCtx) {
        return new DatumSeqVisitor() {
            ITexRegion basis;
            int flags = 0;
            boolean tiled = false;

            @Override
            public DatumVisitor handle(int idx) {
                if (idx == 0)
                    return resCtx.genVisitor((obj, srcLoc) -> basis = (ITexRegion) obj, null);
                return new DatumDecToLambdaVisitor((res, srcLoc) -> {
                    if (isSym(res, "moveDown")) {
                        flags |= ThemingCentral.BF_MOVEDOWN;
                        return;
                    } else if (isSym(res, "clear")) {
                        flags |= ThemingCentral.BF_CLEAR;
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
                IBorder b = tiled ? new TiledBorder(flags, basis) : new RegularBorder(flags, basis);
                parent.visitTree(b, srcLoc);
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
