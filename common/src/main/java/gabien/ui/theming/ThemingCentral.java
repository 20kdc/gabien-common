/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import java.io.InputStreamReader;

import gabien.GaBIEn;
import gabien.IImage;
import gabien.datum.*;
import gabien.ui.UIBorderedElement;

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
    // text, etc. should be black
    public static final int BF_LIGHTBKG = 8;
    public static final Theme[] themes = new Theme[4];
    public static IImage themesImg;

    /**
     * It might be nice to do theme inheritance.
     */
    public static Theme getGlobalTheme() {
        return themes[UIBorderedElement.borderTheme];
    }

    public static void setupAssets() {
        try {
            themesImg = GaBIEn.getImageEx("themes.png", false, true);
            ThemingResCtx resCtx = new ThemingResCtx(themesImg);

            // Read in resources
            InputStreamReader themesISR = GaBIEn.getTextResource("themes.scm");
            new DatumReaderTokenSource("themes.scm", themesISR).visit(new DatumKVDVisitor() {
                @Override
                public DatumVisitor handle(String key) {
                    return resCtx.genVisitor(resCtx, key);
                }
            });
            // Grab resources
            for (int i = 0; i < 4; i++)
                themes[i] = (Theme) resCtx.resources.get("t" + i);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        for (int borderTheme = 0; borderTheme < UIBorderedElement.BORDER_THEMES; borderTheme++) {
            Theme theme = themes[borderTheme];
            for (int borderType = 0; borderType < UIBorderedElement.BORDER_TYPES; borderType++) {
                int baseX = borderType * 12;
                int baseY = borderTheme * 18;
                theme.border[borderType].doSetup(themesImg, baseX, baseY);
            }
        }

    }
}
