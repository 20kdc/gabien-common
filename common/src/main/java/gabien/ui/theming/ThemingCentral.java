/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import java.io.InputStreamReader;
import java.util.List;

import gabien.GaBIEn;
import gabien.datum.DatumDecodingVisitor;
import gabien.datum.DatumReaderTokenSource;
import gabien.ui.UIBorderedElement;

import static gabien.datum.DatumTreeUtils.*;

/**
 * This is holding all the stuff that's being pulled out of UIBorderedElement.
 * Please don't access this.
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
    public static final int[] borderFlags = new int[UIBorderedElement.BORDER_THEMES * UIBorderedElement.BORDER_TYPES];
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
            new DatumReaderTokenSource(themesISR).visit(new DatumDecodingVisitor() {
                @Override
                public void visitTree(Object obj) {
                    if (obj instanceof List) {
                        @SuppressWarnings("rawtypes")
                        Object[] oa = ((List) obj).toArray();
                        if (isSym(oa[0], "borderFlags")) {
                            int borderTheme = asInt(oa[1]);
                            int borderType = -1;
                            for (int i = 0; i < borderTypeNames.length; i++)
                                if (isSym(oa[2], borderTypeNames[i]))
                                    borderType = i;
                            if (borderType == -1)
                                throw new RuntimeException("Unknown border type " + oa[2]);
                            int res = 0;
                            for (int i = 3; i < oa.length; i++) {
                                Object flg = oa[i];
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
                            borderFlags[(borderType * UIBorderedElement.BORDER_THEMES) + borderTheme] = res;
                        } else {
                            throw new RuntimeException("Unrecognized object in themes.scm of kind " + oa[0]);
                        }
                    } else {
                        throw new RuntimeException("Unrecognized element in themes.scm: " + obj);
                    }
                }
                @Override
                public void visitEnd() {
                    // nothing to do here
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
