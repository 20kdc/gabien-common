/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import gabien.GaBIEn;
import gabien.ui.WindowCreatingUIElementConsumer;
import gabien.ui.theming.ThemingCentral;

/**
 * Created 19th June, 2023.
 */
public class Application {
    public static void gabienmain() {
        GaBIEn.sysThemeRoot.setThemeOverride(ThemingCentral.themes[1]);
        WindowCreatingUIElementConsumer wcuiec = new WindowCreatingUIElementConsumer();
        wcuiec.accept(new UIMainMenu(wcuiec));
        while (wcuiec.runningWindowCount() > 0) {
            double time = GaBIEn.endFrame(0.05d);
            wcuiec.runTick(time);
        }
        GaBIEn.ensureQuit();
    }
}
