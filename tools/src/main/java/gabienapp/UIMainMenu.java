/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import gabien.ui.UIScrollLayout;
import gabien.ui.UITextButton;
import gabien.ui.WindowCreatingUIElementConsumer;
import gabien.GaBIEn;
import gabien.media.RIFFNode;
import gabien.ui.Rect;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.UILabel;

/**
 * Main menu!
 * Created 19th June, 2023.
 */
public class UIMainMenu extends UIProxy {
    public final UIScrollLayout vsl = new UIScrollLayout(true, 16);
    public final UILabel lbl = new UILabel("RIFF Clipboard: (none!)", 16);
    public RIFFNode riffClipboard;
    public WindowCreatingUIElementConsumer ui;
    public UIMainMenu(WindowCreatingUIElementConsumer ui) {
        this.ui = ui;
        proxySetElement(vsl, false);
        vsl.panelsAdd(new UITextButton("Start RIFF Editor", 16, () -> {
            ui.accept(new UIRIFFEditor(this));
        }));
        vsl.panelsAdd(lbl);
        setForcedBounds(null, new Rect(0, 0, 640, 480));
        setLAFParentOverride(GaBIEn.sysThemeRoot);
    }
    public void copyRIFF(RIFFNode rn) {
        riffClipboard = rn.copy();
        lbl.text = "RIFF Clipboard: " + rn.chunkId;
    }
}
