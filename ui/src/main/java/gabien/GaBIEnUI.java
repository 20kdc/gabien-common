package gabien;

import gabien.ui.LAFChain;
import gabien.ui.theming.ThemingCentral;

/**
 * Just to get code out of gabien-common.
 * Created 17th October 2023.
 */
public class GaBIEnUI {

    /**
     * Theme root used by internal UI.
     */
    public static final LAFChain.Node sysThemeRoot = new LAFChain.Node();

    static void setupAssets() {
        ThemingCentral.setupAssets(GaBIEn.internal);
    }
}
