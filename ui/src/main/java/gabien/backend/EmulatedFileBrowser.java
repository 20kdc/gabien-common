/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

import java.util.function.Consumer;

import gabien.GaBIEn;
import gabien.wsi.WindowSpecs;
import gabien.ui.UIElement;
import gabien.ui.WindowCreatingUIElementConsumer;

/**
 * Created on 04/03/2020.
 */
public class EmulatedFileBrowser implements IGaBIEnFileBrowser {

    // The initial browser directory.
    public String browserDirectory;
    private final IGaBIEn backend;
    
    public EmulatedFileBrowser(IGaBIEn backend) {
        this.backend = backend;
        setBrowserDirectory(".");
    }

    @Override
    public void setBrowserDirectory(String s) {
        browserDirectory = GaBIEn.absolutePathOf(s);
    }

    @Override
    public void startFileBrowser(String text, boolean saving, String exts, Consumer<String> result, String initialName) {
        // Need to setup an environment for a file browser.
        final WindowCreatingUIElementConsumer wc = new WindowCreatingUIElementConsumer() {
            @Override
            protected WindowSpecs setupSpecs(UIElement o, int scale, boolean fullscreen, boolean resizable) {
                WindowSpecs ws = super.setupSpecs(o, scale, fullscreen, resizable);
                ws.engineElevateToSystemPriority(backend);
                return ws;
            }
        };
        // if this crashes, you're pretty doomed
        UIFileBrowser fb = new UIFileBrowser(browserDirectory, result, text, saving, GaBIEn.sysCoreFontSize, GaBIEn.sysCoreFontSize, initialName);
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
