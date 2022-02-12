/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.backendhelp;

import java.io.File;

import gabien.GaBIEn;
import gabien.IGaBIEnFileBrowser;
import gabien.PriorityElevatorForUseByBackendHelp;
import gabien.WindowSpecs;
import gabien.ui.IConsumer;
import gabien.ui.UIElement;
import gabien.ui.WindowCreatingUIElementConsumer;

/**
 * Created on 04/03/2020.
 */
public class EmulatedFileBrowser extends PriorityElevatorForUseByBackendHelp implements IGaBIEnFileBrowser {

    // The initial browser directory.
    public String browserDirectory;
    
    public EmulatedFileBrowser() {
        setBrowserDirectory(".");
    }

    @Override
    public void setBrowserDirectory(String s) {
        browserDirectory = new File(s).getAbsolutePath();
    }

    @Override
    public void startFileBrowser(String text, boolean saving, String exts, IConsumer<String> result) {
        // Need to setup an environment for a file browser.
        final WindowCreatingUIElementConsumer wc = new WindowCreatingUIElementConsumer() {
            @Override
            protected WindowSpecs setupSpecs(UIElement o, int scale, boolean fullscreen, boolean resizable) {
                WindowSpecs ws = super.setupSpecs(o, scale, fullscreen, resizable);
                elevateToSystemPriority(ws);
                return ws;
            }
        };
        // if this crashes, you're pretty doomed
        UIFileBrowser fb = new UIFileBrowser(browserDirectory, result, text, saving ? GaBIEn.wordSave : GaBIEn.wordLoad, GaBIEn.sysCoreFontSize, GaBIEn.sysCoreFontSize);
        wc.accept(fb);
        final Runnable tick = new Runnable() {
            double lastTime = GaBIEn.getTime();
            @Override
            public void run() {
                double newTime = GaBIEn.getTime();
                double dT = newTime - lastTime;
                lastTime = newTime;
                wc.runTick(dT);
                if (wc.runningWindows().size() > 0)
                    GaBIEn.pushLaterCallback(this);
            }
        };
        GaBIEn.pushCallback(tick);
    }

}
