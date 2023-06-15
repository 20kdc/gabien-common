/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import java.io.InputStreamReader;

import org.eclipse.jdt.annotation.NonNull;

import gabien.GaBIEn;
import gabien.datum.*;

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
    public static final int BORDER_THEMES = 4;
    public static final Theme[] themes = new Theme[BORDER_THEMES];
    public static final @NonNull Theme THEME_OF_LAST_RESORT = new Theme();

    public static void setupAssets() {
        // setup "base" themes
        for (int i = 0; i < 4; i++)
            themes[i] = new Theme();
        try {
            ThemingResCtx resCtx = new ThemingResCtx();

            try {
                // Read in override file
                InputStreamReader themesISR = GaBIEn.getTextResource("themes.override.scm");
                if (themesISR != null) {
                    new DatumReaderTokenSource("themes.override.scm", themesISR).visit(new DatumKVDVisitor() {
                        @Override
                        public DatumVisitor handle(String key) {
                            return resCtx.genVisitor(resCtx, key);
                        }
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // Read in resources
            InputStreamReader themesISR = GaBIEn.getTextResource("themes.scm");
            new DatumReaderTokenSource("themes.scm", themesISR).visit(new DatumKVDVisitor() {
                @Override
                public DatumVisitor handle(String key) {
                    return resCtx.genVisitor(resCtx, key);
                }
            });
            // Grab resources
            for (int i = 0; i < BORDER_THEMES; i++) {
                Theme tx = (Theme) resCtx.resources.get("t" + i);
                if (tx != null)
                    themes[i] = tx;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
